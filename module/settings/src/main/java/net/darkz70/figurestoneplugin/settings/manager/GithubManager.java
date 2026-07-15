package net.darkz70.figurestoneplugin.settings.manager;

import java.nio.file.*;
import java.util.*;
import net.darkz70.figurestoneplugin.settings.FigureStonePluginSettings;

public class GithubManager {

	public static void apply(Path root, List<String> loaderAndVersions) {
		Path path = root.resolve(".github/workflows/build.yml");
		if (!Files.exists(path)) {
			return;
		}
		try {
			String content = new String(Files.readAllBytes(path));

			content = content.replaceAll(
					"platform:\\s*\\[[^\\]]*\\]",
					"platform: [%s]".formatted(String.join(", ", loaderAndVersions))
			);

			Files.write(path, content.getBytes());
		} catch (Exception e) {
			FigureStonePluginSettings.LOGGER.log("Failed to sync github build workflow!");
			e.printStackTrace(System.out);
		}
	}

}
