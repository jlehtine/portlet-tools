package net.jlehtinen.maven.plugin.jettypluto;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Container for artifact identity information. Used for mojo configuration. Either Maven
 * artifact identifiers can be provided or a direct path to a local file.
 */
public class ArtifactIdentity {

	/** Group identifier */
	protected String groupId;
	
	/** Artifact identifier */
	protected String artifactId;

	/** Version number */
	protected String version;
	
	/** Type of the artifact */
	protected String type;
	
	/** Direct path to a local file containing the artifact */
	protected String file;

	/**
	 * Constructs a new uninitialized instance.
	 */
	public ArtifactIdentity() {
	}
	
	/**
	 * Constructs a new instance initialized from the specified identifiers.
	 * 
	 * @param groupId group identifier
	 * @param artifactId artifact identifier
	 * @param version version number
	 * @param type type of the artifact
	 */
	public ArtifactIdentity(String groupId, String artifactId, String version, String type) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.type = type;
	}
	
	/**
	 * Returns the group identifier.
	 * 
	 * @return group identifier
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the artifact identifier.
	 * 
	 * @return artifact identifier
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Returns the version number.
	 * 
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the type of the artifact.
	 * 
	 * @return type of the artifact
	 */
	public String getType() {
		return type == null ? "jar" : type;
	}

	/**
	 * Returns a path to a local file.
	 * 
	 * @return path to a local file
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Sets a path to a local file.
	 * 
	 * @param file path to a local file
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * Validates this artifact identity and throws an exception if it is incomplete or
	 * invalid.
	 * 
	 * @throws MojoExecutionException if identity is invalid
	 */
	public void validate() throws MojoExecutionException {
		if (file == null) {
			if (groupId == null) {
				throw new MojoExecutionException("groupId must be specified");
			}
			if (artifactId == null) {
				throw new MojoExecutionException("artifactId must be specified");
			}
			if (version == null) {
				throw new MojoExecutionException("version must be specified");
			}
		} else {
			if (!(new File(file).exists())) {
				throw new MojoExecutionException(MessageFormat.format("file {0} does not exist", new Object[] { file }));
			}
		}
	}

}
