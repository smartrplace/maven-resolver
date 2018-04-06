# Maven resolver

## Content
* [Introduction](#introduction)
* [Getting started](#getting-started)
* [Configuration](#configuration)
* [Build](#build)
* [Dependencies](#dependencies)
* [License](#license)

## Introduction
The Maven resolver project is an OSGi bundle that resolves other bundles via Maven, installs them in your OSGi platform, and starts them. Furthermore, it provides a URL stream handler for Maven artifacts (the *mvn*-protocol).

## Getting started
Once started, the bundle looks for jar-files in the *init*-folder (relative to the current working directory) and for a configuration file *config/config.xml*. It will then try to resolve the Maven artifacts listed in the config.xml file, install them along with the jar-files from the init-folder, resolve all bundles and start them (except for fragment bundles). The resolving strategy is to first check the local Maven repository for an artifact, and if not successful, check a set of remote repositories. If the artifact is available in a remote repository, it will be downloaded to the local one. 

TODO link to sample run configuration.

The format of the config.xml file is illustrated by the following example, which starts some basic OSGi components:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration 
[
	<!ENTITY slf4j-version "1.7.25">
]>
<configuration>
	<bundles>
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.framework.security" version="2.6.0" startLevel="1" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.scr" version="2.0.12" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.configadmin" version="1.8.14" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.metatype" version="1.1.2" startLevel="2" />
 		<bundle groupId="org.osgi" artifactId="org.osgi.service.useradmin" version="1.1.0" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.useradmin" version="1.0.3" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.useradmin.filestore" version="1.0.2" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.fileinstall" version="3.6.0" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.http.servlet-api" version="1.1.2" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.eventadmin" version="1.4.8" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.gogo.runtime" version="1.0.10" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.gogo.command" version="1.0.2" startLevel="2" />
	 	<bundle groupId="org.jline" artifactId="jline" version="3.6.1" startLevel="2" />
		<bundle groupId="org.apache.felix" artifactId="org.apache.felix.gogo.jline" version="1.0.10" startLevel="3" /> 

		<bundle groupId="org.slf4j" artifactId="slf4j-api" version="&slf4j-version;" startLevel="1"/>
		<bundle groupId="org.slf4j" artifactId="slf4j-simple" version="&slf4j-version;" startLevel="1"/>
		<bundle groupId="org.slf4j" artifactId="osgi-over-slf4j" version="&slf4j-version;" startLevel="1"/>
		<bundle groupId="org.slf4j" artifactId="log4j-over-slf4j" version="&slf4j-version;" startLevel="1"/>
		<bundle groupId="org.slf4j" artifactId="jul-to-slf4j" version="&slf4j-version;" startLevel="1"/>
		<bundle groupId="org.slf4j" artifactId="jcl-over-slf4j" version="&slf4j-version;" startLevel="1"/>

	</bundles>
</configuration>
```

## Configuration
By default, the resolver checks the local .m2-repository (directory *~/.m2*) and Maven central for Maven artifacts. The remote repositories can be configured via a file *config/repos.properties*. Put each repository URL in a separate line, for instance:
```
http://central.maven.org/maven2
https://ogema-source.net/artifactory/libs-release
```
Supported configuration properties (may be set as system properties or OSGi framework properties):

| Property | Default value | Description |
|----------|---------------|-------------|
| org.smartrplace.maven.resolver.init_dir | init | Folder to search for static bundle jars |
| org.smartrplace.maven.resolver.repos_file | config/repos.properties | Repositories configuration file |
| org.smartrplace.maven.resolver.config_path | config/config.xml | Path to configuration file for bundles resolved via Maven |

## Build
Go to project base folder and execute `mvn clean install`. Prerequisite: Java 7 or higher and Maven 3 or higher.

## Dependencies
The only runtime depenency is an OSGi framework (spec version 5), other dependencies are bundled into the jar. The Maven-UrlStreamHandler requires a handler for OSGi declarative services.

## License
Apache License v2.0 ([http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0))
