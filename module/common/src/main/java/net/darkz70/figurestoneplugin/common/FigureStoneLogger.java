package net.darkz70.figurestoneplugin.common;

import java.util.function.Supplier;
import org.gradle.api.Project;

public class FigureStoneLogger {

	private Supplier<String> name;
	private final String pluginModule;

	public FigureStoneLogger(String pluginModule) {
		this.pluginModule = pluginModule;
	}

	public void setup(String name) {
		this.name = () -> name;
	}

	public void setup(Project project) {
		this.name = project::getName;
	}

	@SuppressWarnings("all")
	public void log(String text, Object... objects) {
		System.out.println("[FigureStone%s/%s] %s".formatted(this.pluginModule, this.name.get(), text.formatted(objects)));
	}

	@SuppressWarnings("all")
	public void logModule(String module, String text, Object... objects) {
		System.out.println("[FigureStone%s/%s/%s] %s".formatted(this.pluginModule, this.name.get(), module, text.formatted(objects)));
	}

}
