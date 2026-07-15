package net.darkz70.figurestoneplugin.core.loader;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import net.darkz70.figurestoneplugin.core.extension.FigureStoneCoreDependenciesExtension;
import net.darkz70.figurestoneplugin.core.manager.fabric.LoomManager;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class FabricLoaderManager implements LoaderManager {

	private static final FabricLoaderManager INSTANCE = new FabricLoaderManager();

	public static FabricLoaderManager getInstance() {
		return INSTANCE;
	}

	@Override
	public void applyPlugins(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		PluginContainer plugins = project.getPlugins();
		if (isRemapVersion(data)) {
			plugins.apply("fabric-loom");
		} else {
			plugins.apply("net.fabricmc.fabric-loom");
		}
	}

	@Override
	public void applyDependencies(@NotNull FigureStoneProjectConfigurationData data, FigureStoneCoreDependenciesExtension extension) {
		Project project = data.project();
		String minecraft = extension.getMinecraft();
		String fabricApi = extension.getFabricApi();
		String fabricLoader = extension.getFabricLoader();

		DependencyHandler dependencies = project.getDependencies();
		dependencies.add("minecraft", "com.mojang:minecraft:%s".formatted(minecraft));

		if (isRemapVersion(data)) {
			dependencies.add("mappings", ((LoomGradleExtensionAPI) project.getExtensions().getByName("loom")).officialMojangMappings());
		}

		dependencies.add(this.getModDependenciesImplementationMethod(data), "net.fabricmc.fabric-api:fabric-api:%s".formatted(fabricApi));
		dependencies.add(this.getModDependenciesImplementationMethod(data), "net.fabricmc:fabric-loader:%s".formatted(fabricLoader));
	}

	@Override
	public void configureExtensions(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().configure(LoomGradleExtensionAPI.class, (loom) -> {
			LoomManager.apply(data, loom);
		});
	}

	@Override
	public String getModDependenciesImplementationMethod(FigureStoneProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "implementation";
		}
		return "modImplementation";
	}

	@Override
	public String getJarTaskName(FigureStoneProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "jar";
		}
		return "remapJar";
	}

	@Override
	public String getAWFileExtension(FigureStoneProjectConfigurationData data) {
		if (!isRemapVersion(data)) {
			return "classTweaker";
		}
		return "accesswidener";
	}

	@Override
	public boolean excludeUselessFiles(FileCopyDetails details) {
		if (details.getName().contains("mods.toml")) {
			details.exclude();
			return true;
		}
		return false;
	}

	private static boolean isRemapVersion(FigureStoneProjectConfigurationData data) {
		StonecutterBuildExtension stonecutter = data.project().getStonecutter();
		return stonecutter.eval(data.comparableMinecraftVersion(), "<26.1");
	}

	@Override
	public Map<String, String> getLoaderConfigurations(List<String> configurations, FigureStoneProjectConfigurationData data) {
		Map<String, String> map = new HashMap<>();

		StonecutterBuildExtension stonecutter = data.project().getStonecutter();
		if (stonecutter.eval(data.comparableMinecraftVersion(), ">=26.1")) {
			for (String s : configurations) {
				map.put(s, s);
			}
			return map;
		}

		for (String s : configurations) {
			if (s.equals("include")) {
				map.put(s, s);
				continue;
			}
			map.put(s, "mod" + String.valueOf(s.charAt(0)).toUpperCase(Locale.ROOT) + s.substring(1));
		}
		return map;
	}
}
