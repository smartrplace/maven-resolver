package org.smartrplace.drivers.maven.resolver.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ConfigFile {

	private final List<MavenArtifact> artifacts;
	private final List<Path> deleteFiles;
	
	public ConfigFile() {
		this(Collections.<MavenArtifact> emptyList(), null);
	}
	
	public ConfigFile(List<MavenArtifact> artifacts, List<String> deleteFiles) {
		this.artifacts = Collections.unmodifiableList(artifacts);
		if (deleteFiles == null || deleteFiles.isEmpty())
			this.deleteFiles = Collections.emptyList();
		else {
			final List<Path> paths = new ArrayList<>(deleteFiles.size());
			for (String f : deleteFiles) {
				paths.add(Paths.get(f));
			}
			this.deleteFiles  =Collections.unmodifiableList(paths);
		}
	}
	
	public List<MavenArtifact> getArtifacts() {
		return artifacts;
	}
	
	public List<Path> getDeleteFiles() {
		return deleteFiles;
	}
	
	static ConfigFile merge(Collection<ConfigFile> files) {
		final List<MavenArtifact> artifacts = new ArrayList<>();
		final List<String> deleteFiles = new ArrayList<>();
		for (ConfigFile cf : files) {
			artifacts.addAll(cf.getArtifacts());
			for (Path p : cf.getDeleteFiles()) {
				deleteFiles.add(p.toString());
			}
		}
		return new ConfigFile(artifacts, deleteFiles);
	}
	
}
