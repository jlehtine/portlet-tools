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
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.pluto.driver.services.impl.resource.RenderConfigServiceImpl;
import org.apache.pluto.driver.services.portal.PageConfig;
import org.apache.pluto.driver.services.portal.RenderConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customized version of {@link RenderConfigService} that automatically creates a default
 * page for the portlets to be prototyped.
 */
public class PortletPrototypingRenderConfigService extends RenderConfigServiceImpl {

	/** The logger to use */
	private static final Logger logger = LoggerFactory.getLogger(PortletPrototypingRenderConfigService.class);
	
	/** System property for the portlet context */
	protected static final String PORTLET_CONTEXT_PATH_PROPERTY = "portletContextPath";
	
	/** System property for the portlet names */
	protected static final String PORTLET_NAMES_PROPERTY = "portletNames";

	/** Name of the portlet prototyping page */
	protected static final String PORTLET_PAGE_NAME = "Portlet Prototyping";
	
	/** Identifier of the portlet prototyping page */
	protected static final String PORTLET_PAGE_ID = "/" + PORTLET_PAGE_NAME;
	
	/** URI of the portlet prototyping page */
	protected static final String PORTLET_PAGE_URI = "/WEB-INF/themes/pluto-default-theme.jsp";

	/** Configuration of the portlet prototyping page */
	protected PageConfig portletPrototypingPage = null;
	
	public void init(ServletContext ctx) {
		super.init(ctx);
		
		// Get prototype portlet information
		logger.info("Configuring Pluto portal for portlet prototyping");
		String portletContext = System.getProperty(PORTLET_CONTEXT_PATH_PROPERTY);
		String portletNames = System.getProperty(PORTLET_NAMES_PROPERTY);
		if (portletContext == null) {
			logger.warn(MessageFormat.format("System property {0} not set, skipping configuration", new Object[] { PORTLET_CONTEXT_PATH_PROPERTY }));
			return;
		}
		if (portletNames == null) {
			logger.warn(MessageFormat.format("System property {0} not set, skipping configuration", new Object[] { PORTLET_NAMES_PROPERTY }));
			return;
		}
		logger.info(MessageFormat.format("Portlet context path = {0}", new Object[] { portletContext }));
		logger.info(MessageFormat.format("Portlet names = {0}", new Object[] { portletNames }));

		// Parse portlet identifiers
		String[] parsedPortletNames = portletNames.split(",");
		
		// Configure prototyping page, if any portlets defined
		if (parsedPortletNames.length > 0) {
			portletPrototypingPage = createPortletPrototypingPageConfig(portletContext, parsedPortletNames);
		}
		
		logger.info(MessageFormat.format("Configured {0} prototype portlets", new Object[] { new Integer(parsedPortletNames.length) }));
	}

	/**
	 * Creates a {@link PageConfig} for the portlet prototyping page.
	 * 
	 * @param portletContext context path for portlet implementations
	 * @param portletNames portlet names
	 * @return configuration for the portlet prototyping page
	 */
	protected PageConfig createPortletPrototypingPageConfig(String portletContext, String[] portletNames) {
		PageConfig config = new PageConfig();
		config.setName(PORTLET_PAGE_NAME);
		config.setUri(PORTLET_PAGE_URI);
		for (int i = 0; i < portletNames.length; i++) {
			config.addPortlet(portletContext, portletNames[i]);
		}
		return config;
	}
	
	public PageConfig getDefaultPage() {
		if (portletPrototypingPage != null) {
			return portletPrototypingPage;
		} else {
			return super.getDefaultPage();
		}
	}

	public PageConfig getPage(String id) {
		if (portletPrototypingPage != null && PORTLET_PAGE_ID.equals(id)) {
			return portletPrototypingPage;
		} else {
			return super.getPage(id);
		}
	}

	public List getPages() {
		if (portletPrototypingPage != null) {
			List pages = new ArrayList(super.getPages());
			pages.add(0, portletPrototypingPage);
			return pages;
		} else {
			return super.getPages();
		}
	}

}
