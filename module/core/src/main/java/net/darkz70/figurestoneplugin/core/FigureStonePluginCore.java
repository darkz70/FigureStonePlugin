package net.darkz70.figurestoneplugin.core;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import lombok.Getter;
import me.modmuss50.mpp.ModPublishExtension;
import net.darkz70.figurestoneplugin.common.*;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import net.darkz70.figurestoneplugin.core.loader.*;
import net.darkz70.figurestoneplugin.core.manager.*;
import net.darkz70.figurestoneplugin.core.util.MultiVersion;
import org.gradle.api.*;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.plugins.*;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.tasks.*;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

@Getter
public class FigureStonePluginCore implements Plugin<Project> {

	public static final FigureStoneLogger LOGGER = new FigureStoneLogger("Core");

	private MultiVersion projectMultiVersion;
	private int javaVersionIndex;
	private JavaVersion javaVersion;

	@Override
	public void apply(@NotNull Project project) {
		LOGGER.setup(project);

		FigureStoneProjectConfigurationData data = FigureStoneProjectConfigurationData.create(project, this);

		//

		PluginContainer plugins = project.getPlugins();
		plugins.apply("dev.kikugie.stonecutter");
		plugins.apply("me.modmuss50.mod-publish-plugin");
		plugins.apply("dev.kikugie.fletching-table");
		plugins.apply("maven-publish");
		data.loaderManager().applyPlugins(data);

		//

		this.projectMultiVersion = FigureStonePluginCore.getProjectMultiVersion(project);
		this.javaVersionIndex = FigureStonePluginCore.getJavaVersion(project);
		this.javaVersion = JavaVersion.toVersion(this.javaVersionIndex);

		//

		FigureStonePluginCore.configureProject(data);
		JavaManager.apply(data);
		J52JManager.apply(data);
		ProcessResourcesManager.apply(data);
		DependenciesManager.apply(data);
		StonecutterManager.apply(data);

		//

		FigureStonePluginCore.configureExtensions(data);
		FigureStonePluginCore.configureTasks(data);

		LOGGER.log("Project Version: %s", project.getVersion());
		LOGGER.log("Java Version: %s", this.javaVersionIndex);
	}

	private static void configureExtensions(@NotNull FigureStoneProjectConfigurationData data) {
		LoaderManager loaderManager = data.loaderManager();
		Project project = data.project();

		loaderManager.configureExtensions(data);

		project.afterEvaluate((p) -> {
			project.getExtensions().configure(ModPublishExtension.class, (mpe) -> {
				ModPublishManager.apply(data, mpe);
			});

			project.getExtensions().configure(PublishingExtension.class, (pe) -> {
				String loader = FigureStoneUtils.substringBefore(project.getName(), "-");
				String version = FigureStoneUtils.substringSince(project.getName(), "-");

				RepositoryHandler repositories = pe.getRepositories();
				for (ArtifactRepository repository : repositories) {
					project.getRootProject().getTasks().register("publishMaven+%s+%s+%s".formatted(loader, repository.getName(), version), (task) -> {
						task.setGroup("ad-figurestone-maven-%s".formatted(loader));
						task.dependsOn(":%s:publishAllPublicationsTo%sRepository".formatted(project.getName(), repository.getName()));
					});
				}
			});
		});
	}

	private static void configureTasks(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		LoaderManager loaderManager = data.loaderManager();

		project.getTasks().register("rebuildLibs", Delete.class, task -> {
			task.setGroup("build");
			String modName = FigureStoneUtils.getProperty(project, "data.mod_name").replace(" ", "");
			String version = project.getVersion().toString();

			String jarFileName = "libs/%s-%s.jar".formatted(modName, version);
			String sourcesJarFileName = "libs/%s-%s-sources.jar".formatted(modName, version);

			task.delete(getRootFile(project, jarFileName));
			task.delete(project.getLayout().getBuildDirectory().file(jarFileName));
			task.delete(project.getLayout().getBuildDirectory().file(sourcesJarFileName));
		});
		project.getTasks().named("build", task -> {
			task.mustRunAfter("rebuildLibs");
		});
		project.afterEvaluate((p) -> {
			project.getTasks().register("buildAndCollect", Copy.class, task -> {
				task.setGroup("build");
				task.dependsOn("rebuildLibs", "build");
				task.from(((Jar) project.getTasks().getByName(loaderManager.getJarTaskName(data))).getArchiveFile().get());
				task.into(getRootFile(project, "libs/"));
			});

			List<String> publishTasks = new ArrayList<>();

			String modrinthId = getProperty(project, "modrinth_id");
			String curseForgeId = getProperty(project,"curseforge_id");

			if (!modrinthId.equals("none")) {
				publishTasks.add("publishModrinth");
			}
			if (!curseForgeId.equals("none")) {
				publishTasks.add("publishCurseforge");
			}

			for (String publishTask : publishTasks) {
				project.getTasks().named(publishTask).configure((task) -> {
					task.doLast((t) -> {
						try {
							Thread.sleep(1000L);
						} catch (Exception e) {
							FigureStonePluginCore.LOGGER.log("Failed to wait before publishing!");
							e.printStackTrace(System.out);
						}
					});
				});
			}
		});
	}

	private static void configureProject(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		FigureStonePluginCore plugin = data.plugin();

		String projectVersion = plugin.getFigureStoneProjectVersion(data);
		String mavenGroup = FigureStoneUtils.getProperty(project, "data.mod_maven_group");
		project.setVersion(projectVersion);
		project.setGroup(mavenGroup);

		BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
		base.getArchivesName().set(FigureStoneUtils.getProperty(project, "data.mod_name").replace(" ", ""));

		Jar jar = (Jar) project.getTasks().getByName("jar");
		jar.getArchiveBaseName().set(base.getArchivesName().get());
		jar.from(getRootFile(project, "LICENSE"), (spec) -> {
			spec.rename(s -> "%s_%s".formatted(s, base.getArchivesName().get()));
		});
	}

	public static int getJavaVersion(Project project) {
		String currentMCVersion = getCurrentMCVersion(project);
		StonecutterBuildExtension stonecutter = getStonecutter(project);
		return stonecutter.compare("26.1", currentMCVersion) == 1 ?
				stonecutter.compare("1.20.5", currentMCVersion) == 1 ?
						stonecutter.compare("1.18", currentMCVersion) == 1 ?
								stonecutter.compare("1.16.5", currentMCVersion) == 1 ?
										8
										:
										16
								:
								17
						:
						21
				:
				25;
	}


	public static MultiVersion getProjectMultiVersion(@NotNull Project currentProject) {
		String currentMCVersion = getCurrentMCVersion(currentProject);
		String currentLoader = getCurrentLoader(currentProject);

		String[] versions = FigureStoneUtils.getProperty(currentProject, "%s.versions_specifications".formatted(currentLoader)).split(" ");
		for (String version : versions) {
			if (!version.contains("[") || !version.contains("]")) {
				continue;
			}
			String[] split = version.substring(0, version.length()-1).split("\\[");
			String project = split[0];
			if (Objects.equals(project, currentMCVersion)) {
				String supportedVersionsString = split[1];
				if (supportedVersionsString.contains("-")) {
					String[] supportedVersions = supportedVersionsString.split("-");
					return new MultiVersion(currentMCVersion, supportedVersions[0], supportedVersions[1]);
				} else if (supportedVersionsString.contains(".")) {
					return new MultiVersion(currentMCVersion, currentMCVersion, supportedVersionsString);
				} else {
					int a = project.indexOf(".");
					int i = project.lastIndexOf(".");
					if (a == i) {
						i = project.length();
					}
					String p = project.substring(0, i);
					String supportedMaxVersion = "%s.%s".formatted(p, supportedVersionsString);
					return new MultiVersion(currentMCVersion, currentMCVersion, supportedMaxVersion);
				}
			}
		}
		return new MultiVersion(currentMCVersion, currentMCVersion, currentMCVersion);
	}

	public static Properties getPersonalProperties(@NotNull Project project) {
		File file = project.getRootProject().file("personal/personal.properties");
		Properties personalProperties = new Properties();

		if (!file.exists()) {
			return personalProperties;
		}

		try (InputStream stream = new FileInputStream(file)) {
			personalProperties.load(stream);
		} catch (IOException e) {
			LOGGER.log("Something went wrong when parsing personal properties:");
			LOGGER.log(e.getMessage());
		}

		try {
			String mixinPath = "absolute_path_to_sponge_mixin";

			for (String line : Files.readAllLines(file.toPath())) {
				if (!line.startsWith(mixinPath)) {
					continue;
				}
				personalProperties.setProperty(mixinPath, line.substring(mixinPath.length() + 1));
			}
		} catch (Exception e) {
			LOGGER.log("Something went wrong when parsing personal properties mixin path:");
			LOGGER.log(e.getMessage());
		}

		return personalProperties;
	}

	public static Map<String, String> getFigureStoneProperties(Project project, String prefix) {
		HashMap<String, String> dependencies = new HashMap<>();

		Map<String, ?> properties = project.getProperties();
		for (Entry<String, ?> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!key.startsWith(prefix + ".")) {
				continue;
			}
			dependencies.put(FigureStoneUtils.substringSince(key, "."), value.toString());
		}

		return dependencies;
	}

	public static String getCurrentMCVersion(@NotNull Project project) {
		String version = getStonecutter(project).getCurrent().getVersion();
		if (version.startsWith("26.1-")) {
			return "26.1";
		}
		return version;
	}

	public static String getCurrentLoader(@NotNull Project project) {
		return FigureStoneUtils.substringBefore(getStonecutter(project).getCurrent().getProject(), "-");
	}

	public static @NotNull StonecutterBuildExtension getStonecutter(@NotNull Project project) {
		return (StonecutterBuildExtension) project.getExtensions().getByName("stonecutter");
	}

	public static String getProperty(Project project, String id) {
		return FigureStoneUtils.getProperty(project, id);
	}

	public String getFigureStoneProjectVersion(FigureStoneProjectConfigurationData data) {
		String modVersion = FigureStoneUtils.getProperty(data.project(), "data.mod_version");
		return "%s+%s+%s".formatted(modVersion, data.comparableMinecraftVersion(), data.loaderName());
	}

	public static File getRootFile(@NotNull Project project, String path) {
		return project.getRootProject().file(path);
	}

}
