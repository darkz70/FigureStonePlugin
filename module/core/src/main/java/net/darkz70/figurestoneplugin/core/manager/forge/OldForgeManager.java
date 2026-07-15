package net.darkz70.figurestoneplugin.core.manager.forge;

//import java.nio.file.Path;
//import lombok.experimental.ExtensionMethod;
//import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
//import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
//import net.darkz70.figurestoneplugin.core.extension.FigureStoneCoreDependenciesExtension;
//import net.minecraftforge.gradle.common.util.*;
//import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension;
//import org.gradle.api.*;
//import org.jetbrains.annotations.NotNull;
//
//@ExtensionMethod(FigureStonePluginCore.class)
//public class ForgeManager {
//
//	public static void apply(@NotNull FigureStoneProjectConfigurationData data, FigureStoneCoreDependenciesExtension extension) {
//		Project project = data.project();
//		LegacyForgeExtension forge = project.getExtensions().getByType(LegacyForgeExtension.class);
//
//		forge.mappings("parchment", extension.getParchment());
//
//		forge.getAccessTransformers().from("../../src/main/resources/aws/neoforge-%s.cfg".formatted(data.minecraftVersion()));
//
//		Path workingDirectory = project.getRootProject().getProjectDir().toPath().resolve("runs");
//		NamedDomainObjectContainer<RunConfig> runs = forge.getRuns();
//		RunConfig client = runs.create("client");
//		client.client(true);
//		client.setWorkingDirectory(workingDirectory.resolve("client").toAbsolutePath().toString());
//
//		RunConfig server = runs.create("server");
//		server.client(false);
//		server.arg("--nogui");
//		server.setWorkingDirectory(workingDirectory.resolve("server").toAbsolutePath().toString());
//
//		runs.configureEach((run) -> {
//			run.arg("-mixin.config=%s.mixins.json".formatted(data.project().getProperty("data.mod_id")));
//		});
//	}
//
//}
