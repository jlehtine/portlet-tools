package net.jlehtinen.portlet.prototyping.maven.util;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides static utility methods for processing portlet.xml files.
 */
public abstract class PortletXmlUtils {

	/**
	 * Loads portlet descriptor from the specified file.
	 * 
	 * @param file portlet.xml to be loaded
	 * @return portlet.xml DOM document
	 */
	public static Document load(File file) throws IOException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(file);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Portlet descriptor parsing error", e);
		}
	}
	
	/**
	 * Filters away all the portlets not contained in the specified set of included
	 * portlets.
	 *  
	 * @param portletXmlDoc DOM document of the portlet.xml to be filtered
	 * @param includedPortlets set of portlet names to be included
	 * @throws IOException
	 */
	public static void filterPortlets(Document portletXmlDoc, Set includedPortlets) {
		Node root = portletXmlDoc.getDocumentElement();
		
		// Check that this is a portlet descriptor
		if (root.getLocalName().equals("portlet-app")) {
			
			// Go through all possible portlet elements
			NodeList rootChildren = root.getChildNodes();
			for (int i = 0; i < rootChildren.getLength(); i++) {
				Node child = rootChildren.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE && child.getLocalName().equals("portlet")) {
					
					// Check portlet name
					String portletName = null;
					NodeList portletChildren = child.getChildNodes();
					for (int j = 0; j < portletChildren.getLength(); j++) {
						Node c = portletChildren.item(j);
						if (c.getNodeType() == Node.ELEMENT_NODE && c.getLocalName().equals("portlet-name")) {
							portletName = c.getTextContent();
						}
					}
					
					// Filter portlet if necessary
					if (portletName != null && !includedPortlets.contains(portletName)) {
						root.removeChild(child);
					}
				}
			}
		}
	}
	
	/**
	 * Saves portlet descriptor to the specified file.
	 * 
	 * @param portletXmlDoc portlet descriptor document
	 * @param file destination file
	 */
	public static void save(Document portletXmlDoc, File file) throws IOException {
		try {
			file.getParentFile().mkdirs();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			Source src = new DOMSource(portletXmlDoc);
			Result res = new StreamResult(file);
			t.transform(src, res);
		} catch (Exception e) {
			throw new IOException("Portlet descriptor encoding error", e);
		}
	}
}
