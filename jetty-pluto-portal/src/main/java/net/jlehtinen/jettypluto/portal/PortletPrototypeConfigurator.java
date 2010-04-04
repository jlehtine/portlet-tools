/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jlehtinen.jettypluto.portal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.pluto.driver.config.DriverConfiguration;
import org.apache.pluto.driver.services.portal.PageConfig;
import org.apache.pluto.driver.services.portal.PortletWindowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * Configures the Pluto portal for the portlet prototype.
 */
public class PortletPrototypeConfigurator implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(PortletPrototypeConfigurator.class);
	
	protected static final String PORTLET_CONTEXT_PROPERTY = "portletContext";
	
	protected static final String PORTLET_IDS_PROPERTY = "portletIds";
	
	protected static final String PORTLET_PAGE_NAME = "Portlet Prototyping";
	
	protected static final String PORTLET_PAGE_URI = "/WEB-INF/themes/pluto-default-theme.jsp";
	
	public void contextInitialized(ServletContextEvent event) {
		
		// Lookup servlet context
		ServletContext servletContext = event.getServletContext();
		logger.info("Configuring Pluto portal for portlet prototyping");

		// Get prototype portlet information
		String portletContext = System.getProperty(PORTLET_CONTEXT_PROPERTY);
		String portletIds = System.getProperty(PORTLET_IDS_PROPERTY);
		if (portletContext == null) {
			logger.warn(MessageFormat.format("System property {0} not set, skipping portlet configuration", new Object[] { PORTLET_CONTEXT_PROPERTY }));
			return;
		}
		if (portletIds == null) {
			logger.warn(MessageFormat.format("System property {0} not set, skipping portlet configuration", new Object[] { PORTLET_IDS_PROPERTY }));
			return;
		}
		logger.info(MessageFormat.format("Portlet context = {0}", new Object[] { portletContext }));
		logger.info(MessageFormat.format("Portlet identifiers = {0}", new Object[] { portletIds }));

		// Parse portlet identifiers
		String[] parsedPortletIds = portletIds.split("[,\\s]+");
		
		// Look up Spring context
		WebApplicationContext springContext =
			(WebApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		
		// Look up Pluto driver configuration
		DriverConfiguration driverConfig =
			(DriverConfiguration) springContext.getBean("DriverConfiguration");

		// Configure prototyping page, if any portlets defined
		if (parsedPortletIds.length > 0) {
			PageConfig pageConfig = new PageConfig();
			pageConfig.setName("Portlet Prototyping");
			pageConfig.setUri(PORTLET_PAGE_URI);
			Collection portletConfigs = new ArrayList();
			for (int i = 0; i < parsedPortletIds.length; i++) {
				PortletWindowConfig portletConfig = new PortletWindowConfig();
				portletConfig.setContextPath(portletContext);
				portletConfig.setPortletName(parsedPortletIds[i]);
				portletConfigs.add(portletConfig);
			}
			pageConfig.setPortletIds(portletConfigs);
			driverConfig.getPages().add(pageConfig);
		}
		
		logger.info(MessageFormat.format("Configured {0} prototype portlets", new Object[] { new Integer(parsedPortletIds.length) }));
	}

	public void contextDestroyed(ServletContextEvent event) {
	}

}
