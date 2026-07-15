package net.darkz70.figurestoneplugin.settings.project;

import net.darkz70.figurestoneplugin.settings.loader.LoaderManager;

public record FigureStoneProject(
		String projectName,
		String loaderName,
		String minecraftVersion,
		String comparableMinecraftVersion,
		LoaderManager loaderManager
) {

}
