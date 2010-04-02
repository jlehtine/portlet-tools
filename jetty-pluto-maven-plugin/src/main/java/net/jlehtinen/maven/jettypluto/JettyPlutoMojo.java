package net.jlehtinen.maven.jettypluto;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.mortbay.jetty.plugin.Jetty6RunMojo;

/**
 * Run the portlet being developed in a Pluto container under Jetty without the time consuming
 * deploy process.
 * 
 * @extendsPlugin jetty
 * @goal run
 */
public class JettyPlutoMojo extends Jetty6RunMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		// TODO: Deploy Pluto to Jetty
		
		// TODO: Prepare portlet for Pluto
		
		super.execute();
	}

}
