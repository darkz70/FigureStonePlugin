package net.darkz70.figurestoneplugin.settings.api;

import com.google.gson.*;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.darkz70.figurestoneplugin.settings.FigureStonePluginSettings;
import org.jetbrains.annotations.NotNull;

public class ModrinthDependenciesAPI {

	@NotNull
	public static String getVersion(String modId, String minecraftVersion, String loader) {
		String encodedLoader = URLEncoder.encode("[\"%s\"]".formatted(loader), StandardCharsets.UTF_8);
		String encodedMinecraftVersion = URLEncoder.encode("[\"%s\"]".formatted(minecraftVersion), StandardCharsets.UTF_8);
		String url = "https://api.modrinth.com/v2/project/%s/version?loaders=%s&game_versions=%s".formatted(modId, encodedLoader, encodedMinecraftVersion);
		JsonElement element;
		try {
			element = JsonHelper.get(url);
		} catch (FileNotFoundException e) {
			FigureStonePluginSettings.LOGGER.log("Failed to find Modrinth project with id \"%s\"", modId);
			e.printStackTrace(System.out);
			return "unknown";
		} catch (Exception e) {
			FigureStonePluginSettings.LOGGER.log("Failed to get version from Modrinth project with id \"%s\"", modId);
			e.printStackTrace(System.out);
			return "unknown";
		}
		JsonArray array = element.getAsJsonArray();
		if (array.isEmpty()) {
			return "unknown";
		}
		JsonElement jsonElement = array.get(0);
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		String versionNumber = jsonObject.get("version_number").getAsString();

		if (versionNumber.contains(minecraftVersion)) {
			return versionNumber;
		}

		return jsonObject.get("id").getAsString();
	}

}
