/*
 * Copyright 2010 Johannes Lehtinen 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jlehtinen.jettypluto.maven;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.jlehtinen.jettypluto.maven.util.ReflectionWrapper;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.pluto.util.assemble.AssemblerConfig;
import org.apache.pluto.util.assemble.AssemblerFactory;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.plugin.Jetty6RunMojo;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Runs the portlet or portlets being developed directly from the Maven source project
 * using the Jetty servlet container and the Apache Pluto portlet container.
 * 
 * @extendsPlugin jetty
 * @goal run
 * @description Runs the Apache Pluto portal under Jetty on a Maven portlet project
 */
public class JettyPlutoRunMojo extends Jetty6RunMojo {

	/** Jetty-Pluto group identifier */
	protected static final String JETTY_PLUTO_GROUP_ID = "net.jlehtinen";
	
	/** Jetty-Pluto portal identifier */
	protected static final String JETTY_PLUTO_PORTAL_ID = "jetty-pluto-portal";
	
	/** Jetty-Pluto portal version */
	protected static final String JETTY_PLUTO_PORTAL_VERSION = "0.1-SNAPSHOT";
	
	/** Pluto group identifier */
	protected static final String PLUTO_GROUP_ID = "org.apache.portals.pluto";
	
	/** System property for the portlet names */
	protected static final String PORTLET_NAMES_PROPERTY = "portletNames";
	
	/** System property for the portlet context */
	protected static final String PORTLET_CONTEXT_PATH_PROPERTY = "portletContextPath";
	
	/**
	 * The portlet.xml file to be used. The default location is in ${basedir}/src/main/webapp/WEB-INF.
	 * 
	 * @parameter expression="${maven.war.portletxml}"
	 */
	protected File portletXml;
	
    /**
     * The destination file into which an assembled version of the web.xml is written.
     * 
     * @parameter expression="${project.build.directory}/pluto-resources/web.xml"
     * @readonly
     * @required
     */
    private File webXmlDestination;

	/**
	 * <p>Specifies the names of the portlets to be prototyped under Pluto as a comma separated list.
	 * The names must match exactly the name provided in the <code>portlet-name</code> element
	 * of the portlet descriptor.</p>
	 * 
	 * <p>Example: <code>&lt;portletNames>MyPortlet,Another Portlet&lt;/portletNames></code></p>
	 * 
	 * @parameter expression="${portletNames}"
	 * @required
	 */
	protected String portletNames;
	
    /**
     * <p>Portal implementation to use. By default a slightly modified Pluto portal implementation is
     * used but this parameter can be used to override it. For example, it is possible to create a
     * customized portal style by creating a WAR overlay on the default portal and then specify
     * the custom portal version using this parameter.</p>
     * 
     * <p>You can either specify a Maven artifact using <code>groupId</code>,
     * <code>artifactId</code> and <code>version</code>, or a direct path to the WAR
     * using <code>file</code>.</p>
     * 
     * <p>The following example demonstrates how to use Maven artifact resolution:</p>
     * 
     * <pre>
     * &lt;portal>
     *   &lt;groupId>net.jlehtinen&lt;/groupId>
     *   &lt;artifactId>jetty-pluto-portal&lt;/artifactId>
     *   &lt;version>0.1&lt;/version>
     *   &lt;packaging>war&lt;/packaging>
     * &lt;/portal>
     * </pre>
     * 
     * @parameter
     */
    protected ArtifactIdentity portal;
    
	/**
	 * <p>Portal libraries to be installed into the shared class path.
	 * By default the libraries needed by the Pluto portal implementation
	 * are included but this parameter can be used to override the default
	 * set of libraries.</p>
	 * 
	 * <p>Each library can be specified either as a Maven artifact using <code>groupId</code>,
	 * <code>artifactId</code> and <code>version</code>, or as a direct path to the JAR
	 * using <code>file</code>.</p>
	 * 
	 * <p>The following example demonstrates both kinds of library referrals:</p>
	 * 
	 * <pre>
	 * &lt;portalLibraries>
	 *   &lt;library>
	 *     &lt;groupId>org.apache.portals.pluto&lt;/groupId>
	 *     &lt;artifactId>pluto-container-api&lt;/artifactId>
	 *     &lt;version>2.0.0&lt;/version>
	 *     &lt;packaging>jar&lt;/packaging>
	 *   &lt;/library>
	 *   &lt;library>
	 *     &lt;file>/opt/j2ee/libs/portlet-api-2.0.jar&lt;/file>
	 *   &lt;/library>
	 * &lt;/portalLibraries>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List portalLibraries;
	
	/**
	 * Version of the Pluto portal implementation to be used. This only affects the
	 * default portal libraries if they have not been explicitly specified using
	 * <code>portalLibraries</code>. To use a different version of the Pluto portal,
	 * you have to specify a portal implementation using the <code>portal</code> parameter.
	 * 
	 * @parameter expression="${pluto.version}" default-value="2.0.0"
	 */
	protected String plutoVersion;
	
	/**
	 * Context path for the portal implementation.
	 * 
	 * @parameter expression="${pluto.contextPath}" default-value="/pluto"
	 */
	protected String plutoContextPath;

	/**
	 * <p>List of users to be added in the user realm. By default a single user <code>pluto</cdoe>
	 * with password <code>pluto</code> and role <code>pluto</code> is included in the
	 * user realm but this parameter can be used to override user realm data.</p>
	 * 
	 * <p>The following example demonstrates the use of this parameter:</p>
	 * 
	 * <pre>
	 * &lt;users>
	 *   &lt;user>
	 *     &lt;name>myuser&lt;/name>
	 *     &lt;password>mypass&lt;/passsword>
	 *     &lt;roles>myrole, admin&lt;/roles>
	 *   &lt;/user>
	 * &lt/users>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List users;
	
	/**
	 * Name of the user realm passed on to Pluto.
	 * 
	 * @parameter expression="${pluto.realm.name}" default-value="Pluto Realm"
	 */
	protected String plutoRealmName;
	
	/**
	 * Artifact resolver
	 * 
	 * @component
	 */
	protected ArtifactResolver artifactResolver;

	/**
	 * Artifact factory
	 * 
	 * @component
	 */
	protected ArtifactFactory artifactFactory;
	
    /**
     * Local repository
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories being used
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List remoteRepositories;

	/**
	 * Context handler for the Pluto portal.
	 */
	protected ContextHandler plutoHandler;
	
	/** The original web.xml file of the web application */
	protected File originalWebXml;
	
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Configure this mojo
		configureJettyPlutoRunMojo();

		// Assemble portlets for Pluto
		assemblePortlets();
		
		// Configure required portal libraries into the Jetty class path
		configureClassPath();
		
		// Create a context handler for Pluto portal
		plutoHandler = createPlutoContextHandler();
		
		super.execute();
	}

	/**
	 * Overrides the Jetty plugin method to add the portal context handler
	 * into the list of handlers.
	 * 
	 * @return configured context handlers and the portal context handler
	 * @see org.mortbay.jetty.plugin.Jetty6RunMojo#getConfiguredContextHandlers()
	 */
	public ContextHandler[] getConfiguredContextHandlers() {
		ContextHandler[] configuredHandlers = super.getConfiguredContextHandlers();
		ContextHandler[] handlers;
		if (configuredHandlers != null) {
			handlers = (ContextHandler[]) Arrays.copyOf(configuredHandlers, configuredHandlers.length + 1);
		} else {
			handlers = new ContextHandler[1];
		}
		handlers[handlers.length - 1] = plutoHandler;
		return handlers;
	}

	/**
	 * Completes the configuration of this mojo.
	 * 
	 * @throws MojoExecutionException on error
	 */
	protected void configureJettyPlutoRunMojo() throws MojoExecutionException {
	
		// Check which original web.xml should be used
		originalWebXml = getWebXml();
		if (originalWebXml == null) {
			originalWebXml = getDefaultWebXml();
		}
		getLog().info(MessageFormat.format("Original web.xml = {0}", new Object[] { originalWebXml }));
		
		// Make Jetty use the assembled web.xml
		configureJettyWebXml(webXmlDestination);
		getLog().info(MessageFormat.format("Assembled web.xml = {0}", new Object[] { webXmlDestination }));
		
		// Initialize default portlet.xml location, if necessary
		if (portletXml == null) {
			portletXml = getDefaultPortletXml();
		}
		getLog().info(MessageFormat.format("Original portlet.xml = {0}", new Object[] { portletXml }));

		// Check that web.xml exists
		if (!originalWebXml.exists()) {
			throw new MojoExecutionException(MessageFormat.format("Web application descriptor {0} does not exist", new Object[] { originalWebXml }));
		}

		// Check that portlet.xml exists
		if (!portletXml.exists()) {
			throw new MojoExecutionException(MessageFormat.format("Portlet descriptor {0} does not exist", new Object[] { portletXml }));
		}
		
		// Validate the user-specified portal implementation
		if (portal != null) {
			try {
				portal.validate();
			} catch (MojoExecutionException e) {
				throw new MojoExecutionException(MessageFormat.format("Invalid <portal> entry in configuration: {0}", new Object[] { e.getMessage() }));				
			}
		}
		
		// Validate the user-specified libraries
		if (portalLibraries != null) {
			Iterator iter = portalLibraries.iterator();
			while (iter.hasNext()) {

				// Check that it is Library
				Object o = iter.next();
				if (o == null || !(o instanceof Library)) {
					throw new MojoExecutionException("Configuration entries in <portalLibraries> must be of type <library>");
				}
				Library l = (Library) o;

				// Validate contents
				try {
					l.validate();
				} catch (MojoExecutionException e) {
					throw new MojoExecutionException(MessageFormat.format("Invalid <library> entry in configuration: {0}", new Object[] { e.getMessage() }));
				}
			}
		}
		
		// Validate the user-specified realm data
		if (users != null) {
			Iterator iter = users.iterator();
			while (iter.hasNext()) {
				
				// Check that it is User
				Object o = iter.next();
				if (o == null || !(o instanceof User)) {
					throw new MojoExecutionException("Configuration entries in <users> must be of type <user>");
				}
				User u = (User) o;
				
				// Validate contents
				try {
					u.validate();
				} catch (MojoExecutionException e) {
					throw new MojoExecutionException(MessageFormat.format("Invalid <user> entry in configuration: {0}",	new Object[] { e.getMessage() }));
				}				
			}
		}
		
		// Initialize the default portal implementation, if necessary
		if (portal == null) {
			portal = createDefaultPortal();
		}
		
		// Initialize the default set of portal libraries, if necessary
		if (portalLibraries == null) {
			portalLibraries = new ArrayList();
			addDefaultPortalLibraries(portalLibraries);
		}

		// Initialize the default set of users, if necessary
		if (users == null) {
			users = new ArrayList();
			users.add(new User("pluto", "pluto", "pluto"));
		}
		
		// Resolve portal implementation WAR if necessary
		if (portal.getFile() == null) {
			portal.setFile(resolveArtifact(createArtifact(portal)));
		}
		
		// Resolve the libraries
		Iterator iter = portalLibraries.iterator();
		while (iter.hasNext()) {
			Library l = (Library) iter.next();
			if (l.getFile() == null) {
				l.setFile(resolveArtifact(createArtifact(l)));
			}
		}
		
		// Pass the context path onwards in a system parameter
		System.setProperty(PORTLET_CONTEXT_PATH_PROPERTY, getContextPath());
		
		// Pass the portlet identifiers onwards in a system parameter
		if (portletNames != null && System.getProperty(PORTLET_NAMES_PROPERTY) == null) {
			System.setProperty(PORTLET_NAMES_PROPERTY, portletNames);
		}
	}
	
	/**
	 * Returns the default web.xml file.
	 * 
	 * @return default web.xml file
	 */
	protected File getDefaultWebXml() {
		return new File(new File(getWebAppSourceDirectory(), "WEB-INF"), "web.xml");
	}
	
	/**
	 * Returns the default portlet.xml file.
	 * 
	 * @return default portlet.xml file
	 */
	protected File getDefaultPortletXml() {
		return new File(new File(getWebAppSourceDirectory(), "WEB-INF"), "portlet.xml");
	}
	
    /**
     * Configures the Jetty to use the assembled web.xml file.
     */
    protected void configureJettyWebXml(File jettyWebXml) {
    	
    	// Use reflection to set webXml parameter
    	ReflectionWrapper thisRef = new ReflectionWrapper(this);
    	thisRef.setFieldValue("webXml", jettyWebXml);
    }
	
	/**
	 * Creates the artifact identity for the default portal implementation.
	 * 
	 * @return artifact identity for the default portal implementation
	 */
	protected ArtifactIdentity createDefaultPortal() {
		return new ArtifactIdentity(JETTY_PLUTO_GROUP_ID, JETTY_PLUTO_PORTAL_ID, JETTY_PLUTO_PORTAL_VERSION, "war");
	}
	
	/**
	 * Adds the default set of portal libraries to the specified list.
	 * 
	 * @param portalLibraries list of portal libraries
	 */
	protected void addDefaultPortalLibraries(List portalLibraries) {
		portalLibraries.add(new Library("org.apache.portals", "portlet-api_2.0_spec", "1.0"));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-driver-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-taglib", plutoVersion));
		portalLibraries.add(new Library("javax.ccpp", "ccpp", "1.0"));
	}
	
	/**
	 * Assembles portlets so that they can be deployed to Pluto.
	 */
	protected void assemblePortlets() throws MojoExecutionException {
		
		// Create assembler configuration
		AssemblerConfig assemblerConfig = new AssemblerConfig();
		assemblerConfig.setWebappDescriptor(originalWebXml);
		assemblerConfig.setPortletDescriptor(portletXml);
		assemblerConfig.setDestination(webXmlDestination);
		
		// Assembler portlets
		try {
			AssemblerFactory.getFactory().createAssembler(assemblerConfig).assemble(assemblerConfig);
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to assemble web application for Pluto", e);
		}
		getLog().info("Assembled web application for Pluto");
	}
	
	/**
	 * Adds the portal libraries to the shared Jetty class path.
	 */
	protected void configureClassPath() throws MojoExecutionException {
		
		// Resolve each library and add it to the class path
		Iterator iter = portalLibraries.iterator();
		while (iter.hasNext()) {

			// Add library to the mojo class loader
			String jar = ((Library) iter.next()).getFile();
			ClassLoader cl = getClass().getClassLoader();
			if (cl != null && cl instanceof URLClassLoader) {
				ReflectionWrapper wcl = new ReflectionWrapper(cl);
				try {
					wcl.invokeMethod("addURL", new Class[] { URL.class }, new Object[] { new File(jar).toURI().toURL() });
					getLog().info(MessageFormat.format("Added to shared class path: {0}", new Object[] { jar }));
				} catch (Throwable t) {
					throw new MojoExecutionException(MessageFormat.format("Failed to add jar {0} to the path of the class loader using reflection", new Object[] { jar }));
				}
			} else {
				throw new MojoExecutionException("Can not modify class loader, even via reflection");
			}
			
		}
	}

	/**
	 * Creates a context handler for the Pluto portal.
	 * 
	 * @return context handler for Pluto
	 * @throws Exception on error
	 */
	protected ContextHandler createPlutoContextHandler() {

		// Log some basic configuration
		getLog().info(MessageFormat.format("Portal context path = {0}", new Object[] { plutoContextPath }));
		getLog().info(MessageFormat.format("Portal WAR = {0}", new Object[] { portal.getFile() }));

		// Create context handler
		WebAppContext plutoHandler = new WebAppContext();
		plutoHandler.setContextPath(plutoContextPath);
		plutoHandler.setWar(portal.getFile());
		plutoHandler.setExtractWAR(false);
		Realm realm = new Realm(plutoRealmName, users);
		plutoHandler.getSecurityHandler().setUserRealm(realm);
		return plutoHandler;
	}

	/**
     * Creates a new runtime artifact identification record from the specified identifiers.
     * 
     * @param groupId group identifier
     * @param artifactId artifact identifier
     * @param version artifact version
     * @param type artifact type
     * @return artifact identification record
     */
    protected Artifact createArtifact(String groupId, String artifactId, String version, String type) {
    	return artifactFactory.createArtifact(groupId, artifactId, version, "runtime", type);
    }
    
    /**
     * Creates a new runtime artifact record from the specified artifact identity.
     * 
     * @param aid artifact identity
     * @return artifact record
     */
    protected Artifact createArtifact(ArtifactIdentity aid) {
    	return artifactFactory.createArtifact(
    			aid.getGroupId(),
    			aid.getArtifactId(),
    			aid.getVersion(),
    			"runtime",
    			aid.getPackaging()
    	);
    }
    
    /**
     * Resolves the specified artifact and returns the path to the artifact in the local repository.
     * 
     * @param artifact artifact identification record
     * @return path to the artifact file in the local repository
     * @throws ArtifactNotFoundException if no such artifact is found
     * @throws ArtifactResolutionException if artifact can not be resolved
     */
    protected String resolveArtifact(Artifact artifact) throws MojoExecutionException {
    	try {
    		artifactResolver.resolve(artifact, remoteRepositories, localRepository);
    	} catch (Exception e) {
    		throw new MojoExecutionException(MessageFormat.format("Could not resolve artifact {0}", new Object[] { artifact }), e);
    	}
    	return new File(localRepository.getBasedir(), localRepository.pathOf(artifact)).getPath();
    }
}
