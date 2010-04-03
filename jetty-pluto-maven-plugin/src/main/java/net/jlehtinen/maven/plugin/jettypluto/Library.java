package net.jlehtinen.maven.plugin.jettypluto;

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

public class Library extends ArtifactIdentity {
	
	protected String jar;
	
	public Library() {
	}
	
	public Library(String groupId, String artifactId, String version) {
		super(groupId, artifactId, version, "jar");
	}

	public String getJar() {
		return jar;
	}

	public void setJar(String jar) {
		this.jar = jar;
	}

	public void validate() throws MojoExecutionException {
		if (jar == null) {
			super.validate();
			if (!"jar".equals(getType())) {
				throw new MojoExecutionException("type must be jar");
			}
		} else {
			if (!(new File(jar).exists())) {
				throw new MojoExecutionException(MessageFormat.format("file {0} does not exist", new Object[] { jar }));
			}
		}
	}

}
