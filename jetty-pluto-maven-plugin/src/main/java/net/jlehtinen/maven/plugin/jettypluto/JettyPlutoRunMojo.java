package net.jlehtinen.maven.plugin.jettypluto;

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
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Run the portlet being developed in a Pluto container under Jetty without the time consuming
 * deploy process.
 * 
 * @extendsPlugin jetty
 * @goal run
 * @requiresProject
 */
public class JettyPlutoRunMojo extends Jetty6RunMojo {

	protected static final String PLUTO_GROUP_ID = "org.apache.portals.pluto";
	
	protected static final String JETTY_CLASS_PATH_PROPERTY = "jetty.class.path";
	
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
	 * Group identifier of the Pluto implementation to be used. Used to resolve the Pluto WAR
	 * when no explicit WAR path is defined.
	 * 
	 * @parameter expression="${pluto.groupId}" default-value="org.apache.portals.pluto"
	 */
	protected String plutoGroupId;

	/**
	 * Artifact identifier of the Pluto portal implementation to be used. Used to resolve the
	 * Pluto WAR when no explicit WAR path is defined.
	 * 
	 * @parameter expression="${pluto.artifactId}" default-value="pluto-portal"
	 */
	protected String plutoArtifactId;
	
	/**
	 * Version of the Pluto portal implementation to be used. Used to resolve the Pluto WAR
	 * when no explicit WAR path is defined.
	 * 
	 * @parameter expression="${pluto.version}" default-value="2.0.0"
	 */
	protected String plutoVersion;

	/**
	 * Absolute path to the Pluto WAR, overriding parameters <code>plutoGroupId</code>,
	 * <code>plutoArtifactId</code> and <code>plutoVersion</code>.
	 * 
	 * @parameter expression="${pluto.war}"
	 */
	protected String plutoWar;
	
	/**
	 * Portal libraries to be installed into the shared class path.
	 * By default the libraries needed by the Pluto portal implementation
	 * are included but this parameter can be used to override the default
	 * set of libraries.
	 * 
	 * @parameter
	 */
	protected List portalLibraries;
	
	/**
	 * Context path for the Pluto portal implementation.
	 * 
	 * @parameter expression="${pluto.contextPath}" default-value="/pluto"
	 */
	protected String plutoContextPath;

	/**
	 * Name of the Pluto user realm.
	 * 
	 * @parameter expression="${pluto.realm.name}" default-value="Pluto Realm"
	 */
	protected String plutoRealmName;
	
	/**
	 * Pluto user realm configuration file in the Jetty {@link HashUserRealm} format.
	 * 
	 * @parameter expression="${pluto.realm.config}"
	 * @required
	 */
	protected String plutoRealmConfig;
	
	/**
	 * Context handler for the Pluto portal.
	 */
	protected ContextHandler plutoHandler;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Dumping JettyPlutoRunMojo classloader");
		logClassLoader(getClass().getClassLoader());

		// Configure this mojo
		configureJettyPlutoRunMojo();

		// Configure required portal libraries into the Jetty class path
		configureJettyClassPath();
		
		// Create a context handler for Pluto portal
		plutoHandler = createPlutoContextHandler();
		
		super.execute();
	}

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

	protected void configureJettyPlutoRunMojo() throws MojoExecutionException {
	
		// Resolve portal implementation WAR if necessary
		if (plutoWar == null) {
			plutoWar = resolveArtifact(
					createArtifact(plutoGroupId, plutoArtifactId, plutoVersion, "war")
			);
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
					throw new MojoExecutionException(MessageFormat.format("Invalid <library> entry in configuration: {0}",	new Object[] { e.getMessage() }));
				}
			}
		}

		// Initialize the default set of portal libraries, if necessary
		if (portalLibraries == null) {
			portalLibraries = new ArrayList();
			addDefaultPortalLibraries();
		}

		// Resolve the libraries
		Iterator iter = portalLibraries.iterator();
		while (iter.hasNext()) {
			Library l = (Library) iter.next();
			if (l.getJar() == null) {
				l.setJar(resolveArtifact(createArtifact(l)));
			}
		}
	}
	
	protected void addDefaultPortalLibraries() {
		portalLibraries.add(new Library("org.apache.portals", "portlet-api_2.0_spec", "1.0"));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-driver-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-taglib", plutoVersion));
		portalLibraries.add(new Library("javax.ccpp", "ccpp", "1.0"));
	}
	
	/**
	 * Adds the portal libraries to the shared Jetty class path.
	 */
	protected void configureJettyClassPath() throws MojoExecutionException {
		
		// Preserve existing class path entries, if any
		String jettyClassPath = System.getProperty(JETTY_CLASS_PATH_PROPERTY);
		StringBuffer cpb = new StringBuffer(jettyClassPath != null ? jettyClassPath : "");
		
		// Resolve each library and add it to the class path
		Iterator iter = portalLibraries.iterator();
		while (iter.hasNext()) {

			/*
			// Append library to the class path
			if (cpb.length() > 0) {
				cpb.append(System.getProperty("path.separator"));
			}
			cpb.append(((Library) iter.next()).getJar());
			*/

			// Add library to the mojo class loader
			String jar = ((Library) iter.next()).getJar();
			ClassLoader cl = getClass().getClassLoader();
			if (cl != null && cl instanceof URLClassLoader) {
				ReflectionWrapper wcl = new ReflectionWrapper(cl);
				try {
					wcl.invokeMethod("addURL", new Class[] { URL.class }, new Object[] { new File(jar).toURI().toURL() });
				} catch (Throwable t) {
					throw new MojoExecutionException(MessageFormat.format("Failed to add jar {0} to the path of the class loader using reflection", new Object[] { jar }));
				}
			} else {
				throw new MojoExecutionException("Can not modify class loader, even via reflection");
			}
			
		}
		String cp = cpb.toString();
		getLog().info(MessageFormat.format("Jetty class path = {0}", new Object[] { cp }));
		
		
		
		// Set the Jetty class path system property
		/*
		System.setProperty(JETTY_CLASS_PATH_PROPERTY, cp);
		*/
	}

	/**
	 * Creates a context handler for the Pluto portal.
	 * 
	 * @return context handler for Pluto
	 * @throws Exception on error
	 */
	protected ContextHandler createPlutoContextHandler() {

		// Log some basic configuration
		getLog().info(MessageFormat.format("Pluto context path = {0}", new Object[] { plutoContextPath }));
		getLog().info(MessageFormat.format("Pluto WAR = {0}", new Object[] { plutoWar }));

		// Create context handler
		WebAppContext plutoHandler = new WebAppContext();
		plutoHandler.setContextPath(plutoContextPath);
		plutoHandler.setWar(plutoWar);
		plutoHandler.setExtractWAR(false);
		//HashUserRealm userRealm = new HashUserRealm(plutoRealmName, plutoRealmConfig);
		//plutoHandler.getSecurityHandler().setUserRealm(userRealm);
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
    			aid.getType()
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
    
    protected void logClassLoader(ClassLoader cl) {
    	getLog().info("START OF CLASS LOADERS");
    	while (cl != null) {
    		getLog().info("[" + cl.getClass().getName() + "] " + cl.toString());
    		cl = cl.getParent();
    	}
    	getLog().info("END OF CLASS LOADERS");
    }
}
