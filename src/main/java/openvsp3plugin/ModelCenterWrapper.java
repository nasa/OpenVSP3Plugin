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
import com.phoenix_int.ModelCenter.Component;
import com.phoenix_int.ModelCenter.DoubleVariable;
import com.phoenix_int.ModelCenter.FileVariable;
import com.phoenix_int.ModelCenter.ModelCenter;
import com.phoenix_int.ModelCenter.ModelCenterException;
import java.util.Map;
import javafx.collections.ObservableList;
import static openvsp3plugin.OpenVSP3Plugin.CFDFILE;

/**
 * Wraps the ModelCenter IO so ModelCenterPlugin and be tested with a mockup wrapper.
 */
class ModelCenterWrapper {
	
	private static final Logger LOG = new Logger(ModelCenterWrapper.class.getSimpleName());
	
	private ModelCenter modelCenter;
	private AddToModel addToModel;
	protected OpenVSP3Plugin plugin;
	
	protected ModelCenterWrapper(OpenVSP3Plugin plugin) {
		this.plugin  = plugin;
	}
	
	ModelCenterWrapper(ModelCenter modelCenter, AddToModel addToModel, OpenVSP3Plugin plugin) throws Exception {
		this(plugin);
		if (modelCenter == null) throw new Exception("addToModel is null");
		this.modelCenter = modelCenter;
		this.addToModel = addToModel;
	}
	
	String getComponentName() throws Exception {
		LOG.trace("getComponentName()");
		return addToModel.getComponent().getFullName();
	}
	
	void onEnd() throws Exception {
		LOG.trace("onEnd()");
		if (modelCenter != null) modelCenter.release();
		if (addToModel != null) addToModel.release();
	}
	
	void addInput(String name, String type, String value) throws Exception {
		LOG.trace("addInput()");
		addToModel.addInput(name, type, value).release();
	}
	
	void addOutput(String name, String type, String value) throws Exception {
		LOG.trace("addOutput()");
		addToModel.addOutput(name, type, value).release();
	}
	
	void updateComponent() throws Exception {
		LOG.trace("updateComponent()");
		addToModel.updateComponent();
	}
	
	void updateDVFromMCvalues(ObservableList<DesignVariable> dvList, PluginState pluginState) throws Exception {
		LOG.trace("updateDVFromMCvalues()");
		Component component = addToModel.getComponent();
		DoubleVariable mcVariable = null;
		for (DesignVariable dv : dvList) {
			try {
				mcVariable = (DoubleVariable) component.getVariable(pluginState.getModelCenterName(dv));
				dv.valueProperty().set(String.format("%f", mcVariable.getValue()));
			} catch (ModelCenterException ex) {
				LOG.debug(ex.toString());
				if (mcVariable == null) throw ex;
				dv.valueProperty().set("INVALID");
				mcVariable.release();
			}
			
		}
		component.release();
	}
	
	void readFiles(ObservableList<DesignVariable> dvList, PluginState pluginState, String tempDir) throws Exception {
		LOG.trace("readFiles()");
		Component component = addToModel.getComponent();
		for (DesignVariable dv : dvList) {
			FileVariable file = (FileVariable) component.getVariable(pluginState.getModelCenterName(dv));
			String filename = tempDir + "\\OpenVSP3Plugin." + dv.getName();
			if (dv.getId().equals(CFDFILE))  filename = tempDir + "\\OpenVSP3PluginCFD." + dv.getName();
			if (dv.getName().equals("DegenGeom")) filename = tempDir + "\\OpenVSP3PluginDegenGeom.csv";
			file.readFile(filename);
			file.release();
		}
		component.release();
	}
	
	void updateMCValuesFromMap(ObservableList<DesignVariable> dvList, Map<String, String> map, PluginState pluginState) throws Exception {
		LOG.trace("updateMCValuesFromMap()");
		Component component = addToModel.getComponent();
		for (DesignVariable dv : dvList) {
			DoubleVariable mcVariable = (DoubleVariable) component.getVariable(pluginState.getModelCenterName(dv));
			if (map.containsKey(dv.getFullName())) {
				mcVariable.setValue(Double.parseDouble(map.get(dv.getFullName())));
			} else {
				throw new Exception("updateMCValuesFromMap() - Couldn't find " + dv.getFullName() + " in map data.");
			}
			mcVariable.release();
		}
	}
	
	void updateMCValuesFromDV(ObservableList<DesignVariable> dvList, PluginState pluginState, XPathUtil xpu) throws Exception {
		LOG.trace("updateMCValuesFromDV()");
		Component component = addToModel.getComponent();
		for (DesignVariable dv : dvList) {
			DoubleVariable mcVariable = (DoubleVariable) component.getVariable(pluginState.getModelCenterName(dv));
			String attribute = xpu.getElementAttribute(dv.getXPath(), "Value", "");
			double  value = Double.parseDouble(attribute);
			dv.valueProperty().set(String.format("%f", value));
			mcVariable.setValue(value);
			mcVariable.release();
		}
		component.release();
	}
	
	void resetMCValuesToOrigVSP(ObservableList<DesignVariable> dvList, PluginState pluginState) throws Exception {
		LOG.trace("resetMCValuesToOrigVSP()");
		Component component = addToModel.getComponent();
		for (DesignVariable dv : dvList) {
			String vspValue = dv.getVspValue();
			if (vspValue != null) {
				dv.valueProperty().set(vspValue);
				LOG.debug(String.format("Setting %s to %s", pluginState.getModelCenterName(dv), vspValue));
				DoubleVariable mcVariable = (DoubleVariable) component.getVariable(pluginState.getModelCenterName(dv));
				mcVariable.setValue(Double.parseDouble(vspValue));
				mcVariable.release();
			}
		}
		component.release();
	}
}
