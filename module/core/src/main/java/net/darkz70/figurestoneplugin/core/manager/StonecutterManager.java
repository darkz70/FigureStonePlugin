package net.darkz70.figurestoneplugin.core.manager;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestoneplugin.common.FigureStoneUtils;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class StonecutterManager {

	public static void apply(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		FigureStonePluginCore plugin = data.plugin();

		StonecutterBuildExtension stonecutter = project.getStonecutter();

		String mcVersion = plugin.getProjectMultiVersion().projectVersion();
		Map<String, String> properties = project.getFigureStoneProperties("data");
		properties.putAll(project.getFigureStoneProperties("build"));
		Map<String, String> dependencies = project.getFigureStoneProperties("dep");
		properties.putAll(dependencies);
		properties.put("java", String.valueOf(plugin.getJavaVersionIndex()));
		properties.put("minecraft", mcVersion);
		properties.put("fabric_api_id", project.getStonecutter().compare("1.19.1", mcVersion) >= 0 ? "fabric" : "fabric-api");
		properties.put("mod_version", project.getVersion().toString());

		properties.forEach((key, value) -> {
			stonecutter.getSwaps().put(key, getFormatted(value));
		});

		dependencies.forEach((modId, version) -> {
			stonecutter.getConstants().put(modId, !version.equals("unknown"));
		});

		Arrays.stream(project.getProperty("mod_loaders").split(" ")).forEach((loader) -> {
			stonecutter.getConstants().put(loader, project.getName().startsWith(loader));
		});

		stonecutter.replacements((container) -> {
			container.string((spec) -> {
				spec.getDirection().set(stonecutter.getCurrent().getParsed().matches(">=1.21.11"));
				spec.replace("ResourceLocation", "Identifier");
				spec.replace(".location()", ".identifier()");
				spec.replace("::location", "::identifier");
			});

			container.string((spec) -> {
				spec.getDirection().set(stonecutter.getCurrent().getProject().contains("forge"));
				spec.getId().set("client_fabric_commands");
				spec.replace("import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;", "import net.minecraft.commands.CommandSourceStack;");
				spec.replace("FabricClientCommandSource", "CommandSourceStack");
			});
		});

		stonecutter.getFilters().exclude("resources/aws/**");
	}

	private static @NotNull String getFormatted(String modVersion) {
		return "\"%s\";".formatted(modVersion);
	}
}
