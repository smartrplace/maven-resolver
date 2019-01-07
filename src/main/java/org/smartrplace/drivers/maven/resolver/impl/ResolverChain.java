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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.osgi.framework.BundleContext;

class ResolverChain {

	private final static String MAVEN_CENTRAL = "http://central.maven.org/maven2";
	private final LocalMavenRepo local;
	private final Queue<Repository> chain = new ArrayDeque<>();

	ResolverChain(BundleContext ctx) throws IOException {
		local = new LocalMavenRepo();
		chain.add(local);
		final Client client = new Client();
		for (URL url : repos(ctx)) {
			chain.add(new RemoteRepository(url, client));
		}
		MavenResolver.info("Maven repositories: " + chain);
	}

	private List<URL> repos(BundleContext ctx) {
		String reposFile0 = ctx.getProperty(Properties.REPOSITORIES_FILE_PROPERTY);
		if (reposFile0 == null)
			reposFile0 = Properties.REPOSITORIES_FILE_DEFAULT;
		final Path reposFile = Paths.get(reposFile0);
		if (!Files.exists(reposFile)) {
			try {
				return Collections.singletonList(new URL(MAVEN_CENTRAL));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		final List<URL> urls = new ArrayList<>();
		try (final BufferedReader reader = Files.newBufferedReader(reposFile, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#"))
					continue;
				final String[] entries = line.split(",");
				for (String entry : entries) {
					entry = entry.trim();
					if (entry.endsWith("\\"))
						entry = entry.substring(0, entry.length()-1);
					if (entry.isEmpty())
						continue;
					try {
						urls.add(new URL(entry));
					} catch (MalformedURLException e) {
						MavenResolver.warn("Invalid repository URL " + entry);
					}
				}
			}

		} catch (IOException e) {
			return Collections.emptyList();
		}
		return urls;
	}

	InputStream resolveLocal(final MavenArtifact artifact) throws IOException {
		return local.resolve(artifact).result;
	}

	Path resolveLocalUrl(final MavenArtifact artifact) throws IOException {
		return local.resolveFile(artifact);
	}

	boolean resolve(final MavenArtifact artifact) throws IOException {
		for (Repository r : chain) {
			try (final ResolutionResult result = r.resolve(artifact)) {
				if (result != null) {
					if (r != local) {
						try {
							local.installArtifact(artifact, result, true);
						} catch (IOException e) {
							MavenResolver.warn("Failed to install artifact " + artifact + " in local Maven repository", e);
							return false;
						}
					}
					return true;
				}
			}
		}
		MavenResolver.debug("Artifact {} not found via Maven",artifact);
		return false;
	}



}
