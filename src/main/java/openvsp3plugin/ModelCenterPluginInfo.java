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

/**
 * This class provides definitions to be used by the ModelCenter UI.
 */
public class ModelCenterPluginInfo implements com.phoenix_int.ModelCenter.IPlugInMetaData {

	@Override
	public String getDescription() throws Exception {
		return "Plugin for OpenVSP version 3 (breaking file format)";
	}

	@Override
	public String getAuthor() throws Exception {
		return "Jim Fenbert";
	}

	@Override
	public String getVersion() throws Exception {
		return OpenVSP3Plugin.VERSION;
	}

	@Override
	public String getHelpURL() throws Exception {
		return "";
	}

	@Override
	public String getKeywords() throws Exception {
		return "OpenVSP";
	}

	@Override
	public String getIconLocation() throws Exception {
		return "";
	}
	
}
