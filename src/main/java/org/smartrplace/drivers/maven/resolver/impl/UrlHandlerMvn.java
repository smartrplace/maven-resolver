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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

@Component(
		service=URLStreamHandlerService.class,
		scope=ServiceScope.SINGLETON,
		property= {
			URLConstants.URL_HANDLER_PROTOCOL+"=mvn",
			Constants.SERVICE_RANKING+"=" + Integer.MIN_VALUE,
		}
)
public class UrlHandlerMvn extends AbstractURLStreamHandlerService {
	
	private volatile ResolverChain resolverChain;
	
	@Activate
	protected void activate(final BundleContext ctx) throws IOException {
		this.resolverChain = new ResolverChain(ctx);
	}
	
	@Deactivate
	protected void deactivate() {
		this.resolverChain = null;
	}
	
	@Override
	public URLConnection openConnection(URL url) throws IOException {
		if (!"mvn".equalsIgnoreCase(url.getProtocol()))
			throw new IllegalArgumentException("Unsupported protocol " + url.getProtocol());
		final String[] components = url.getPath().split("/");
		if (components.length < 3)
			throw new IllegalArgumentException("Invalid Maven coordinates " + url.getPath());
		final ResolverChain chain = this.resolverChain;
		if (chain == null)
			throw new IllegalStateException("Service is inactive");
		final String groupId = components[0];
		final String artifactId = components[1];
		final String version = components[2];
		final MavenArtifact artifact = new MavenArtifact(groupId, artifactId, version, 1, true);
		if (!resolverChain.resolve(artifact))
			throw new IllegalStateException("Artifact not found: " + url.getPath());
		final Path newUrl = resolverChain.resolveLocalUrl(artifact);
		return newUrl.toUri().toURL().openConnection();
	}

}
