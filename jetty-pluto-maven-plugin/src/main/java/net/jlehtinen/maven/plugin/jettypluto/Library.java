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
			if (!"jar".equals(getPackaging())) {
				throw new MojoExecutionException("type must be jar");
			}
		}
	}

}
