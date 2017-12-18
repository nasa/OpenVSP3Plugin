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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import static openvsp3plugin.OpenVSP3Plugin.CFDEXPORTS;
import static openvsp3plugin.OpenVSP3Plugin.CFDFILE;
import static openvsp3plugin.OpenVSP3Plugin.COMPGEOM;
import static openvsp3plugin.OpenVSP3Plugin.COMPGEOM2;
import static openvsp3plugin.OpenVSP3Plugin.EXPORTS;
import static openvsp3plugin.OpenVSP3Plugin.FILE;
import static openvsp3plugin.OpenVSP3Plugin.MASSPROP;
import static openvsp3plugin.OpenVSP3Plugin.MASSPROPVALUES;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains the logic to parse the OpenVSP3 file.
 * 
 * 
 */
public class OpenVSP3File {
	
	public static final String USERPARMS = "UserParms";
	public static final String[] SUBSURFACES = {"SubSurface", "SS_Rectangle", "SS_Ellipse", "SS_Control"};
	public static final String[] CHOICEVECTOR = {"Aspect", "Span", "Area", "Taper", "Avg_Chord", "Root_Chord", "Tip_Chord", "Sec_Sweep"};
	
	private static final String USERPARMPATH = "/Vsp_Geometry/UserParmContainer/UserParm";
	private static final String GEOMETRYPATH = "/Vsp_Geometry/Vehicle/Geom";
	private static final String FUSEGEOMPATH = "FuselageGeom/XSecSurf/XSec";
	private static final String WINGGEOMPATH = "WingGeom/XSecSurf/XSec";
	private static final String PROPGEOMPATH = "PropellerGeom/XSecSurf/XSec";

	private XPathUtil xpu;
	private final boolean addID;
	private final ArrayList<String> setNames = new ArrayList<>();
	private final ArrayList<Integer> nSyms = new ArrayList<>();
	// pass in from controller so it can add a callback = FXCollections.observableArrayList();
	private final ObservableList<DesignVariable> designVariables;
	private final ArrayList<TreeItem<DesignVariableGroup>> containerArrayList = new ArrayList<>();
	private final TreeItem<DesignVariableGroup> compGeomContainer = new TreeItem<>(new DesignVariableGroup(COMPGEOM));
	private final TreeItem<DesignVariableGroup> compGeom2Container = new TreeItem<>(new DesignVariableGroup(COMPGEOM2));
	private final Pattern invalidChars = Pattern.compile("[^a-zA-Z0-9_]"); // could also use "\W" predifined pattern
	private final Pattern hidePattern = Pattern.compile(":XSecCurve.*:Chord");

	// design Variables needs to be constructed in the caller so that is can be created with a callback
	public OpenVSP3File(ObservableList<DesignVariable> designVariables, boolean addID) {
		this.designVariables = designVariables;
		this.addID = addID;
	}
	
	public ArrayList<String> getSetNames() {
		return setNames;
	}
	
	public ArrayList<TreeItem<DesignVariableGroup>> getContainerArrayList() {
		return containerArrayList;
	}
	
	public void read(File file) throws Exception {
		designVariables.clear();
		xpu = new XPathUtil(file);
		loadSetNames();
		loadContainerArrayList();
	}
	
	private void loadSetNames() {
		NodeList nodes = xpu.getElementNodes("/Vsp_Geometry/SetNames/Set");
		for (int i = 0; i < nodes.getLength(); i++) {
			setNames.add(nodes.item(i).getTextContent());
		}
	}
	
	private void loadContainerArrayList() throws Exception {
		containerArrayList.add(getUserParmContainer());
		nSyms.clear();
		int nGeoms = xpu.getElementNodes(GEOMETRYPATH).getLength();
		for (int i = 1; i <= nGeoms; i++) {
			containerArrayList.add(getGeomContainer(GEOMETRYPATH + "[" + i + "]/"));
		}
		addTopLevelContainers();
		containerArrayList.add(getFileContainer());
		containerArrayList.add(compGeomContainer);
		containerArrayList.add(compGeom2Container);
		containerArrayList.add(getMassPropsContainer(nGeoms));
	}
	
	private TreeItem<DesignVariableGroup> getUserParmContainer() throws Exception {
		Map<String, TreeItem<DesignVariableGroup>> map = new HashMap<>();
		TreeItem<DesignVariableGroup> container = new TreeItem<>(new DesignVariableGroup(USERPARMS));
		NodeList nodes = xpu.getElementNodes(USERPARMPATH);
		for (int i = 1; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			String groupName = XPathUtil.getNodeAttribute(node, "GroupName", null);
			if (groupName != null) {
				TreeItem<DesignVariableGroup> group;
				if (map.containsKey(groupName)) {
					group = map.get(groupName);
				} else {
					group =  new TreeItem<>(new DesignVariableGroup(groupName));
					map.put(groupName, group);
					container.getChildren().add(group);	
				}
				addDesignVariable(container, group, XPathUtil.getNodeAttribute(node, "Name", ""),
						XPathUtil.getNodeAttribute(node, "ID", ""),
						XPathUtil.getNodeAttribute(node, "Value", ""),
						USERPARMPATH + "[" + i + "]",
						false);
			}
		}
		return container;
	}
	
	private TreeItem<DesignVariableGroup> getGeomContainer(String prefix) throws Exception {
		TreeItem<DesignVariableGroup> container = getContainer(prefix + "ParmContainer");
		int type = xpu.getInteger(prefix + "GeomBase/TypeID");
		switch (type) {
			case 4: // fuselage
			case 8: // stack
				addGroups(container, prefix + FUSEGEOMPATH, "Cap", "XSec/XSecCurve/ParmContainer/Cap", 1);
				addGroups(container, prefix + FUSEGEOMPATH, "Close", "XSec/XSecCurve/ParmContainer/Close", 1);
				addGroups(container, prefix + FUSEGEOMPATH, "Trim", "XSec/XSecCurve/ParmContainer/Trim", 1);
				addGroups(container, prefix + FUSEGEOMPATH, "XSecCurve", "XSec/XSecCurve/ParmContainer/XSecCurve", 1);
				addGroups(container, prefix + FUSEGEOMPATH, "XSec", "ParmContainer/XSec", 1);
				break;
			case 5: // wing
				addGroups(container, prefix + WINGGEOMPATH, "Cap", "XSec/XSecCurve/ParmContainer/Cap", 1);
				addGroups(container, prefix + WINGGEOMPATH, "Close", "XSec/XSecCurve/ParmContainer/Close", 1);
				addGroups(container, prefix + WINGGEOMPATH, "LowerCoeff", "XSec/XSecCurve/ParmContainer/LowerCoeff", 1);
				addGroups(container, prefix + WINGGEOMPATH, "Trim", "XSec/XSecCurve/ParmContainer/Trim", 1);
				addGroups(container, prefix + WINGGEOMPATH, "UpperCoeff", "XSec/XSecCurve/ParmContainer/UpperCoeff", 1);
				addGroups(container, prefix + WINGGEOMPATH, "XSecCurve", "XSec/XSecCurve/ParmContainer/XSecCurve", 1);
				addWingSections(container, prefix + WINGGEOMPATH, "XSec", "ParmContainer/XSec", 1);
				break;
			case 11:
				addGroup(container, prefix + "PropellerGeom/Chord/ParmContainer/Chord");
				addGroup(container, prefix + "PropellerGeom/Rake/ParmContainer/Rake");
				addGroup(container, prefix + "PropellerGeom/Skew/ParmContainer/Skew");
				addGroup(container, prefix + "PropellerGeom/Twist/ParmContainer/Twist");
				addGroups(container, prefix + PROPGEOMPATH, "Cap", "XSec/XSecCurve/ParmContainer/Cap", 1);
				addGroups(container, prefix + PROPGEOMPATH, "Close", "XSec/XSecCurve/ParmContainer/Close", 1);
				addGroups(container, prefix + PROPGEOMPATH, "Trim", "XSec/XSecCurve/ParmContainer/Trim", 1);
				addGroups(container, prefix + PROPGEOMPATH, "XSecCurve", "XSec/XSecCurve/ParmContainer/XSecCurve", 1);
				addGroups(container, prefix + PROPGEOMPATH, "XSec", "ParmContainer/XSec", 1);
				break;
			case 15:
				addContainer(container, prefix + "XSecCurve/ParmContainer");
				break;
		}
		// sub surfaces
		int[] counters = new int[SUBSURFACES.length];
		for (int i = 0; i < SUBSURFACES.length; i++) counters[i] = 1;
		int nSubSurface = xpu.getElementNodes(prefix + "Geom/SubSurfaces/SubSurface").getLength();
		for (int i = 1; i <= nSubSurface; i++) {
			String ssPrefix = prefix + "Geom/SubSurfaces/SubSurface[" + i + "]";
			String sspPrefix = ssPrefix + "/ParmContainer";
			int ssType = xpu.getInteger(ssPrefix + "/SubSurfaceInfo/Type");
			String name = SUBSURFACES[ssType] + "_" + counters[ssType]++;
			Node node = xpu.getElementNode(sspPrefix + "/" + SUBSURFACES[ssType]);
			TreeItem<DesignVariableGroup> group = new TreeItem<>(new DesignVariableGroup(name));
			addDesignVariables(container, group, node, sspPrefix + "/" + node.getNodeName());
			container.getChildren().add(group);
		}
		return container;
	}
	
	private void addTopLevelContainers() throws Exception {
		NodeList nodes = xpu.getElementNodes("/Vsp_Geometry/*/ParmContainer");
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			Node parent = node.getParentNode();
			containerArrayList.add(getContainer("/Vsp_Geometry/" + parent.getNodeName() + "/ParmContainer"));
		}
	}
	
	private TreeItem<DesignVariableGroup> getContainer(String prefix) throws Exception {
		String name = xpu.getElement(prefix + "/Name");
		TreeItem<DesignVariableGroup> container = new TreeItem<>(new DesignVariableGroup(name));
		addContainer(container, prefix);
		if (name.equals("CFDMeshSettings")) addCFDFileContainer(container);
		return container;
	}
	
	private TreeItem<DesignVariableGroup> getFileContainer() throws Exception {
		// Exportable file types
		TreeItem<DesignVariableGroup> container = new TreeItem<>(new DesignVariableGroup(FILE));
		addOutputGroup(container, FILE, EXPORTS, FILE);
		return container;
	}
	
	private TreeItem<DesignVariableGroup> addCFDFileContainer(TreeItem<DesignVariableGroup> container) throws Exception {
		// CFD file types
		addOutputGroup(container, CFDFILE, CFDEXPORTS, CFDFILE);
		return container;
	}

	private TreeItem<DesignVariableGroup> getMassPropsContainer(int nGeoms) throws Exception {
		// MassProps Data
		TreeItem<DesignVariableGroup> container = new TreeItem<>(new DesignVariableGroup(MASSPROP));
		// use container name for id
		String id = MASSPROP;
		// components
		for (int i = 1; i <= nGeoms; i++) {
			// figure out symmetry (nSyms for comp geom)
			String prefix = GEOMETRYPATH + "[" + i + "]/";
			int type = xpu.getInteger(prefix + "/GeomBase/TypeID");
			if ((type == 6) || (type == 12)) {
				nSyms.add(0);
			} else {
				String symPrefix = prefix + "ParmContainer/Sym/";
				int aFlag = (int)Double.parseDouble(xpu.getElementAttribute(symPrefix + "Sym_Axial_Flag", "Value", "0.0"));
				int pFlag = (int)Double.parseDouble(xpu.getElementAttribute(symPrefix + "Sym_Planar_Flag", "Value", "0.0"));
				int nRot  = (int)Double.parseDouble(xpu.getElementAttribute(symPrefix + "Sym_Rot_N", "Value", "0.0"));
				int ns = (aFlag == 0) ? 1 : nRot;
				if ((pFlag & 1) == 1) ns *= 2;
				if ((pFlag & 2) == 2) ns *= 2;
				if ((pFlag & 4) == 4) ns *= 2;
				if (type == 11) {
					int nBlades = (int)Double.parseDouble(xpu.getElementAttribute(prefix + "ParmContainer/Design/NumBlade", "Value", "0.0"));
					ns *= nBlades;
				}
				nSyms.add(ns);
			}
			String groupName = containerArrayList.get(i).getValue().getName();
			for (int j = 0; j < nSyms.get(i-1); j++) {
				addOutputGroup(container, groupName + j, MASSPROPVALUES, id);
			}
		}
		// Totals
		addOutputGroup(container, "Totals", MASSPROPVALUES, id);
		return container;
	}
	
	private void addContainer(TreeItem<DesignVariableGroup> container, String prefix) throws Exception {
		NodeList nodes = xpu.getElementNode(prefix).getChildNodes();
		for (int i = 1; i <= nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node != null) {
				String group = node.getNodeName();
				if (group.equals("ID") || group.equals("Name") || group.equals("#text")) continue;
				addGroup(container, node, prefix + "/" + group);
			}
		}
	}
	
	private void addWingSections(TreeItem<DesignVariableGroup> container, String prefix, String name, String path, int start) throws Exception {
		NodeList nodes = xpu.getElementNodes(prefix);
		for (int i = start; i <= nodes.getLength(); i++) {
			String sectionPrefix = prefix + "[" + i + "]/";
			TreeItem<DesignVariableGroup> group = new TreeItem<>(new DesignVariableGroup(name + "_" + (i - 1)));
			Node node = xpu.getElementNode(sectionPrefix + path);
			String driverstring = xpu.getElement(sectionPrefix + "XSec/DriverGroup/ChoiceVec");
			if (node != null) {
				HashSet<String> outputs = new HashSet<>();
				for (int j = 0; j < CHOICEVECTOR.length; j++) {
					if (!driverstring.contains(String.format("%d",j)))
						outputs.add(CHOICEVECTOR[j]);
				}
				NodeList nodes2 = node.getChildNodes();
				for (int j = 1; j < nodes2.getLength(); j++) {
					Node child = nodes2.item(j);
					if ((child != null) && !child.getNodeName().startsWith("#text")) {
						addDesignVariable(container, group, child.getNodeName(),
								XPathUtil.getNodeAttribute(child, "ID", ""),
								XPathUtil.getNodeAttribute(child, "Value", ""),
								sectionPrefix + path + "/" + child.getNodeName(),
								outputs.contains(child.getNodeName()));
					}
				}
			}
			container.getChildren().add(group);
		}
	}
	
	private void addGroups(TreeItem<DesignVariableGroup> container, String prefix, String name, String path, int start) throws Exception {
		NodeList nodes = xpu.getElementNodes(prefix);
		for (int i = start; i <= nodes.getLength(); i++) {
			String sectionPrefix = prefix + "[" + i + "]/";
			TreeItem<DesignVariableGroup> group = new TreeItem<>(new DesignVariableGroup(name +"_" + (i-1)));
			Node node = xpu.getElementNode(sectionPrefix + path);
			if (node != null) {
				addDesignVariables(container, group, node, sectionPrefix + "/" + path);
				container.getChildren().add(group);
			}
		}
	}
	
	private void addGroup(TreeItem<DesignVariableGroup> container, String xpath) throws Exception {
		Node node = xpu.getElementNode(xpath);
		addGroup(container, node, xpath);
	}
	
	private void addGroup(TreeItem<DesignVariableGroup> container, Node node, String xpath) throws Exception {
		if (node != null) {
			TreeItem<DesignVariableGroup> group = new TreeItem<>(new DesignVariableGroup(node.getNodeName()));
			addDesignVariables(container, group, node, xpath);
			container.getChildren().add(group);
		}
	}
	
	private void addDesignVariables(TreeItem<DesignVariableGroup> container, TreeItem<DesignVariableGroup> group,
			Node node, String xpath) throws Exception {
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if ((child != null) && !child.getNodeName().startsWith("#text")) {
				addDesignVariable(container, group, child.getNodeName(),
						XPathUtil.getNodeAttribute(child, "ID", ""),
						XPathUtil.getNodeAttribute(child, "Value", ""), xpath + "/" + child.getNodeName(), false);
			}
		}
	}
	
	private void addDesignVariable(TreeItem<DesignVariableGroup> container, TreeItem<DesignVariableGroup> group,
			String name, String id, String value, String xpath, boolean isOutput) throws Exception {
		DesignVariable dv = new DesignVariable(container.getValue().getName(), group.getValue().getName(), name, id, value);
		dv.setXPath(xpath);
		dv.setVspValue(value);
		// check for naming issues
		String fullName = dv.getFullName();
		ObservableList<DesignVariable> existing = designVariables.filtered(v -> (v.getFullName().equals(fullName)));
		if (!addID && (existing.size() > 0)) {
			throw new Exception("Design Variable Naming problem: " + dv.getFullName() + " already exists.");
		}
		Matcher matcher = invalidChars.matcher(dv.getModelCenterName(true, false, false));
		while (matcher.find()) {
			String message = String.format("Design Variable Naming problem: \"%s\" contains an invalid character \"%s\"",
					dv.getModelCenterName(true, false, false), matcher.group());
			throw new Exception(message);
		}
		if (shouldHide(dv)) {
			return;
		}
		if (isOutput || shouldMakeOutput(dv)) {
			dv.setState("Output");
		}
		container.getValue().getDesignVariables().add(dv);
		group.getValue().getDesignVariables().add(dv);
		designVariables.add(dv);
	}
	
	private void addOutputGroup(TreeItem<DesignVariableGroup> container, String groupName,
									String[] groupValues, String id) throws Exception {
		TreeItem<DesignVariableGroup> group = new TreeItem<>(new DesignVariableGroup(groupName));
		for (String variable : groupValues) {
			addDesignVariable(container, group, variable, id, "0", "", true);
		}
		container.getChildren().add(group);
	}
	
	private boolean shouldHide(DesignVariable dv) {
		return hidePattern.matcher(dv.getFullName()).find();
	}
	
	private boolean shouldMakeOutput(DesignVariable dv) {
		return false;
	}
	
	public void updateCompGeom(Map<String, String> map) throws Exception {
		compGeomContainer.getChildren().clear();
		String groupName = "";
		TreeItem<DesignVariableGroup> group = null;
		for (String key : map.keySet()) {
			String[] names = key.split(":");
			if (names.length == 3) {
				if (!groupName.equals(names[1])) {
					groupName = names[1];
					group = new TreeItem<>(new DesignVariableGroup(groupName));
					compGeomContainer.getChildren().add(group);
				}	
				if (group != null) addDesignVariable(compGeomContainer, group, names[2], names[0], "0", "", true);
			}
		}
	}
	
	public void updateTagCompGeom(Map<String, String> map) throws Exception {
		compGeom2Container.getChildren().clear();
		String groupName = "";
		TreeItem<DesignVariableGroup> group = null;
		for (String key : map.keySet()) {
			String[] names = key.split(":");
			if (names.length == 3) {
				if (!groupName.equals(names[1])) {
					groupName = names[1];
					group = new TreeItem<>(new DesignVariableGroup(groupName));
					compGeom2Container.getChildren().add(group);
				}	
				if (group != null) addDesignVariable(compGeom2Container, group, names[2], names[0], "0", "", true);
			}
		}
	}
}
