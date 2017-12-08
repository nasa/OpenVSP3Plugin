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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * This class represents a design variable and is used as the type of the TableView.
 * It has the OpenVSP container, group, variable name,
 * the 11 character OpenVSP ID (ID="UZNTPYGVPBJ"), and the value string.
 * The XPath, state, and checked properties are use by the plugin.
 */
public class DesignVariable {

	final private SimpleStringProperty container;
	final private SimpleStringProperty group;
	final private SimpleStringProperty name;
	final private SimpleStringProperty id;
	final private SimpleStringProperty value;
	final private SimpleStringProperty xpath;
	final private SimpleStringProperty state;
	final private SimpleBooleanProperty checked;
	final private String fullName;
	private String vspValue = null;
	
	public DesignVariable(DesignVariable dv) {
		this.container = new SimpleStringProperty(dv.container.get());
		this.group = new SimpleStringProperty(dv.group.get());
		this.name = new SimpleStringProperty(dv.name.get());
		this.id = new SimpleStringProperty(dv.id.get());
		this.value = new SimpleStringProperty(dv.value.get());
		this.xpath = new SimpleStringProperty(dv.xpath.get());
		this.state = new SimpleStringProperty(dv.state.get());
		this.checked = new SimpleBooleanProperty(dv.checked.get());
		this.fullName = dv.fullName;
		this.vspValue = dv.vspValue;
	}
	
	public DesignVariable(String container, String group, String name, String id, String value) {
		this.container = new SimpleStringProperty(container);
		this.group = new SimpleStringProperty(group);
		this.name = new SimpleStringProperty(name);
		this.id = new SimpleStringProperty(id);
		this.value = new SimpleStringProperty(value);
		this.xpath = new SimpleStringProperty("");
		this.state = new SimpleStringProperty("Input");
		this.checked = new SimpleBooleanProperty(false);
		this.fullName = container + ":" + group + ":" + name;
	}
	
	// The property getters allow automatic table updating.
	public SimpleStringProperty containerProperty() {
		return container;
	}
	
	public SimpleStringProperty groupProperty() {
		return group;
	}
	
	public SimpleStringProperty nameProperty() {
		return name;
	}
	
	public SimpleStringProperty idProperty() {
		return id;
	}
	
	public SimpleStringProperty valueProperty() {
		return value;
	}
	
	public SimpleStringProperty xpathProperty() {
		return xpath;
	}
	
	public SimpleStringProperty stateProperty() {
		return state;
	}
	
	public SimpleBooleanProperty checkedProperty() {
		return checked;
	}
	
	public String getContainer() {
		return container.get();
	}

	public String getGroup() {
		return group.get();
	}

	public String getName() {
		return name.get();
	}
	
	public String getId() {
		return id.get();
	}
	
	public String getValue() {
		return value.get();
	}
	
	public String getVspValue() {
		return vspValue;
	}
	
	public void setVspValue(String value) {
		vspValue = value;
	}
	
	public String getXPath() {
		return xpath.get();
	}
	
	public void setXPath(String newXPath) {
		xpath.set(newXPath);
	}
	
	public boolean isChecked() {
		return checked.get();
	}
	
	public boolean isOutput() {
		return !state.get().equals("Input");
	}
	
	public String getState() {
		return state.get();
	}
	
	public void setState(String state) {
		this.state.set(state);
	}
	
	public void toggleState() {
		switch (getState()) {
			case "Input":
				setState("MCOutput");
				checked.set(true);
				break;
			case "MCOutput":
				setState("Input");
				checked.set(true);
				break;
		}
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public String getModelCenterName(boolean flatNames, boolean addID, boolean groupOutputs) {
		String mcname;
		String sep = ".";
		String pre = "";
		if (groupOutputs) {
			if (isOutput()) pre = "Output" + sep;
			else pre = "Input" + sep;
		}
		if (flatNames) sep = "_";
		if (getContainer().equals(getGroup())) {
			mcname = pre + getContainer() + sep + getName();
		} else {
			mcname = pre + getContainer() + sep + getGroup() + sep + getName();
		}
		if (addID) mcname += "_" + getId();
		return mcname;
	}
	
	public String toDesString() {
		return String.format("%s:%s: %s", getId(), getFullName(), getValue());
	}
	
	public String toDesString(String newValue) {
		return String.format("%s:%s: %s", getId(), getFullName(), newValue);
	}

	public String toXDDMString() {
		return String.format("  <Variable ID=\"%s\" Value=\"%s\" Min=\"0.0\" Max=\"1.0\" VSPID=\"%s\"/>",
				getFullName(), getValue(), getId());
	}
	
	public String toStateString() {
		return String.format("  <Variable ID=\"%s\" Value=\"%s\" VSPID=\"%s\" STATE=\"%s\" XPATH=\"%s\"/>",
				getFullName(), getValue(), getId(), getState(), getXPath());
	}
	
	public String toFullString() {
		return getName() + " " + getValue() + " " + isChecked();
	}
	
	@Override
	public String toString() {
		return getName() + " " + getValue();
	}
}
