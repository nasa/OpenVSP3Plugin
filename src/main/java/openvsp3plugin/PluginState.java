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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents the state of the plugin instance.
 * It can be created from an XML string or output as an XML string.
 * ModelCenter uses this XML string to store the plugin state in the model file.
 * In OpenMDAO mode the XML state string is read as input.
 */
public class PluginState {

	private String openVSPFilename;
	private final ObservableList<DesignVariable> designVariables;
	private final boolean flatNames;
	private final boolean addID;
	private final boolean groupOutputs;
	private final Double epsilon;
	private final int setID;
	private final int nApplyDes;
	private final String pluginVersion;
	private final String vspVersion;
	private final String logLevel;

	static public String argString(PluginState state) {
		return (state == null) ? "null" : "not null";
	}
	
	static public PluginState fromString(String contents) throws Exception {
		ObservableList<DesignVariable> designVariables = FXCollections.<DesignVariable>observableArrayList();
		XPathUtil xpu = new XPathUtil(contents);
		String root = "/State";
		Node node = xpu.getElementNode(root);
		if (node == null) {
			root = "/Model";
			node = xpu.getElementNode(root);	
		}
		String pluginVersion = XPathUtil.getNodeAttribute(node, "Version", "Pre 1.3");
		String vspVersion = XPathUtil.getNodeAttribute(node, "VSPVersion", "Unknown");
		String logLevel = XPathUtil.getNodeAttribute(node, "LogLevel", "INFO");
		String filename = XPathUtil.getNodeAttribute(node, "ID", "");
		String naming = XPathUtil.getNodeAttribute(node, "NamingCode", "");
		if (naming.length() == 3) naming = naming + "0";
		Double epsilon = null;
		String epsilonString = XPathUtil.getNodeAttribute(node, "Epsilon", "");
		try {
			epsilon = new Double(epsilonString);
		} catch (Exception ex) {}
		String setIDString = XPathUtil.getNodeAttribute(node, "SetID", "");
		int setID = 1;
		if (!setIDString.isEmpty()) {
			try {
				setID = Integer.parseInt(setIDString);
			} catch (Exception ex) {
				setID = 1;
			}
		}
		String nApplyDesString = XPathUtil.getNodeAttribute(node, "NApplyDes", "");
		int nApplyDes = 1;
		if (!nApplyDesString.isEmpty()) {
			try {
				nApplyDes = Integer.parseInt(nApplyDesString);
			} catch (Exception ex) {
				nApplyDes = 1;
			}
		}
		NodeList nodes = xpu.getElementNodes(root + "/Variable");
		for (int i = 0; i <= nodes.getLength(); i++) {
			node = nodes.item(i);
			String name = XPathUtil.getNodeAttribute(node, "ID", null);
			String value = XPathUtil.getNodeAttribute(node, "Value", null);
			String vspid = XPathUtil.getNodeAttribute(node, "VSPID", null);
			String state = XPathUtil.getNodeAttribute(node, "STATE", null);
			String xpath = XPathUtil.getNodeAttribute(node, "XPATH", null);
			// Version 1.3 replaced OUPUT with STATE
			String output = XPathUtil.getNodeAttribute(node, "OUTPUT", null);
			if ((name != null) && (value != null) && (vspid != null)) {
				String[] names = name.split(":");
				if (names.length == 3) {
					DesignVariable dv = new DesignVariable(names[0], names[1], names[2], vspid, value);
					if (xpath != null) dv.setXPath(xpath);
					if (state != null) {
						dv.setState(state);
					} else {
						// pre 1.3
						if ((output != null) && output.equals("true")) dv.setState("Output");
					}
					dv.checkedProperty().set(true);
					designVariables.add(dv);
				}
			}
		}
		return new PluginState(filename, designVariables,
				naming.startsWith("1"),	naming.substring(1).startsWith("1"), naming.substring(2).startsWith("1"), epsilon,
				setID, nApplyDes, pluginVersion, vspVersion, logLevel);
	}

	public PluginState(String openVSPPath, ObservableList<DesignVariable> designVariables,
			boolean flatNames, boolean addID, boolean groupOutputs, Double epsilon,
			int setID, int nApplyDes, String pluginVersion, String vspVersion, String logLevel) {
		this.openVSPFilename = openVSPPath;
		// This made a copy of the list (rev 779) but the DesignVariables should also be copied.
		// TODO Does this need to be observable?
		// TODO only needs to be copied when called from JavaFXUI.getPluginState() not PluginState.fromString()
		List<DesignVariable> copylist = designVariables.stream().map(dv -> new DesignVariable(dv)).collect(Collectors.toList());
		this.designVariables = FXCollections.<DesignVariable>observableArrayList(copylist);
		this.flatNames = flatNames;
		this.addID = addID;
		this.groupOutputs = groupOutputs;
		this.epsilon = epsilon;
		this.setID = setID;
		this.nApplyDes = nApplyDes;
		this.pluginVersion = pluginVersion;
		this.vspVersion = vspVersion;
		this.logLevel = logLevel;
	}
	
	public String getOpenVSPFilename() {
		return openVSPFilename;
	}
	
	public void setOpenVSPFilename(String name) {
		openVSPFilename = name;
	}
	
	public ObservableList<DesignVariable> getDesignVariables() {
		return designVariables;
	}

	public boolean getFlatNames() {
		return flatNames;
	}
	
	public boolean getAddID() {
		return addID;
	}
	
	public boolean getGroupOutputs() {
		return groupOutputs;
	}
	
	public Double getEpsilon() {
		return epsilon;
	}
	
	public int getSetID() {
		return setID;
	}
	
	public int getNApplyDes() {
		return nApplyDes;
	}
	
	public String getPluginVersion() {
		return pluginVersion;
	}
	
	public String getVSPVersion() {
		return vspVersion;
	}
	
	public String getLogLevel() {
		return logLevel;
	}
	
	public String getNamingCode() {
		return (flatNames ? "1" : "0") + (addID ? "1" : "0") + (groupOutputs ? "1" : "0");
	}

	@Override
	public String toString() {
		String nl = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>");
		sb.append(nl);
		sb.append(String.format("<State Version=\"%s\" ID=\"%s\" NamingCode=\"%s\" SetID=\"%d\" NApplyDes=\"%d\" VSPVersion=\"%s\" Epsilon=\"%f\" LogLevel=\"%s\">",
				pluginVersion, openVSPFilename, getNamingCode(), setID, nApplyDes, vspVersion, epsilon, logLevel));
		sb.append(nl);
		for (DesignVariable dv : designVariables) {
			sb.append(dv.toStateString());
			sb.append(nl);
		}
		sb.append("</State>");
		sb.append(nl);
		return sb.toString();
	}

	public void writeXDDMFile(File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write("<?xml version=\"1.0\"?>");
		bw.newLine();
		bw.write(String.format("<Model ID=\"%s\" Modeler=\"%s\" Wrapper=\"%s\">", openVSPFilename, "OpenVSP", "wrap_vsp.csh"));
		bw.newLine();
		for (DesignVariable dv : designVariables) {
			if (!dv.isOutput()) {
				bw.write(dv.toXDDMString());
				bw.newLine();
			}
		}
		bw.write("</Model>");
		bw.newLine();
		bw.close();
	}
	
	public void writeDesFile(File file, boolean sort) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		SortedList<DesignVariable> sortedDesignVariables;
		if (sort) sortedDesignVariables = desFileSort(designVariables);
		else sortedDesignVariables = designVariables.sorted();
		bw.write(String.format("%d", designVariables.filtered(dv -> !dv.isOutput()).size()));
		bw.newLine();
		for (DesignVariable dv : sortedDesignVariables) {
			if (!dv.isOutput()) { 
				bw.write(dv.toDesString());
				bw.newLine();
			}
		}
		bw.close();
	}
	
	private SortedList<DesignVariable> desFileSort(ObservableList<DesignVariable> list) {
		return list.sorted((DesignVariable dv1, DesignVariable dv2) -> {
			int value;
			if (dv1.getContainer().equals(dv2.getContainer())) {
				if (dv1.getGroup().equals(dv2.getGroup())) {
					value = dv1.getName().compareTo(dv2.getName());
				} else {
					value = dv2.getGroup().replace("_", " ").compareTo(dv1.getGroup().replace("_", " "));
				}
			} else {
				value = dv1.getContainer().compareTo(dv2.getContainer());
			}
			return value;
		});
	}
	
	public String getModelCenterName(DesignVariable dv) {
		return dv.getModelCenterName(flatNames, addID, groupOutputs);
	}
	
	public String getCompareString(PluginState newState) {
		StringBuilder sb = new StringBuilder();
		if (!pluginVersion.equals(newState.pluginVersion)) sb.append(String.format("Plugin version: %s -> %s\n", pluginVersion, newState.pluginVersion));
		if (!vspVersion.equals(newState.vspVersion)) sb.append(String.format("OpenVSP version: %s -> %s\n", vspVersion, newState.vspVersion));
		if (!openVSPFilename.equals(newState.openVSPFilename)) sb.append(String.format("OpenVSP File: %s -> %s\n", openVSPFilename, newState.openVSPFilename));
		if (!getNamingCode().equals(newState.getNamingCode())) sb.append(String.format("Naming Code: %s -> %s\n", getNamingCode(), newState.getNamingCode()));
		if (!compareEpsilons(epsilon, newState.epsilon)) sb.append(String.format("Epsilon: %f -> %f\n", epsilon, newState.epsilon));
		if (setID != newState.setID) sb.append(String.format("Set ID: %d -> %d\n", setID, newState.setID));
		if (nApplyDes != newState.nApplyDes) sb.append(String.format("nApplyDes: %d -> %d\n", nApplyDes, newState.nApplyDes));
		if (!logLevel.equals(newState.logLevel)) sb.append(String.format("Loging level %s -> %s\n", logLevel, newState.logLevel));
		if (designVariables.size() != newState.designVariables.size()) {
			sb.append(String.format("Num Design Variables: %d -> %d\n", designVariables.size(), newState.designVariables.size()));
		}
		sb.append(getCompareDesignVariableListString(newState));
		return sb.toString();
	}
	
	public String getCompareDesignVariableListString(PluginState newState) {
		StringBuilder sb = new StringBuilder();
		List<String> list1 = designVariables.stream().map(dv -> dv.getFullName()).collect(Collectors.toList());
		List<String> list2 = newState.designVariables.stream().map(dv -> dv.getFullName()).collect(Collectors.toList());
		list1.stream().filter((s) -> (!list2.contains(s))).forEach((s) -> {
			sb.append("Removed ").append(s).append("\n");
		});
		list2.stream().filter((s) -> (!list1.contains(s))).forEach((s) -> {
			sb.append("Added ").append(s).append("\n");
		});
		return sb.toString();
	}
	
	public boolean compareEpsilons(Double e1, Double e2) {
		if (e1 == null) return (e2 == null);
		if (e2 == null) return false;
		return e1.equals(e2);
	}
	
	public enum CompareStatus {
		SAME, SIMILAR, DIFFERENT
	}
	
	public CompareStatus compareTo(PluginState other) {
		if (!openVSPFilename.equals(other.openVSPFilename)
				|| !getNamingCode().equals(other.getNamingCode())
				|| (setID != other.setID)
				|| (designVariables.size() != other.designVariables.size())
				|| !getCompareDesignVariableListString(other).isEmpty()
				|| !compareEpsilons(epsilon, other.epsilon))
			return CompareStatus.DIFFERENT;
		if ((nApplyDes != other.nApplyDes)
				|| !pluginVersion.equals(other.pluginVersion)
				|| !vspVersion.equals(other.vspVersion)
				|| !logLevel.equals(other.logLevel))
			return CompareStatus.SIMILAR;
		return CompareStatus.SAME;
	}
}
