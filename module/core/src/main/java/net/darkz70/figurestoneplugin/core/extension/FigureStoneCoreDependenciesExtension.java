package net.darkz70.figurestoneplugin.core.extension;

import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.tasks.*;

@Getter
public class FigureStoneCoreDependenciesExtension {

	@Input
	String minecraft;

	@Input
	String fabricApi;

	@Input
	String fabricLoader;

	@Input
	String neoForge;

	@Input
	String forge;

	@Input
	String parchment;

	@Input
	String lombok;

	@Input
	String disableMixinAp;

	@Nested
	FigureStoneCoreAdditionalDependencies additional = new FigureStoneCoreAdditionalDependencies();

	public void additional(Action<FigureStoneCoreAdditionalDependencies> action) {
		action.execute(this.additional);
	}
}
