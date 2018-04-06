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
import java.io.InputStream;
import java.net.URL;

class RemoteRepository implements Repository {

	private final Client client;
	private final URL url;

	RemoteRepository(URL url, Client client) {
		this.url = url;
		this.client = client;
	}

	@Override
	public InputStream resolve(MavenArtifact artifact) throws IOException {
		return client.download(url.toString(), artifact);
	}

	@Override
	public String toString() {
		return "RemoteRepository: " + url;
	}

}
