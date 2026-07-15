package net.darkz70.figurestoneplugin.core.loader;

import java.util.*;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import net.darkz70.figurestoneplugin.core.extension.FigureStoneCoreDependenciesExtension;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCopyDetails;
import org.jetbrains.annotations.NotNull;

public interface LoaderManager {

	static LoaderManager of(String loader) {
		if (loader.equals("forge")) {
			return ForgeLoaderManager.getInstance();
		} else if (loader.contains("neoforge")) {
			return NeoForgeLoaderManager.getInstance();
		} else if (loader.contains("fabric")) {
			return FabricLoaderManager.getInstance();
		} else {
			throw new RuntimeException("Unsupported loader \"%s\"!".formatted(loader));
		}
	}

	void applyPlugins(@NotNull FigureStoneProjectConfigurationData data);

	void applyDependencies(@NotNull FigureStoneProjectConfigurationData data, FigureStoneCoreDependenciesExtension extension);

	void configureExtensions(@NotNull FigureStoneProjectConfigurationData data);

	boolean excludeUselessFiles(FileCopyDetails details);

	String getModDependenciesImplementationMethod(FigureStoneProjectConfigurationData data);

	String getJarTaskName(FigureStoneProjectConfigurationData data);

	String getAWFileExtension(FigureStoneProjectConfigurationData data);

	Map<String, String> getLoaderConfigurations(List<String> configurations, FigureStoneProjectConfigurationData data);

	default Configuration registerCustomConfiguration(@NotNull FigureStoneProjectConfigurationData data, String name, String originalName, String loaderName) {
		return data.project().getConfigurations().create(name);
	}
}
