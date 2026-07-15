package net.darkz70.figurestoneplugin.core.data;

import net.darkz70.figurestoneplugin.common.FigureStoneUtils;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.loader.LoaderManager;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public record FigureStoneProjectConfigurationData(
		FigureStonePluginCore plugin,
		String projectName,
		String loaderName,
		String minecraftVersion,
		String comparableMinecraftVersion,
		LoaderManager loaderManager,
		Project project
) {

	public static FigureStoneProjectConfigurationData create(@NotNull Project project, FigureStonePluginCore plugin) {
		String projectName = project.getName();
		String loaderName = FigureStoneUtils.substringBefore(projectName, "-");
		String minecraftVersion = FigureStoneUtils.substringSince(projectName, "-");
		String comparableMinecraftVersion = FigureStoneUtils.substringBefore(minecraftVersion, "-");
		LoaderManager loaderManager = LoaderManager.of(loaderName);
		return new FigureStoneProjectConfigurationData(plugin, projectName, loaderName, minecraftVersion, comparableMinecraftVersion, loaderManager, project);
	}
}
