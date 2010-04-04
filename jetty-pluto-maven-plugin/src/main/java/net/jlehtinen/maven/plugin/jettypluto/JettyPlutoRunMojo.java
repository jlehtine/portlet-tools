package net.jlehtinen.maven.plugin.jettypluto;

/*
 * Copyright (c) 2010 Johannes Lehtinen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.plugin.Jetty6RunMojo;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Run the portlet being developed in the Apache Pluto container under Jetty without the time
 * consuming deploy process.
 * 
 * @extendsPlugin jetty
 * @goal run
 * @description Runs the Apache Pluto portal under Jetty on a Maven portlet project
 */
public class JettyPlutoRunMojo extends Jetty6RunMojo {

	/** Pluto group identifier */
	protected static final String PLUTO_GROUP_ID = "org.apache.portals.pluto";
	
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
     * <p>Portal implementation to use. By default the Pluto portal implementation is
     * used but this parameter can be used to override it. While this plugin is Pluto dependent,
     * this parameter may be used, for example, to specify a customized Pluto based portal.</p>
     * 
     * <p>You can either specify a Maven artifact using <code>groupId</code>,
     * <code>artifactId</code> and <code>version</code>, or a direct path to the WAR
     * using <code>file</code>.</p>
     * 
     * <p>The following example demonstrates how to use Maven artifact resolution:</p>
     * 
     * <pre>
     * &lt;portal>
     *   &lt;groupId>org.apache.portals.pluto&lt;/groupId>
     *   &lt;artifactId>pluto-portal&lt;/artifactId>
     *   &lt;version>2.0.0&lt;/version>
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
	 * default portal implementation and the default portal libraries if they have not
	 * been explicitly specified using <code>portal</code> and <code>portalLibraries</code>.
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
	 * Context handler for the Pluto portal.
	 */
	protected ContextHandler plutoHandler;
	
	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Configure this mojo
		configureJettyPlutoRunMojo();

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
	}
	
	/**
	 * Creates the artifact identity for the default portal implementation.
	 * 
	 * @return artifact identity for the default portal implementation
	 */
	protected ArtifactIdentity createDefaultPortal() {
		return new ArtifactIdentity(PLUTO_GROUP_ID, "pluto-portal", plutoVersion, "war");
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
