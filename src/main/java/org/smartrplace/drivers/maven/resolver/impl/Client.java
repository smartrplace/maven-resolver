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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

class Client {

	private final CloseableHttpClient client = HttpClients.createDefault();
	
	InputStream download(final String baseUrl, final MavenArtifact artifact) throws ClientProtocolException, IOException {
		return download(baseUrl, artifact, null);
	}

	InputStream download(final String baseUrl, final MavenArtifact artifact, String checksumAlgo) throws ClientProtocolException, IOException {
		final StringBuilder sb = new StringBuilder();
		if (baseUrl.endsWith("/"))
			sb.append(baseUrl.substring(0, baseUrl.length()-1));
		else
			sb.append(baseUrl);
		for (String cmp : artifact.getGroupId().split("\\.")) {
			sb.append('/').append(cmp);
		}
		sb.append('/').append(artifact.getArtifactId()).append('/').append(artifact.getVersion()).append('/')
			.append(artifact.getArtifactId()).append('-').append(artifact.getVersion()).append(".jar");
		if (checksumAlgo != null) {
			sb.append('.').append(checksumAlgo);
		}
		final HttpGet get = new HttpGet(sb.toString());
		final CloseableHttpResponse resp = client.execute(get);
		if (resp.getStatusLine().getStatusCode() / 100 != 2) {
			closeSmoothly(resp);
			return null;
		}
		return new InnerInputStream(resp.getEntity().getContent(), resp);
	}
	
	static void closeSmoothly(final Closeable stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (Exception ignore) {}
	}

	private static class InnerInputStream extends BufferedInputStream {

		private final Closeable closeable;

		InnerInputStream(InputStream stream, Closeable closeable) {
			super(stream);
			this.closeable = closeable;
		}

		@Override
		public void close() throws IOException {
			if (closeable != null) {
				try {
					closeable.close();
				} catch (Exception ignore) {}
			}
			super.close();
		}

	}


}
