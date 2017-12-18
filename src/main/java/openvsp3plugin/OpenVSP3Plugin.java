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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javafx.collections.ObservableList;

/**
 * This class contains OpenMDAO version of the plugin. 
 */
class OpenVSP3Plugin {
	
	static final String VERSION = "2.0.6";
	static final String TITLE = "OpenVSP 3.0 Plugin v(" + VERSION + ")";
	static final String CSVSPLITSTRING = "\\s+|\\s*,\\s*";
	static final String FILE = "File";
	static final String COMPGEOM = "CompGeom";
	static final String COMPGEOM2 = "TagCompGeom";
	static final String[] COMPGEOMVALUES = {"TheoreticalArea", "WettedArea", "TheoreticalVolume", "WettedVolume"};
	static final String[] COMPGEOMVALUES2 = {"TagTheoreticalArea", "TagWettedArea"};
	static final String MASSPROP = "MassProperties";
	static final String[] MASSPROPVALUES = {"Mass", "cgX", "cgY", "cgZ", "Ixx", "Iyy", "Izz", "Ixy", "Ixz", "Iyz", "Volume"};
	static final HashSet<String> MADETEMP = new HashSet<>(Arrays.asList(new String[] {"stl", "dat", "tri", "msh"}));
	static final String[] EXPORTS = {"vsp3", "DegenGeom", "des", "hrm", "p3d", "stl", "dat", "tri", "msh",
												"pov", "inc", "x3d", "stp", "igs", "dxf", "svg"};
	static final String CFDFILE = "CFDFile";
	static final String[] CFDEXPORTS = {"stl", "poly", "tri", "obj", "dat", "key", "msh", "srf", "tkey", "facet"};
	private static final Logger LOG = new Logger(OpenVSP3Plugin.class.getSimpleName());
			
	static final Map<String, String> EXPORTMAP;
	static
	{
        EXPORTMAP = new HashMap<>();
        EXPORTMAP.put("hrm", "EXPORT_XSEC");
        EXPORTMAP.put("stl", "EXPORT_STL");
		EXPORTMAP.put("dat", "EXPORT_NASCART");
		EXPORTMAP.put("tri", "EXPORT_CART3D");
		EXPORTMAP.put("msh", "EXPORT_GMSH");
		EXPORTMAP.put("pov", "EXPORT_POVRAY");
		EXPORTMAP.put("x3d", "EXPORT_X3D");
		EXPORTMAP.put("stp", "EXPORT_STEP");
		EXPORTMAP.put("p3d", "EXPORT_PLOT3D");
		EXPORTMAP.put("igs", "EXPORT_IGES");
		EXPORTMAP.put("dxf", "EXPORT_DXF");
		EXPORTMAP.put("svg", "EXPORT_SVG");
    }
	
	static final Map<String, String> CFDMAP;
	static
	{
        CFDMAP = new HashMap<>();
        CFDMAP.put("stl", "CFD_STL_TYPE");
        CFDMAP.put("poly", "CFD_POLY_TYPE");
		CFDMAP.put("tri", "CFD_TRI_TYPE");
		CFDMAP.put("obj", "CFD_OBJ_TYPE");
		CFDMAP.put("dat", "CFD_DAT_TYPE");
		CFDMAP.put("key", "CFD_KEY_TYPE");
		CFDMAP.put("msh", "CFD_GMSH_TYPE");
		CFDMAP.put("srf", "CFD_SRF_TYPE");
		CFDMAP.put("tkey", "CFD_TKEY_TYPE");
		CFDMAP.put("facet", "CFD_FACET_TYPE");
    }
	
	private Path openMDAOStatePath = Paths.get("State.xml");
	protected String tempDir;
	protected SwingDialog dialog;
	protected PluginState pluginState = null;
	protected String componentName = "OpenMDAO";
	private String openVSPExe;
	private String openVSPVersion;

	/**
	 * This is the OpenMDAO behavior ModelCenterPlugin overrides this method.
	 */
	@Override
	public String toString() {
		LOG.debug("OpenVSPPlugin.toString() state string:\n" + pluginState.toString());
		return pluginState.toString();
	}
	
	/**
	 * This is the OpenMDAO behavior ModelCenterPlugin overrides this method.
	 */
	void applyPluginState() {
		LOG.trace("apply() OpenMDAO");
		saveOpenMDAO();
	}
	
	/**
	 * This is the OpenMDAO behavior ModelCenterPlugin overrides this method.
	 */
	void resetToOrig() {
		// does nothing
	}
	
	PluginState getPluginState() {
		return pluginState;
	}
	
	void setPluginState(PluginState state) {
		LOG.trace("setPluginState()");
		pluginState = state;
		if (pluginState != null) {
			applyPluginState();
		} else {
			LOG.warn("setPluginState() pluginState set to null, apply() skipped");
		}
	}
	
	String getComponentName() {
		return componentName;
	}
	
	String getOpenVSPExe() {
		return openVSPExe;
	}
	
	String getOpenVSPVersion() {
		return openVSPVersion;
	}
	
	boolean isOpenMDAO() {
		return componentName.equals("OpenMDAO");
	}
	
	void initOpenVSP3Plugin(boolean useTempDir) throws Exception {
		LOG.trace("initOpenVSP3Plugin()");
		// initialize executable and temp dir strings
		openVSPExe = System.getenv("OpenVSP_EXE");
		if ((openVSPExe == null) || openVSPExe.isEmpty()) {
			openVSPExe = "ERROR: Environment variable OpenVSP_EXE not set.";
		} else {
			File file1 = new File(openVSPExe);
			if (file1.isDirectory()) {
				openVSPExe = openVSPExe + "\\vsp.exe";
				File file2 = new File(openVSPExe);
				if (!file2.exists()) {
					openVSPExe = "ERROR: OpenVSP_EXE is a directory (%s) and it does not contain vsp.exe";
				}
			}
		}
		openVSPVersion = extractOpenVSPVersion(openVSPExe);
		String tmp;
		if (useTempDir) {
			tmp = System.getenv("TMP");
			Path basedir = FileSystems.getDefault().getPath(tmp);
			Path tempDirPath = Files.createTempDirectory(basedir, "OpenVSP3Plugin_");
			tempDir = tempDirPath.toString();
		} else {
			tmp = ".";
			tempDir = ".";
		}
		LOG.info(String.format("initOpenVSP3Plugin() - OpenVSP_EXE = %s (%s), TMP = %s", openVSPExe, openVSPVersion, tempDir));
	}
	
	void makeOrRestoreDialog(boolean loadFile, boolean setVisible) throws Exception {
		LOG.trace(String.format("makeOrRestoreDialog(dialog %s, loadFile %b, setVisible %b) state = %s",
				SwingDialog.argString(dialog), loadFile, setVisible, PluginState.argString(pluginState)));
		if (dialog == null) createSwingDialog();
		dialog.restoreDialog(pluginState, loadFile, setVisible);
	}
	
	protected void createSwingDialog() throws Exception {
		// This must be constructed in the Swing Event Dispatch Thread (EDT)
		SwingDialog.invokeAndWait(() -> {
			dialog = new SwingDialog(TITLE + " - " + componentName, this);
			dialog.initSwingDialog();
		});
		if (openVSPExe.startsWith("ERROR")) dialog.showErrorPopup(openVSPExe);
	}
	
	void configureProcessBuilder(ProcessBuilder pb) {
		pb.directory(new File(tempDir));
		pb.redirectErrorStream(true);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
	}
	
	void runOpenVSPScript(String vspFilename) throws Exception {
		LOG.trace("runOpenVSPScript()");
		ProcessBuilder pb = new ProcessBuilder().command(openVSPExe,  new File(vspFilename).getAbsolutePath(),
						"-script", "OpenVSP3Plugin.vspscript");	
		configureProcessBuilder(pb);
		Process p = pb.start();
		LOG.info(String.format("runOpenVSPScript() - " + pb.command().toString() + " - Exit code %d", p.waitFor()));
	}
	
	boolean shouldSort() {
		if ((dialog != null) && (dialog.controller != null) && (dialog.controller.sortButton != null)) return dialog.controller.sortButton.isSelected();
		return true;
	}
	
	void showOpenVSP() {
		LOG.trace("showOpenVSP()");
		try {
			if (pluginState == null) {
				LOG.warn("showOpenVSP() pluginState is null");
			} else {
				try {
					if (dialog != null) dialog.checkIfStateDirty(pluginState);
					// run OpenVSP to create modified vsp3 file
					writeVSPScriptFile("OpenVSP3Plugin.vspscript");
					pluginState.writeDesFile(new File(tempDir + "\\OpenVSP3Plugin.des"), shouldSort());
					runOpenVSPScript(pluginState.getOpenVSPFilename());
					// run OpenVSP in UI mode and load the modified vsp3 file
					ProcessBuilder pb = new ProcessBuilder().command(openVSPExe, tempDir + "\\OpenVSP3Plugin.vsp3");
					configureProcessBuilder(pb);
					pb.start();
				} catch (Exception ex) {
					LOG.fatal(ex.toString());
				}
			}
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
	}
	
	void writeCompGeomScriptFile(String filename) throws Exception {
		LOG.trace("writeCompGeomScriptFile()");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempDir + "\\" + filename));
		bw.write("void main()"); bw.newLine();
		bw.write("{"); bw.newLine();
		bw.write("  SetComputationFileName(COMP_GEOM_TXT_TYPE, \"./OpenVSP3PluginCompGeom.txt\");"); bw.newLine();
		bw.write("  SetComputationFileName(COMP_GEOM_CSV_TYPE, \"./OpenVSP3PluginCompGeom.csv\");"); bw.newLine();
		bw.write("  ComputeCompGeom(0, false, COMP_GEOM_CSV_TYPE);"); bw.newLine();
		bw.write("  while ( GetNumTotalErrors() > 0 )"); bw.newLine();
		bw.write("  {"); bw.newLine();
		bw.write("    ErrorObj err = PopLastError();"); bw.newLine();
		bw.write("    Print( err.GetErrorString() );"); bw.newLine();
		bw.write("  }"); bw.newLine();
		bw.write("}"); bw.newLine();
		bw.close();
	}
	
	void writeVSPScriptFile(String filename) throws Exception {
		LOG.trace("writeVSPScriptFile()");
		if (pluginState == null) {
			LOG.warn("writeVSPScriptFile() pluginState is null");
		} else {
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempDir + "\\" + filename));
			bw.write("void main()"); bw.newLine();
			bw.write("{"); bw.newLine();
			bw.write("  array<string> meshgeoms;"); bw.newLine();
			for (int i = 0; i < pluginState.getNApplyDes(); i++) {
				bw.write("  ReadApplyDESFile(\"OpenVSP3Plugin.des\");"); bw.newLine();
			}
			bw.write(String.format("  WriteVSPFile(\"%s\", %d);", "OpenVSP3Plugin.vsp3", 0)); bw.newLine();
			ObservableList<DesignVariable> files = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(FILE)));
			for (DesignVariable dv : files) {
				if (dv.getName().equals("vsp3") || dv.getName().equals("des")) {
					// moved above for loop since we need this file to parse outputs
				} else if (dv.getName().equals("DegenGeom")) {
					bw.write("  SetComputationFileName(DEGEN_GEOM_CSV_TYPE, \"" + "OpenVSP3PluginDegenGeom.csv\");"); bw.newLine();
					bw.write(String.format("  ComputeDegenGeom(%d, DEGEN_GEOM_CSV_TYPE);", pluginState.getSetID())); bw.newLine();
				} else {
					if (dv.getName().equals("inc")) continue; // inc is extra file created with pov
					bw.write(String.format("  ExportFile(\"%s\", %d, %s);", ("OpenVSP3Plugin." + dv.getName()), pluginState.getSetID(), EXPORTMAP.get(dv.getName()))); bw.newLine();
					if (MADETEMP.contains(dv.getName())) {
						bw.write("  meshgeoms = FindGeomsWithName(\"MeshGeom\");"); bw.newLine();
						bw.write("  CutGeomToClipboard(meshgeoms[meshgeoms.length - 1]);"); bw.newLine();
					}
				}
			}
			// CompGeom
			if ((pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(COMPGEOM))).size() > 0)
					|| (pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(COMPGEOM2))).size() > 0)) {
				bw.write("  SetComputationFileName(COMP_GEOM_TXT_TYPE, \"" + tempDir.replace("\\", "/") + "/OpenVSP3PluginCompGeom.txt\");"); bw.newLine();
				bw.write("  SetComputationFileName(COMP_GEOM_CSV_TYPE, \"" + tempDir.replace("\\", "/") + "/OpenVSP3PluginCompGeom.csv\");"); bw.newLine();
				bw.write(String.format("  ComputeCompGeom(%d, false, COMP_GEOM_CSV_TYPE);", pluginState.getSetID())); bw.newLine();
				bw.write("  meshgeoms = FindGeomsWithName(\"MeshGeom\");"); bw.newLine();
				bw.write("  CutGeomToClipboard(meshgeoms[meshgeoms.length - 1]);"); bw.newLine();
			}
			// MassProperties
			if (pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(MASSPROP))).size() > 0) {
				bw.write("  SetComputationFileName(MASS_PROP_TXT_TYPE, \"" + tempDir.replace("\\", "/") + "/OpenVSP3PluginMassProp.txt\");"); bw.newLine();
				bw.write(String.format("  ComputeMassProps(%d, 100);", pluginState.getSetID())); bw.newLine();
				bw.write("  meshgeoms = FindGeomsWithName(\"MeshGeom\");"); bw.newLine();
				bw.write("  CutGeomToClipboard(meshgeoms[meshgeoms.length - 1]);"); bw.newLine();
			}
			// CFD Mesh
			ObservableList<DesignVariable> cfdfiles = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(CFDFILE)));
			for (DesignVariable dv : cfdfiles) {
				bw.write(String.format("  SetComputationFileName(%s, \"%s\");", CFDMAP.get(dv.getName()), tempDir.replace("\\", "/") + "/OpenVSP3PluginCFD." + dv.getName())); bw.newLine();
				bw.write(String.format("  ComputeCFDMesh(%d, %s);", pluginState.getSetID(), CFDMAP.get(dv.getName()))); bw.newLine();
			}
			bw.write("  while ( GetNumTotalErrors() > 0 )"); bw.newLine();
			bw.write("  {"); bw.newLine();
			bw.write("    ErrorObj err = PopLastError();"); bw.newLine();
			bw.write("    Print( err.GetErrorString() );"); bw.newLine();
			bw.write("  }"); bw.newLine();
			bw.write("}"); bw.newLine();
			bw.close();
		}
	}
	
	void readCompGeomMaps(Map<String, String> compGeomMap, Map<String, String> tagCompGeomMap) throws Exception {
		LOG.trace("readCompGeomMaps()");
		// Read the CompGeom file and store data in map parameters if not null
		HashMap<String, Integer> nameCount = new HashMap<>();
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(tempDir + "\\OpenVSP3PluginCompGeom.csv"))) {
			boolean firstTable = true;
			br.readLine(); // read headers
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(CSVSPLITSTRING);
				if (columns.length < 3) {
					firstTable = false;
					br.readLine(); // read 2nd headers after blank line
				} else if (firstTable) { // old table
					if (compGeomMap != null) {
						if (columns.length != 5) {
							throw new Exception(String.format("CompGeom table has %d columns not 5/n%s", columns.length, line));
						}
						// Keep names unique by adding a count
						String name = columns[0];
						if (!name.equals("Totals")) {
							if (nameCount.containsKey(name)) nameCount.put(name, nameCount.get(name) + 1);
							else nameCount.put(name, 0);
							name += nameCount.get(name);
					}
						for (int i = 1; i < 5; i++) {
							compGeomMap.put(String.format("%s:%s:%s", COMPGEOM, name, COMPGEOMVALUES[i - 1]), columns[i]);
						}
					}
				} else { // tag table
					if (tagCompGeomMap != null) {
						StringBuilder group = new StringBuilder(columns[0]);
						for (int i = 1; i < columns.length - 2; i++) {
							group.append("_");
							group.append(columns[i]);
						}
						for (int i = 0; i < 2; i++) {
							tagCompGeomMap.put(String.format("%s:%s:%s", COMPGEOM2, group.toString(), COMPGEOMVALUES2[i]), columns[columns.length - 2 + i]);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses the vsp executable to get the version number.
	 */
	private String extractOpenVSPVersion(String path) {
		if (path.equals("Unknown")) return "Unknown";
		int index = 0;
		StringBuilder sb = new StringBuilder();
		char[] text = {'O', 'p', 'e', 'n', 'V', 'S', 'P', ' '};
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
			int i;
			while ((i = is.read()) != -1) {
				char ch = (char) i;
				if (index < 8) {					// find OpenVsp
					if (i == text[index]) index++;
					else index = 0;
				} else if (index < 10) {			// find digits and 2 periods
					if (Character.isDigit(ch)) {
						sb.append(ch);
					} else if (ch == '.') {
						sb.append(ch);
						index++;
					} else {
						index = 0;
					}
				} else if (index == 10) {			// find digits after 2nd period
					if (Character.isDigit(ch)) sb.append(ch);
					else index++;
				} else {							// finishsed
					break;
				}
			}
		} catch (Exception ex) {
			LOG.debug("getOpenVSPVersion() - Failed to find VSP version.");
			return "Unknown";
		}
		return sb.toString();
	}
	
	/**
	 * OpenMDAO methods
	 * loadOpenMDAO() called from OpenMDAO main()
	 * saveOpenMDAO() called from applyPluginState()
	 */
	void loadOpenMDAO(Path path) {
		LOG.trace("loadOpenMDAO()");
		openMDAOStatePath = path;
		try {
			// If the state file exists load it, otherwise just show the plugin
			if (path.toFile().exists()) {
				String content = new String(Files.readAllBytes(path));
				pluginState = PluginState.fromString(content);
				makeOrRestoreDialog(true, true);
			} else {
				makeOrRestoreDialog(false, true);
			}
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
	}

	void saveOpenMDAO() {
		LOG.trace("saveOpenMDAO()");
		try {
			writeVSPScriptFile("OpenVSP3Plugin.vspscript");
			Files.write(openMDAOStatePath, pluginState.toString().getBytes());
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
	}
	
	void exit() {
		System.exit(0);
	}
}
