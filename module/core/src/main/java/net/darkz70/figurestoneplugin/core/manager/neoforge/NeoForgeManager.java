package net.darkz70.figurestoneplugin.core.manager.neoforge;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestoneplugin.common.FigureStoneUtils;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import net.darkz70.figurestoneplugin.core.extension.FigureStoneCoreDependenciesExtension;
import net.neoforged.moddevgradle.dsl.*;
import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class NeoForgeManager {

	public static void apply(@NotNull FigureStoneProjectConfigurationData data, ModDevExtension extension, FigureStoneCoreDependenciesExtension dependencies, String platform) {
		Project project = data.project();

		Properties personalProperties = project.getPersonalProperties();

		String playerNickname = FigureStoneUtils.getPlayerNickname(personalProperties);
		Map<String, UUID> altAccounts = FigureStoneUtils.getAltAccounts(personalProperties);
		UUID playerUuid = FigureStoneUtils.getPlayerUuid(personalProperties);
		Object quickPlayWorld = personalProperties.get("quick_play_world");

		if (data.project().getStonecutter().eval(data.comparableMinecraftVersion(), "<26.1")) {
			Parchment parchment = extension.getParchment();
			parchment.getMappingsVersion().set(dependencies.getParchment());
			parchment.getMinecraftVersion().set(dependencies.getMinecraft());
		}

		extension.getValidateAccessTransformers().set(true);
		extension.getAccessTransformers().from("../../src/main/resources/aws/%s-%s.cfg".formatted(data.loaderName(), data.minecraftVersion()));

		String sides = data.project().getProperty("data.sides").toLowerCase(Locale.ROOT);
		boolean createClient = sides.equals("client") || sides.equals("both");
		boolean createServer = sides.equals("server") || sides.equals("both");

		extension.runs((container) -> {
			Path runs = project.getRootProject().getProjectDir().toPath().resolve("runs");

			if (createClient) {
				RunModel client = container.create("client");
				client.client();
				client.getGameDirectory().set(runs.resolve("client").toFile());
				addProgramArg(client, "--username", playerNickname);
				addProgramArg(client, "--uuid", playerUuid);
				addProgramArg(client, "--quickPlaySingleplayer", quickPlayWorld);

				for (Entry<String, UUID> entry : altAccounts.entrySet()) {
					String runName = "client_" + entry.getKey();

					RunModel altClient = container.create(runName);
					altClient.client();
					altClient.getGameDirectory().set(runs.resolve(runName).toFile());
					addProgramArg(altClient, "--username", entry.getKey());
					addProgramArg(altClient, "--uuid", entry.getValue());
					addProgramArg(altClient, "--quickPlaySingleplayer", quickPlayWorld);

					altClient.getIdeName().set("%s / %s / client / %s".formatted(platform, data.minecraftVersion(), entry.getKey()));
				}

				client.getIdeName().set("%s / %s / %s".formatted(platform, data.minecraftVersion(), "client"));
			}

			if (createServer) {
				RunModel server = container.create("server");
				server.server();
				server.programArgument("--nogui");
				server.getGameDirectory().set(runs.resolve("server").toFile());
				server.getIdeName().set("%s / %s / %s".formatted(platform, data.minecraftVersion(), "server"));
			}
		});

		extension.mods((container) -> {
			NamedDomainObjectProvider<ModModel> provider = container.register(project.getProperty("data.mod_id"));
			provider.configure((model) -> {
				JavaPluginExtension javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
				model.sourceSet(javaPlugin.getSourceSets().getByName("main"));
			});
		});
	}

	private static void addProgramArg(RunModel client, String key, Object argument) {
		if (argument == null || argument.toString().equals("none")) {
			return;
		}
		client.programArgument(key);
		client.programArgument(argument.toString());
	}

	private static void addVMArgument(RunModel client, String key, Object argument) {
		if (argument == null || argument.toString().equals("none")) {
			return;
		}
		client.jvmArgument(key);
		client.jvmArgument(argument.toString());
	}

}
