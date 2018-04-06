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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

class Resolver {
	
	private final Collection<MavenArtifact> artifacts;
	private final BundleContext ctx;
	private final ResolverChain chain;
	
	Resolver(Collection<MavenArtifact> artifacts, BundleContext ctx) throws IOException {
		this.artifacts = artifacts;
		this.ctx = ctx;
		this.chain = new ResolverChain(ctx);
	}
	
	/**
	 * @return
	 */
	Collection<ResolvedArtifact> resolve() {
		final Bundle[] bundles = ctx.getBundles();
		final Map<String, Bundle> bundlesMap = new HashMap<>(bundles.length);
		for (Bundle b : bundles) {
			bundlesMap.put(b.getSymbolicName(), b);
		}
		final List<ResolvedArtifact> newBundles = new ArrayList<>(bundlesMap.size()+2);
		for (MavenArtifact artifact : artifacts) {
			final String bsn = artifact.getBundleSymbolicName();
			if (bundlesMap.containsKey(bsn))
				continue;
			try {
				if (chain.resolve(artifact)) {
					final InputStream in =  chain.resolveLocal(artifact);
					if (in == null) {
						MavenResolver.warn("Something went wrong... artifact " + artifact  +" not found");
						continue;
					}
					final Bundle b;
					try (final BufferedInputStream stream = new BufferedInputStream(in)) {
						b = ctx.installBundle("mvn-init:" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion(), stream);
					}
					if (b == null)
						throw new NullPointerException("Bundle is null");
					newBundles.add(new ResolvedArtifact(artifact, b));
				}
			} catch (Exception e) {
				MavenResolver.warn("Failed to install artifact " + artifact, e);
				continue;
			}
		}
		return newBundles;
	}
	
}
