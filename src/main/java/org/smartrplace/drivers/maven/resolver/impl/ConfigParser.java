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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ConfigParser {

	static ConfigFile parse(final BundleContext ctx) throws ParserConfigurationException, IOException, SAXException {
		final String path0 = ctx.getProperty(Properties.CONFIG_FILE_PROPERTY);
		final Path path = Paths.get(path0 != null ? path0 : Properties.CONFIG_FILE_DEFAULT);
		if (!Files.exists(path)) {
			MavenResolver.warn("No config.xml file found at " + path);
			return new ConfigFile();
		}
		final List<MavenArtifact> artifacts = new ArrayList<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document;
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
			document = builder.parse(bis);
			Node configuration = getUniqueSubnode(document, "configuration");
			Node bundles = getUniqueSubnode(configuration, "bundles");
			NodeList nl = bundles.getChildNodes();
			for (int i=0;i<nl.getLength();i++) {
				Node node = nl.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE || !"bundle".equals(node.getNodeName()))
					continue;
				NamedNodeMap attributes = node.getAttributes();
				if (attributes == null)
					continue;
				final Node groupNode = attributes.getNamedItem("groupId");
				final Node artifactNode = attributes.getNamedItem("artifactId");
				final Node versionNode = attributes.getNamedItem("version");
				if (groupNode == null || artifactNode == null || versionNode == null)
					continue;
				final Node startLevelNode = attributes.getNamedItem("startLevel");
				final int startLevel =  getStartLevel(startLevelNode);
				final Node startNode = attributes.getNamedItem("start");
				final boolean doStart = doStart(startNode);
				final MavenArtifact artifact = new MavenArtifact(
						groupNode.getTextContent(),
						artifactNode.getTextContent(),
						versionNode.getTextContent(),
						startLevel,
						doStart);
				artifacts.add(artifact);
			}
			final Node deleteNode = getUniqueSubnode(configuration, "deleteList");
			final List<String> deleteList;
			if (deleteNode == null)
				deleteList = null;
			else {
				deleteList = new ArrayList<>();
				final NodeList nl2 = deleteNode.getChildNodes();
				for (int i=0;i<nl2.getLength();i++) {
					Node node = nl2.item(i);
					if (node.getNodeType() != Node.ELEMENT_NODE || !"file".equals(node.getNodeName()))
						continue;
					String text = node.getTextContent();
					if (text == null)
						continue;
					text = text.trim();
					if (text.isEmpty())
						continue;
					deleteList.add(text);
				}
			}
			return new ConfigFile(artifacts, deleteList);
		}
	}

	private static boolean doStart(final Node startNode) {
		if (startNode == null)
			return true;
		final String text = startNode.getTextContent();
		try {
			return Boolean.parseBoolean(text);
		} catch (Exception e) {
			return true;
		}
	}

	private static int getStartLevel(final Node startLevelNode) {
		if (startLevelNode == null)
			return 4; // TODO configurable
		final String text = startLevelNode.getTextContent();
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return 4;
		}
 	}

	private static Node getUniqueSubnode(Node parent, String nodeType) {
		NodeList nl = parent.getChildNodes();
		for (int i=0;i<nl.getLength();i++) {
			Node sub = nl.item(i);
			if (sub.getNodeType() != Node.ELEMENT_NODE || !nodeType.equals(sub.getNodeName()))
				continue;
			return sub;
		}
		return null;
	}

}
