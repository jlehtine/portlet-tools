package net.jlehtinen.maven.plugin.jettypluto;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Container for library artifact identity information. Mostly exists to be able use proper
 * element name in configuration.
 */
public class Library extends ArtifactIdentity {
	
	/**
	 * Constructs a new uninitialized instance.
	 */
	public Library() {
		super();
	}
	
	/**
	 * Constructs a new instance and initializes it with the specified data.
	 * 
	 * @param groupId group identifier
	 * @param artifactId artifact identifier
	 * @param version version number
	 */
	public Library(String groupId, String artifactId, String version) {
		super(groupId, artifactId, version, "jar");
	}

	/**
	 * @see ArtifactIdentity#validate()
	 */
	public void validate() throws MojoExecutionException {
		super.validate();
		if (file == null) {
			if (!"jar".equals(getType())) {
				throw new MojoExecutionException("type must be jar");
			}
		}
	}

}
