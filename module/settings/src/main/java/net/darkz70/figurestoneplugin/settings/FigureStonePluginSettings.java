package net.darkz70.figurestoneplugin.settings;

import java.io.*;
import java.util.*;
import lombok.Getter;
import net.darkz70.figurestoneplugin.common.*;
import net.darkz70.figurestoneplugin.settings.loader.LoaderManager;
import net.darkz70.figurestoneplugin.settings.manager.*;
import net.darkz70.figurestoneplugin.settings.project.FigureStoneProject;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

@Getter
public class FigureStonePluginSettings implements Plugin<Settings> {

	public static final String PLUGIN_VERSION = "3.7.0-beta.19";

	public static final FigureStoneLogger LOGGER = new FigureStoneLogger("Settings");

	@Override
	public void apply(@NotNull Settings settings) {
		Properties gradleProperties = getGradleProperties(settings.getRootDir());
		settings.getRootProject().setName(FigureStoneUtils.getProperty(gradleProperties, "data.mod_name"));
		LOGGER.setup(settings.getRootProject().getName());
		LOGGER.log("Running FigureStonePlugin " + PLUGIN_VERSION);

		settings.getPlugins().apply("org.gradle.toolchains.foojay-resolver-convention");

		List<String> additionalDependencies = getAdditionalDependencies(gradleProperties);
		if (additionalDependencies.isEmpty()) {
			LOGGER.log("No additional dependencies!");
		} else {
			LOGGER.log("Found additional dependencies: [%s]".formatted(String.join(", ", additionalDependencies)));
		}

		List<String> loaders = getLoaders(gradleProperties);
		Map<String, List<String>> loadersAndVersions = getLoadersAndVersions(gradleProperties, loaders);

		GithubManager.apply(settings.getRootDir().toPath(), loaders);

		loadersAndVersions.forEach((loader, versions) -> {
			LOGGER.logModule(loader, "Found MC versions: [%s]".formatted(String.join(", ", versions)));
		});

		StonecutterManager.apply(settings, loadersAndVersions);
		List<FigureStoneProject> projects = getFigureStoneProjects(loadersAndVersions);
		AccessWidenerManager.apply(settings, projects);
		VersionedGradlePropertiesManager.apply(settings, gradleProperties, projects, additionalDependencies);
	}

	public static List<String> getAdditionalDependencies(Properties properties) {
		List<String> additionalDepends = new ArrayList<>();

		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			if (key.startsWith("dep.")) {
				int i = key.indexOf(".") + 1;
				String modId = key.substring(i);
				additionalDepends.add(modId);
			}
		}

		return additionalDepends;
	}

	public static Map<String, List<String>> getLoadersAndVersions(Properties gradleProperties, List<String> loaders) {
		HashMap<String, List<String>> map = new HashMap<>();
		for (String loader : loaders) {
			List<String> versions = Arrays.stream(FigureStoneUtils.getProperty(gradleProperties, "%s.multi_versions".formatted(loader)).split(" ")).toList();
			map.put(loader, versions);
		}
		return map;
	}

	public static List<FigureStoneProject> getFigureStoneProjects(Map<String, List<String>> loaderAndVersions) {
		List<FigureStoneProject> projects = new ArrayList<>();

		loaderAndVersions.forEach((loader, versions) -> {
			LoaderManager loaderManager = LoaderManager.of(loader);
			for (String version : versions) {
				projects.add(new FigureStoneProject("%s-%s".formatted(loader, version), loader, version, FigureStoneUtils.substringBefore(version, "-"), loaderManager));
			}
		});

		return projects;
	}

	public static List<String> getLoaders(Properties gradleProperties) {
		return Arrays.stream(FigureStoneUtils.getProperty(gradleProperties, "mod_loaders").split(" ")).toList();
	}

	public static @NotNull Properties getGradleProperties(File project) {
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(project.toPath().resolve("gradle.properties").toFile())) {
			properties.load(reader);
		} catch (Exception e) {
			FigureStonePluginSettings.LOGGER.log("Failed to read gradle.properties from \"%s\"!".formatted(project.getAbsolutePath()));
			e.printStackTrace(System.out);
		}
		return properties;
	}

}
