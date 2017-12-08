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

import com.phoenix_int.ModelCenter.AddToModel;
import com.phoenix_int.ModelCenter.ModelCenter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.collections.ObservableList;

/**
 * This class extends OpenVSP3Plugin and contains all the references to ModelCenter and log4j.
 * 
 */
public class ModelCenterPlugin extends OpenVSP3Plugin implements com.phoenix_int.ModelCenter.IComponentPlugIn {
	private static final org.apache.log4j.Logger LOG4J = org.apache.log4j.Logger.getLogger("OpenVSP3Plugin");
	private static final Logger LOG = new Logger(ModelCenterPlugin.class.getSimpleName());

	// HashSet wasn't working see note in onEnd()
	protected static final Map<String, Integer> DIALOGS = new LinkedHashMap<>();
	
	protected ModelCenterWrapper mcWrapper;
	
	public ModelCenterPlugin() {
		super();
		// Can set login level here instead of log4j.properties if testing (must be called first)
//		Logger.initLogLevel("T");
		// allow using log4j level until pluginState is created
		org.apache.log4j.Level level = LOG4J.getLevel(); // Level can be null
		if (level != null) Logger.initLogLevel(LOG4J.getLevel().toString());

	}
	
	// <editor-fold desc="These methods are the ModelCenter interface and are always called on the ModelCenter thread"> 
	
	@Override
	public void construct(ModelCenter modelCenter, AddToModel addToModel) throws Exception {
		LOG.trace("construct()");
		mcWrapper = createModelCenterWrapper(modelCenter, addToModel);
		componentName = mcWrapper.getComponentName();
		DIALOGS.computeIfAbsent(componentName, (n) -> 0);
		DIALOGS.put(componentName, DIALOGS.get(componentName) + 1);
		LOG.debug("construct() " + componentName + "count = " + DIALOGS.get(componentName));
		initOpenVSP3Plugin(true);
	}
	
	protected ModelCenterWrapper createModelCenterWrapper(ModelCenter modelCenter, AddToModel addToModel) throws Exception {
		return new ModelCenterWrapper(modelCenter, addToModel, this);
	}
	
	@Override
	public boolean show() throws Exception {
		LOG.trace("show()");
		if (dialog != null) updatePluginState();
		makeOrRestoreDialog(false, true);
		return false;
	}
	
	@Override
	public String toString() {
		LOG.debug("ModelCenterPlugin.toString() state string:\n" + pluginState.toString());
		return pluginState.toString();
	}
	
	@Override
	public void fromString(String string) throws Exception {
		LOG.debug("fromString() state string:\n" + string);
		pluginState = PluginState.fromString(string);
		if (dialog == null) {
			if (DIALOGS.get(componentName) == 1) {
				// apply the logging level as soon as possible
				Logger.initLogLevel(pluginState.getLogLevel());
				LOG.info("Making dialog for " + componentName);
				makeOrRestoreDialog(true, false);
			} else {
				LOG.info("Skipping dialog for " + componentName);
			}
		} else {
			// can this ever happen?
			throw new Exception("fromString called with dialog != null");
		}
	}
	
	@Override
	public void run() throws Exception {
		LOG.trace("run()");
		if (pluginState == null) {
			throw new Exception("Plugin state is null\nOpen UI and load file.");
		} else {
			updatePluginState();
			if (dialog != null) {
				if (dialog.checkIfLoadingFile()) throw new Exception("\n\nCan't run until OpenVSP file has successfully loaded.\n");
				dialog.checkIfStateDirty(pluginState);
			}
			writeVSPScriptFile("OpenVSP3Plugin.vspscript");
			pluginState.writeDesFile(new File(tempDir + "\\OpenVSP3Plugin.des"), shouldSort());
			runOpenVSPScript(pluginState.getOpenVSPFilename());
			readExportFiles();
			readCompGeom();
			readMassProp();
			readOutputs();
			// now update the UI
			if ((dialog != null) && (dialog.isVisible() == true)) {
				LOG.debug("run() updating visible UI");
				dialog.restoreDialog(pluginState, false, false);
			}
		}
	}
	
	@Override
	public void onEnd() throws Exception {
		LOG.trace("onEnd()");
		// DIALOGS.clear was added so that closing the model and reopening it would not skip the dialog.
		// However it was clearing ALL data when ANY plugin called onEnd() like optimization finished.
		// Changing to a map counter so increment in construct() and decrement in onEnd() and skip if not 1 in fromString().
		DIALOGS.put(componentName, DIALOGS.get(componentName) - 1);
		LOG.debug("onEnd() " + componentName + "count = " + DIALOGS.get(componentName));
		mcWrapper.onEnd();
		if (pluginState != null) {
			LOG.debug("onEnd() deleting " + tempDir);
			deleteDirectoryContents(new File(tempDir));
		}
		if (dialog != null) {
			LOG.debug("onEnd() dialog.dispose()");
			dialog.dispose();
		}
	}
	
	// </editor-fold>
	
	/**
	 * Overriding applyPluginState()
	 * This version actually updates the ModelCenter variables.
	 */
	@Override
	void applyPluginState() {
		LOG.trace("applyPluginState() ModelCenter");
		try {
			// This is called on the JavaFX Application thread which can't access mcWrapper.addToModel but the Swing thread can
			SwingDialog.invokeAndWait(() -> {
				LOG.trace("applyPluginState() invokeAndWait()");
				try {
					for (DesignVariable dv : pluginState.getDesignVariables().filtered(dv -> !dv.isOutput())) {
						mcWrapper.addInput(pluginState.getModelCenterName(dv), "double", dv.getValue());
					}
					for (DesignVariable dv : pluginState.getDesignVariables().filtered(dv -> dv.isOutput())) {
						if (dv.getId().equals("File")) {
							mcWrapper.addOutput(pluginState.getModelCenterName(dv), "file", "");
						} else {
							mcWrapper.addOutput(pluginState.getModelCenterName(dv), "double", dv.getValue());
						}
					}
					mcWrapper.updateComponent();
				} catch (Exception ex) {
					LOG.fatal(ex.toString());
				}
				LOG.debug(String.format("applyPluginState() - %d Design Variables Selected", pluginState.getDesignVariables().size()));
			});
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
	}
	
	/**
	 * This resets the ModelCenter variables to the original VSP values.
	 */
	@Override
	void resetToOrig() {
		try {
			// This is called on the JavaFX Application thread which can't access mcWrapper.addToModel but the Swing thread can
			SwingDialog.invokeAndWait(() -> {
				try {
					mcWrapper.resetMCValuesToOrigVSP(pluginState.getDesignVariables().filtered(dv -> !dv.isOutput()), pluginState);
				} catch (Exception ex) {
					LOG.fatal(ex.toString());
				}
			});
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
	}
	
	private void updatePluginState() throws Exception {
		LOG.trace("updatePluginState() ModelCenter");
		// if there is no pluginState there is nothing to update
		if (pluginState == null) return;
		// Update the input variables with the ModelCenter values
		ObservableList<DesignVariable> inputs = pluginState.getDesignVariables().filtered(dv -> !dv.isOutput());
		mcWrapper.updateDVFromMCvalues(inputs, pluginState);
	}
	
	private void readExportFiles() throws Exception {
		LOG.trace("readExportFiles()");
		ObservableList<DesignVariable> files = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(FILE)));
		mcWrapper.readFiles(files, pluginState, tempDir);
	}
	
	private void readCompGeom() throws Exception {
		LOG.trace("readCompGeom()");
		ObservableList<DesignVariable> compGeoms = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(COMPGEOM)));
		ObservableList<DesignVariable> tagCompGeoms = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(COMPGEOM2)));
		if ((compGeoms.size() > 0) || (tagCompGeoms.size() > 0)) {
			// Read the CompGeom file
			Map<String, String> compGeomMap = new LinkedHashMap<>();
			Map<String, String> tagCompGeomMap = new LinkedHashMap<>();
			readCompGeomMaps(compGeomMap, tagCompGeomMap);
			// Update the Model Center variables
			// CompGeom
			mcWrapper.updateMCValuesFromMap(compGeoms, compGeomMap, pluginState);
			// TagCompGeom
			mcWrapper.updateMCValuesFromMap(tagCompGeoms, tagCompGeomMap, pluginState);
		}	
	}
	
	private void readMassProp() throws Exception {
		LOG.trace("readMassProp()");
		ObservableList<DesignVariable> massProps = pluginState.getDesignVariables().filtered(dv -> (dv.getId().equals(MASSPROP)));
		if (massProps.size() > 0) {
			// Read the MassProp file
			String line;
			Map<String, String> map = new LinkedHashMap<>();
			BufferedReader br = new BufferedReader(new FileReader(tempDir.replace("\\", "/") + "/OpenVSP3PluginMassProp.txt"));
			boolean foundTable = false;
			String lastName = "";
			int counter = 0;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Name")){
					foundTable = true;
					continue;
				}
				if (foundTable) {
					String[] columns = line.split(CSVSPLITSTRING);
					if (columns.length > MASSPROPVALUES.length) {
						if (!lastName.equals(columns[0])) {
							counter = 0;
							lastName = columns[0];
						} else {
							counter++;
						}
						String group = columns[0].equals("Totals") ? columns[0] : columns[0] + counter;
						for (int i = 0; i < MASSPROPVALUES.length; i++) {
							map.put(String.format("%s:%s:%s", MASSPROP, group, MASSPROPVALUES[i]), columns[i + 1]);
						}
					}
				}
			}
			// Update the Model Center variables
			mcWrapper.updateMCValuesFromMap(massProps, map, pluginState);
			br.close();
		}
	}
	
	private void readOutputs() throws Exception {
		LOG.trace("readOutputs()");
		LOG.debug("readOutputs() " + pluginState.getDesignVariables().size() + " Design variables");
		LOG.debug("readOutputs() " + pluginState.getDesignVariables().filtered(dv -> (dv.isOutput())).size() + " outputs");
		ObservableList<DesignVariable> outputs = pluginState.getDesignVariables().filtered(dv -> (dv.isOutput() && !dv.getXPath().isEmpty()));
		LOG.debug("readOutputs() " + outputs.size() + " with XPath defined");
		XPathUtil xpu = new XPathUtil(new File(tempDir + "/OpenVSP3Plugin.vsp3"));
		// Update the Model Center variables
		mcWrapper.updateMCValuesFromDV(outputs, pluginState, xpu);
		// check that inputs were applied if dialog exists and epsilon defined
		if ((dialog != null) && (pluginState.getEpsilon() != null)) {
			LOG.debug("readOutputs() " + pluginState.getDesignVariables().filtered(dv -> (!dv.isOutput())).size() + " inputs");
			ObservableList<DesignVariable> inputs = pluginState.getDesignVariables().filtered(dv -> (!dv.isOutput() && !dv.getXPath().isEmpty()));
			LOG.debug("readOutputs() " + inputs.size() + " with XPath defined");
			boolean ignoreWarnings = false;
			String question = "\n\nIgnore all other warnings?\n";
			for (DesignVariable dv : inputs) {
				String desValueString = dv.getValue();
				String vspValueString = xpu.getElementAttribute(dv.getXPath(), "Value", "");
				double desValue = Double.parseDouble(desValueString);
				double vspValue = Double.parseDouble(vspValueString);
				if (Math.abs(vspValue - desValue) > Math.abs(pluginState.getEpsilon())) {
					// Always log popups can be ignored
					LOG.warn(dv.getFullName() + " does not match, des = " + desValueString + ", vsp3 = " + vspValueString);
					String message = String.format("Design variable %s not applied.\nOpenVSP3Plugin.des = %s\nOpenVSP3Plugin.vsp3 = %s", dv.getFullName(), desValueString, vspValueString);
					if (!ignoreWarnings) {
						ignoreWarnings = dialog.showPopup(message + question);
					}
				}
			}
		}
	}
	
	private void deleteDirectoryContents(File path) {
		LOG.trace("deleteDirectoryContents() " + path);
		// only needed when ModelCenterDummy quit button pressed
		if (path.exists()) {
			File[] files = path.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectoryContents(file);
				} else {
					file.delete();
				}
			}
			path.delete();
		}
	}
}
