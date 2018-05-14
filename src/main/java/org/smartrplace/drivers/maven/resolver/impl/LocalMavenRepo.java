/**
 * Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.drivers.maven.resolver.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.smartrplace.drivers.maven.resolver.impl.checksums.Checksums;

class LocalMavenRepo implements Repository {

	final Path homeRepo;

	LocalMavenRepo() throws IOException {
		final Path repo;
		final String m2Home = System.getProperty("MAVEN_HOME");
		if (m2Home == null)
			repo = Paths.get(System.getProperty("user.home")).resolve(".m2");
		else
			repo = Paths.get(m2Home);
		homeRepo = repo.resolve("repository");
		if (!Files.exists(homeRepo))
			Files.createDirectories(homeRepo);
	}

	@Override
	public ResolutionResult resolve(MavenArtifact artifact) throws IOException {
		final Path file = resolveFile(artifact);
		return file == null ? null : new ResolutionResult(Files.newInputStream(file));
	}

	Path resolveFile(MavenArtifact artifact) throws MalformedURLException {
		Path dir = homeRepo;
		for (String pckg : artifact.getGroupId().split("\\.")) {
			if (pckg.isEmpty())
				continue;
			dir = dir.resolve(pckg);
		}
		final Path file = dir.resolve(artifact.getArtifactId()).resolve(artifact.getVersion())
			.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
		if (!Files.exists(file))
			return null;
		return file;
	}

	void installArtifact(final MavenArtifact artifact, final ResolutionResult result, boolean retry) throws IOException {
		Path dir = homeRepo;
		for (String pckg : artifact.getGroupId().split("\\.")) {
			if (pckg.isEmpty())
				continue;
			dir = dir.resolve(pckg);
		}

		dir = dir.resolve(artifact.getArtifactId()).resolve(artifact.getVersion());
		Files.createDirectories(dir);
		final Path file = dir
			.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
		Files.copy(result.result, file, StandardCopyOption.REPLACE_EXISTING);
		if (result.checksumAlgo != null) {
			final Path checksum = dir
					.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar." + result.checksumAlgo);
			Files.copy(result.checksumInput, checksum);
			if (!Checksums.getValidator(result.checksumAlgo).validate(Files.newInputStream(file), 
					toString(Files.newBufferedReader(checksum, StandardCharsets.UTF_8)).toString())) {
				Files.delete(file);
				Files.delete(checksum);
				if (retry)
					installArtifact(artifact, result, false);
				else
					throw new IOException("Failed to install artifact " + artifact + ": checksums do not match!");
			}
		}
		
	}

	private static StringBuilder toString(final BufferedReader reader) throws IOException {
		final char[] buff = new char[124];
		final StringBuilder sb = new StringBuilder();
		int read;
		while((read = reader.read(buff)) != -1) {
			sb.append(buff, 0, read);
			if (sb.length() > 1000)
				return null;
		}
		return sb;
	}
	
	@Override
	public String toString() {
		return "Local repository: " + homeRepo;
	}

}
