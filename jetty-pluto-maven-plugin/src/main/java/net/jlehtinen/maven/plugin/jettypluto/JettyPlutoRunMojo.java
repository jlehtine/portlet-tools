package net.jlehtinen.maven.plugin.jettypluto;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.plugin.Jetty6PluginWebAppContext;
import org.mortbay.jetty.plugin.Jetty6RunMojo;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;

/**
 * Run the portlet being developed in a Pluto container under Jetty without the time consuming
 * deploy process.
 * 
 * @extendsPlugin jetty
 * @goal run
 * @requiresProject
 */
public class JettyPlutoRunMojo extends Jetty6RunMojo {

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

	public void finishConfigurationBeforeStart() throws Exception {
		
		// Create a context handler for Pluto portal
		plutoHandler = createPlutoContextHandler();
		
		super.finishConfigurationBeforeStart();
	}

	/**
	 * Creates a context handler for the Pluto portal.
	 * 
	 * @return context handler for Pluto
	 * @throws Exception on error
	 */
	protected ContextHandler createPlutoContextHandler() throws Exception {

		// Resolve Pluto container WAR
		if (plutoWar == null) {
			plutoWar = getResolvedArtifactPath(
					createArtifact(plutoGroupId, plutoArtifactId, plutoVersion, "war")
			);
		}
		
		// Create context handler
		WebAppContext plutoHandler = new WebAppContext();
		plutoHandler.setContextPath(plutoContextPath);
		plutoHandler.setWar(plutoWar);
		plutoHandler.setExtractWAR(false);
		HashUserRealm userRealm = new HashUserRealm(plutoRealmName, plutoRealmConfig);
		plutoHandler.getSecurityHandler().setUserRealm(userRealm);
		getLog().info(MessageFormat.format("Pluto context path = {0}", new Object[] { plutoContextPath }));
		getLog().info(MessageFormat.format("Pluto WAR = {0}", new Object[] { plutoWar }));
		return plutoHandler;
	}

	/**
     * Returns a new runtime artifact identification record.
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
     * Resolves the specified artifact and returns the path to the artifact in the local repository.
     * 
     * @param artifact artifact identification record
     * @return path to the artifact file in the local repository
     * @throws ArtifactNotFoundException if no such artifact is found
     * @throws ArtifactResolutionException if artifact can not be resolved
     */
    protected String getResolvedArtifactPath(Artifact artifact) throws MojoExecutionException {
    	try {
    		artifactResolver.resolve(artifact, remoteRepositories, localRepository);
    	} catch (Exception e) {
    		String errorMsg = MessageFormat.format("Could not resolve artifact {0}", new Object[] { artifact });
    		getLog().error(errorMsg);
    		throw new MojoExecutionException(errorMsg, e);
    	}
    	return new File(localRepository.getBasedir(), localRepository.pathOf(artifact)).getPath();
    }
}
