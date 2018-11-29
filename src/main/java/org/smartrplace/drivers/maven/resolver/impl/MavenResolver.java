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
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class MavenResolver implements BundleActivator {

	private final Semaphore initLock = new Semaphore(1);

	@Override
	public void start(final BundleContext ctx) throws Exception {
		if (!initLock.tryAcquire())
			return;
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final File cleanMarker = ctx.getDataFile("cleanMarker");
					if (cleanMarker.exists()) {
						debug("Unclean start detected.");
						setStartLevel();
						return;
					}
					debug("Clean start detected, now launching bundles.");
					final ConfigFile cfg = ConfigParser.parse(ctx);
					cleanUp(cfg.getDeleteFiles(), ctx);
					startBundles(cfg.getArtifacts());
					cleanMarker.createNewFile();
				} catch (ParserConfigurationException | IOException | SAXException e) {
					MavenResolver.warn("Initialization failed", e);
				} finally {
					initLock.release();
				}
			}
			
			// called on clean start only
			private void cleanUp(final List<Path> deleteFiles, final BundleContext ctx) {
				final Path workingDir = Paths.get(".");
				final String property = ctx.getProperty("org.osgi.framework.storage");
				final Path osgiStorageDir; // TODO the method below is not reliable
				if (property != null) {
					osgiStorageDir = Paths.get(property);
				} else {
					osgiStorageDir = Paths.get("felix-cache");
				}
				for (Path path : deleteFiles) {
					if (!isSubdir(workingDir, path) || workingDir.normalize().equals(path.normalize())) {
						warn("Specified delete file " + path + " is not a subpath of the working dir, omitting this.");
						continue;
					}
					if (isSubdir(osgiStorageDir, path)) {
						debug("Skipping deletion of OSGi storage dir {}", path);
						continue;
					}
					try {
						deleteRecursively(path);
					} catch (IOException e) {
						warn("Failed to delete file/folder " + path, e);
					}
				}
			}
			
			private void deleteRecursively(final Path p) throws IOException {
				if (Files.isRegularFile(p)) {
					debug("Deleting file {}", p);
					Files.delete(p);
				}
				else if (Files.isDirectory(p)) {
					debug("Deleting folder {}", p);
					Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
						
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							Files.delete(dir);
						    return FileVisitResult.CONTINUE;
						}
					});
				}
			}

			private void setStartLevel() {
				int maxStartLevel = 1;
				for (Bundle b:  ctx.getBundles()) {
					final BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
					if (bsl.getStartLevel() > maxStartLevel)
						maxStartLevel = bsl.getStartLevel();
				}
				final FrameworkStartLevel fsl = ctx.getBundle(0).adapt(FrameworkStartLevel.class);
				fsl.setStartLevel(maxStartLevel + 1);
				fsl.setInitialBundleStartLevel(maxStartLevel + 1);
				debug("Start level set to " + (maxStartLevel + 1));
			}

			private void startBundles(final List<MavenArtifact> artifacts) throws ParserConfigurationException, IOException, SAXException {
				if (artifacts == null || artifacts.isEmpty()) { // FIXME install static files in any case
					return;
				}
				final Resolver resolver = new Resolver(artifacts, ctx);
				final int startLevel = 4; // TODO configurable
				final Map<Integer, List<ResolvedArtifact>> bundles = new HashMap<>();
				final List<Bundle> allBundles = new ArrayList<>();
				bundles.put(startLevel, new ArrayList<ResolvedArtifact>(artifacts.size()+2));
				String initDir0 = ctx.getProperty(Properties.INIT_DIR_PROPERTY);
				if (initDir0 == null)
					initDir0 = Properties.INIT_DIR_DEFAULT;
				final Path initDir = Paths.get(initDir0);
				if (Files.isDirectory(initDir)) {
					Files.walkFileTree(initDir, new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							try (final BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
								final String location = "file-init:" + initDir.relativize(file).toString();
								final Bundle b = ctx.installBundle(location, stream);
								if (b == null)
									throw new NullPointerException("Newly installed bundle is null: " + file);
								final ResolvedArtifact dummyArtifact = new ResolvedArtifact(new MavenArtifact(startLevel, true), b);
									bundles.get(startLevel).add(dummyArtifact);
									allBundles.add(b);
							} catch (BundleException e) {
								warn("Failed to install bundle " + file);
							}
							return FileVisitResult.CONTINUE;
						}

					});
				}
				final Collection<ResolvedArtifact> bundles0 = resolver.resolve();
				for (ResolvedArtifact a : bundles0) {
					final int startLevel0 = a.getArtifact().getStartLevel();
					if (!bundles.containsKey(startLevel0))
						bundles.put(startLevel0, new ArrayList<ResolvedArtifact>());
					bundles.get(startLevel0).add(a);
					allBundles.add(a.getBundle());
				}
				info(allBundles.size() +" bundles installed");
				final Bundle system = ctx.getBundle(0);
				final FrameworkWiring fw = system.adapt(FrameworkWiring.class);
				final Collection<Bundle> closures = fw.getDependencyClosure(allBundles);
				if (closures == null || closures.isEmpty())
					return;
				fw.resolveBundles(closures);
				int maxStartLevel = startLevel;
				for (Map.Entry<Integer, List<ResolvedArtifact>> entry : bundles.entrySet()) {
					final int startLevel1 = entry.getKey();
					for (ResolvedArtifact a: entry.getValue()) {
						final BundleStartLevel sl = a.getBundle().adapt(BundleStartLevel.class);
						sl.setStartLevel(startLevel1);
					}
					if (startLevel1 > maxStartLevel)
						maxStartLevel = startLevel1;
				}
				final FrameworkStartLevel fsl = system.adapt(FrameworkStartLevel.class);
				final int currentStartLevel = Math.max(maxStartLevel, fsl.getInitialBundleStartLevel()) + 1;
				fsl.setStartLevel(currentStartLevel);
				fsl.setInitialBundleStartLevel(currentStartLevel);
				debug("Start level set to {}", currentStartLevel);
				for (List<ResolvedArtifact> list : bundles.values()) {
					for (ResolvedArtifact a : list) {
						if (!a.getArtifact().isDoStart())
							continue;
						final Bundle b = a.getBundle();
						if (isFragment(b) || b.getState() == Bundle.INSTALLED)
							continue;
						try {
							b.start();
						} catch (Exception e) {
							info("Failed to start bundle " + b.getSymbolicName());
						}
					}
				}
			}

		}, "maven-resolver-thread").start();;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	private static boolean isFragment(final Bundle b) {
		final BundleRevision rev = b.adapt(BundleRevision.class);
		return ((rev.getTypes() & BundleRevision.TYPE_FRAGMENT) > 0);
	}

	static void debug(String msg, Object... arguments) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).debug(msg);
		} catch (NoClassDefFoundError e) {
			for (Object arg: arguments) {
				msg = msg.replaceFirst("\\{\\}", arg.toString().replace("\\", "\\\\"));
			}
			System.out.println(msg);
		}
	}

	static void info(String msg) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).debug(msg);
		} catch (NoClassDefFoundError e) {
			System.out.println(msg);
		}
	}

	static void warn(String msg) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).warn(msg);
		} catch (NoClassDefFoundError e) {
			System.err.println(msg);
		}
	}

	public static void warn(String msg, Throwable e) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).warn(msg, e);
		} catch (NoClassDefFoundError ee) {
			System.err.println(msg);
			e.printStackTrace();
		}
	}

	static void error(String msg) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).error(msg);
		} catch (NoClassDefFoundError e) {
			System.err.println(msg);
		}
	}

	static void error(String msg, Throwable e) {
		try {
			LoggerFactory.getLogger(MavenResolver.class).error(msg, e);
		} catch (NoClassDefFoundError ee) {
			System.err.println(msg);
			e.printStackTrace();
		}
	}

	static boolean isSubdir(final Path parent, final Path child) {
		return child.toAbsolutePath().normalize().toString().startsWith(parent.toAbsolutePath().normalize().toString());
	}
	
}
