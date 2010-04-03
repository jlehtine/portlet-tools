package net.jlehtinen.maven.plugin.jettypluto;

import org.apache.maven.plugin.MojoExecutionException;

public abstract class ArtifactIdentity {

	protected String groupId;
	
	protected String artifactId;
	
	protected String version;
	
	protected String type;

	public ArtifactIdentity() {
	}
	
	public ArtifactIdentity(String groupId, String artifactId, String version, String type) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.type = type;
	}
	
	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getType() {
		return type == null ? "jar" : type;
	}

	public void validate() throws MojoExecutionException {
		if (groupId == null) {
			throw new MojoExecutionException("groupId must be specified");
		}
		if (artifactId == null) {
			throw new MojoExecutionException("artifactId must be specified");
		}
		if (version == null) {
			throw new MojoExecutionException("version must be specified");
		}
	}

}
