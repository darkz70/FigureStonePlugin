package net.darkz70.figurestoneplugin.core.extension;

import java.util.*;
import lombok.Getter;

@Getter
public class FigureStoneCoreAdditionalDependencies {

	private final Map<String, AdditionalDependencyOverride> overrides = new HashMap<>();
	private final Set<String> disabled = new HashSet<>();

	public void override(String configurationName, String modId) {
		this.overrides.put(modId, new AdditionalDependencyOverride(modId, configurationName));
	}

	public void disable(String modId) {
		this.disabled.add(modId);
	}

	public record AdditionalDependencyOverride(String modId, String configurationName) {

	}
}
