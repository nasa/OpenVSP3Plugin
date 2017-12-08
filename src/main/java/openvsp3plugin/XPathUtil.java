/**
 * Copyright 2017 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * 
 * All Rights Reserved.
 * 
 * The OpenVSP3Plugin platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package openvsp3plugin;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility class to parse XML files using xpath
 */
public class XPathUtil {

	private static Boolean ignoreNumberFormatErrors = false;

	public static void setIgnoreNumberFormatErrors(Boolean value) {
		ignoreNumberFormatErrors = value;
	}

	private final Document doc;
	private final XPath xp;
	
	public XPathUtil(File input) throws ParserConfigurationException, SAXException, IOException {
		this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
		this.xp = XPathFactory.newInstance().newXPath();
	}
	
	public XPathUtil(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		this.doc = builder.parse(new InputSource(new StringReader(xml)));
		this.xp = XPathFactory.newInstance().newXPath();
	}

	public String getElement(String expression) {
		String element = "";
		try {
			element = xp.evaluate(expression, doc);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return element;
	}

	public Node getElementNode(String expression) {
		Node n = null;
		try {
			n = (Node) xp.evaluate(expression, doc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return n;
	}

	public NodeList getElementNodes(String expression) {
		NodeList n = null;
		try {
			n = (NodeList) xp.evaluate(expression, doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return n;
	}

	public int getInteger(String expression) {
		int value = 0;
		try {
			value = Integer.parseInt(getElement(expression));
		} catch (NumberFormatException e) {
			if (ignoreNumberFormatErrors) {
				System.err.println("***** getInteger:NumberFormatException parsing \"" + getElement(expression) + "\"");
			} else {
				throw e;
			}
		}
		return value;
	}

	public String getElementAttribute(String expression, String attribute, String defaultValue) {
		String value = defaultValue;
		Node node = getElementNode(expression);
		if (node != null) {
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				Node attributeNode = attributes.getNamedItem(attribute);
				if (attributeNode != null) {
					value = attributeNode.getNodeValue();
				}
			}
		}
		return value;
	}

	public static String getNodeAttribute(Node node, String attribute, String defaultValue) {
		String value = defaultValue;
		if (node != null) {
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				Node attributeNode = attributes.getNamedItem(attribute);
				if (attributeNode != null) {
					value = attributeNode.getNodeValue();
				}
			}
		}
		return value;
	}
}
