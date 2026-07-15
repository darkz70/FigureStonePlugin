package net.darkz70.figurestoneplugin.settings.api;

import net.darkz70.figurestoneplugin.settings.FigureStonePluginSettings;

public class ForgeDependenciesAPI {

	public static String getForgeVersion(String minecraft) {
		try {
			return JsonHelper.get("https://maven.minecraftforge.net/api/maven/latest/version/releases/net/minecraftforge/forge?filter=%s-".formatted(minecraft))
					.getAsJsonObject()
					.get("version")
					.getAsString();
		} catch (Exception e) {
			FigureStonePluginSettings.LOGGER.log("Failed to find forge version!");
			e.printStackTrace(System.out);
			return "unknown";
		}
	}

}
