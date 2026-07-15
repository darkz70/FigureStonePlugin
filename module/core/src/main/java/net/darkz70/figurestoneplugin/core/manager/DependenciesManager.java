package net.darkz70.figurestoneplugin.core.manager;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestoneplugin.common.FigureStoneUtils;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import net.darkz70.figurestoneplugin.core.extension.*;
import net.darkz70.figurestoneplugin.core.extension.FigureStoneCoreAdditionalDependencies.AdditionalDependencyOverride;
import net.darkz70.figurestoneplugin.core.loader.LoaderManager;
import org.gradle.api.*;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.repositories.*;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class DependenciesManager {

	private static void addCustomConfigurations(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		LoaderManager loaderManager = data.loaderManager();

		ConfigurationContainer configurations = project.getConfigurations();

		Map<String, String> loaderList = loaderManager.getLoaderConfigurations(
				List.of("include", "api", "implementation", "compileOnly", "runtimeOnly"),
				data
		);

		for (Entry<String, String> entry : loaderList.entrySet()) {
			String originalName = entry.getKey();
			String loaderName = entry.getValue();
			String name = "figurestone" + String.valueOf(originalName.charAt(0)).toUpperCase(Locale.ROOT) + originalName.substring(1);

			Configuration created = loaderManager.registerCustomConfiguration(data, name, originalName, loaderName);
			configurations.named(loaderName).configure((action) -> action.extendsFrom(created));
		}
	}

	private static void addDependencies(@NotNull FigureStoneProjectConfigurationData data, FigureStoneCoreDependenciesExtension extension) {
		Project project = data.project();
		LoaderManager loaderManager = data.loaderManager();

		String minecraft = extension.getMinecraft();
		String lombok = extension.getLombok();

		DependencyHandler dependencies = project.getDependencies();
		loaderManager.applyDependencies(data, extension);
		dependencies.add("compileOnly", "org.projectlombok:lombok:%s".formatted(lombok));
		dependencies.add("annotationProcessor", "org.projectlombok:lombok:%s".formatted(lombok));

		Map<String, String> properties = project.getFigureStoneProperties("dep");
		FigureStoneCoreAdditionalDependencies additional = extension.getAdditional();
		additional.disable("yacl");

		String modImplementation = loaderManager.getModDependenciesImplementationMethod(data);

		Map<String, AdditionalDependencyOverride> overrides = additional.getOverrides();
		Set<String> disabled = additional.getDisabled();
		properties.forEach((modId, version) -> {
			if (disabled.contains(modId)) {
				return;
			}

			if (version.equals("unknown")) {
				return;
			}

			AdditionalDependencyOverride override = overrides.get(modId);
			String configurationName = override != null ? override.configurationName() : modImplementation;
			dependencies.add(configurationName, "maven.modrinth:%s:%s".formatted(modId, version));
		});

		project.getConfigurations().forEach((configuration) -> {
			configuration.resolutionStrategy((strategy) -> {
				strategy.force("com.twelvemonkeys.common:common-io:3.10.0");
				strategy.force("com.twelvemonkeys.common:common-lang:3.10.0");
				strategy.force("com.twelvemonkeys.common:common-image:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-metadata:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-webp:3.10.0");
				strategy.force("com.twelvemonkeys.imageio:imageio-core:3.10.0");
			});
		});

		String yaclVersion = properties.get("yacl");
		if (yaclVersion != null && !yaclVersion.equals("unknown")) {
			Set<String> oldMavenVersions = Set.of("1.19.4", "1.20", "1.20.2", "1.20.3");
			AdditionalDependencyOverride override = overrides.get("yacl");
			String configurationName = override != null ? override.configurationName() : modImplementation;
			if (oldMavenVersions.contains(minecraft)) {
				dependencies.add(configurationName, "dev.isxander.yacl:yet-another-config-lib-fabric:%s".formatted(FigureStoneUtils.substringBeforeLast(yaclVersion, "-")));
			} else {
				dependencies.add(configurationName, "dev.isxander:yet-another-config-lib:%s".formatted(yaclVersion));
			}
		}
	}

	private static void addRepositories(Project project) {
		project.getRepositories().mavenCentral();
		addRepository(project, "Forge", "https://maven.minecraftforge.net");
		addRepository(project, "Minecraft libraries", "https://libraries.minecraft.net");
		addRepository(project, "Quilt", "https://maven.quiltmc.org/repository/release/");
		addRepository(project, "Sonatype", "https://oss.sonatype.org/content/repositories/snapshots/");
		addRepository(project, "Terraformers", "https://maven.terraformersmc.com/");
		addRepository(project, "YACL", "https://maven.isxander.dev/releases");
		addRepository(project, "Nucleoid", "https://maven.nucleoid.xyz/");
		addRepository(project, "Modrinth", "https://api.modrinth.com/maven", (repository) -> {
			repository.content((descriptor) -> {
				descriptor.includeGroup("maven.modrinth");
			});
		});
		addRepository(project, "Sponge", "https://repo.spongepowered.org/repository/maven-public", (repository) -> {
			project.getRepositories().exclusiveContent((content) -> {
				content.forRepositories(repository);
				@SuppressWarnings("all")
				ExclusiveContentRepository filter = content.filter((descriptor) -> {
					descriptor.includeGroupAndSubgroups("org.spongepowered");
				});
			});
		});
		addRepository(project, "YACL Kotlin For Forge", "https://thedarkcolour.github.io/KotlinForForge/", (repository) -> {
			project.getRepositories().exclusiveContent((content) -> {
				content.forRepositories(repository);
				@SuppressWarnings("all")
				ExclusiveContentRepository filter = content.filter((descriptor) -> {
					descriptor.includeGroup("thedarkcolour");
				});
			});
		});
	}

	private static void addRepository(Project project, String name, String url) {
		addRepository(project, name, url, (repository) -> {});
	}

	private static void addRepository(Project project, String name, String url, Consumer<MavenArtifactRepository> consumer) {
		project.getRepositories().maven((repository) -> {
			repository.setName(name);
			repository.setUrl(url);
			consumer.accept(repository);
		});
	}

	public static void apply(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().create("figurestoneDependencies", FigureStoneCoreDependenciesExtension.class);

		addRepositories(project);
		addCustomConfigurations(data);

		project.getGradle().addProjectEvaluationListener(new ProjectEvaluationListener() {
			@Override
			public void beforeEvaluate(@NotNull Project project) {
			}

			@Override
			public void afterEvaluate(@NotNull Project project, @NotNull ProjectState state) {
				FigureStoneCoreDependenciesExtension extension = project.getExtensions().getByType(FigureStoneCoreDependenciesExtension.class);
				DependenciesManager.addDependencies(data, extension);
				project.getGradle().removeProjectEvaluationListener(this);
			}
		});
	}
}
