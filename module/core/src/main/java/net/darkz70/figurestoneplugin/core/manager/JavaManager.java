package net.darkz70.figurestoneplugin.core.manager;

import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestoneplugin.core.FigureStonePluginCore;
import net.darkz70.figurestoneplugin.core.data.FigureStoneProjectConfigurationData;
import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(FigureStonePluginCore.class)
public class JavaManager {

	public static void apply(@NotNull FigureStoneProjectConfigurationData data) {
		Project project = data.project();
		FigureStonePluginCore plugin = data.plugin();

		int javaVersionIndex = plugin.getJavaVersionIndex();
		JavaVersion javaVersion = plugin.getJavaVersion();

		TaskCollection<JavaCompile> collection = project.getTasks().withType(JavaCompile.class);
		for (JavaCompile javaCompile : collection) {
			javaCompile.getOptions().getRelease().set(javaVersionIndex);
		}

		JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(javaVersion.getMajorVersion()));
		javaExtension.setSourceCompatibility(javaVersion);
		javaExtension.setTargetCompatibility(javaVersion);
	}
}
