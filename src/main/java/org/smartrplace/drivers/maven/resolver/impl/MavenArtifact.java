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

class MavenArtifact {

	private final static char[] FILTER_CHARS = {'-', '.', '_'};
	private final String groupId;
	private final String artifactId;
	private final String versionId;
	private final int startLevel;
	private final boolean doStart;
	private volatile String bundleSymbolicName;

	MavenArtifact(int startLevel, boolean doStart) {
		this(null, null, null, startLevel, doStart);
	}

	MavenArtifact(String groupId, String artifactId, String versionId, int startLevel, boolean doStart) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.versionId = versionId;
		this.startLevel = startLevel > 0 ? startLevel : 4;
		this.doStart = doStart;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return versionId;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public boolean isDoStart() {
		return doStart;
	}

	public String getBundleSymbolicName() {
		String bsn = this.bundleSymbolicName;
		if (bsn != null)
			return bsn;
		bsn = getBundleSymbolicName(this);
		this.bundleSymbolicName = bsn;
		return bsn;
	}

	@Override
	public String toString() {
		return "MavenArtifact: " + groupId + "/" + artifactId + "/" + versionId;
	}

	private static String getBundleSymbolicName(MavenArtifact artifact) {
		final String groupId = artifact.getGroupId();
		final String artifactId = artifact.getArtifactId();
		if (groupId == null || groupId.isEmpty())
			return artifactId;
		if (artifactId == null || artifactId.isEmpty())
			return groupId;
		int overlap = 0;
		final int glength = groupId.length();
		for (int i=1; i <= Math.min(artifactId.length(), groupId.length()); i++) {
			final String substring = artifactId.substring(0, i);
			final int idx = groupId.lastIndexOf(substring);
			if (idx < 0)
				break;
			if (idx == glength - substring.length()) {
				overlap = i;
				if (i < artifactId.length() - 1 && contains(FILTER_CHARS, artifactId.charAt(i)))
					overlap++;
			}
		}
		if (overlap == 0)
			return groupId + "." + artifactId;
		if (overlap == artifactId.length())
			return groupId;
		return groupId + "." + artifactId.substring(overlap);
	}

	private static boolean contains(final char[] array, final char c) {
		for (char a : array) {
			if (a == c)
				return true;
		}
		return false;
	}

}
