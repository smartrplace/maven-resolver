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
import java.nio.file.Paths;

import org.osgi.framework.BundleContext;
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
			URLConstants.URL_HANDLER_PROTOCOL+"=file-init"
		}
)
public class UrlHandlerFileInit extends AbstractURLStreamHandlerService {

	private volatile Path initDir;

	@Activate
	protected void start(BundleContext ctx) {
		final String initDir = ctx.getProperty(Properties.INIT_DIR_PROPERTY);
		this.initDir = Paths.get(initDir != null ? initDir : Properties.INIT_DIR_DEFAULT);
	}

	@Deactivate
	protected void stop() {
		this.initDir = null;
	}

	@Override
	public URLConnection openConnection(URL url) throws IOException {
		final Path file = Paths.get(url.getPath());
		final String newFile = initDir.resolve(file).toString();
		final URL copy= new URL("file", url.getHost(), url.getPort(), newFile);
		return copy.openConnection();
	}

}
