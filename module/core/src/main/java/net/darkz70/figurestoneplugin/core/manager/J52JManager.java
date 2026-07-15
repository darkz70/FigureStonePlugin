package net.darkz70.figurestoneplugin.core.manager;

import dev.kikugie.fletching_table.extension.FletchingTableExtension;
import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class J52JManager {

	public static void apply(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().configure(FletchingTableExtension.class, (extension) -> {
			var main = extension.getJ52j().register("main");
			main.configure((container) -> container.extension("json", "**/*.json5"));
		});
	}

}
