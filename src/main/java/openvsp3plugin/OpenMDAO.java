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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main class for running from the command line (OpenMDAO mode).
 * This creates an instance of the OpenVSP3Plugin without the ModelCenter wrapper.
 * It looks for an input file argument or the default which is "State.xml" and loads it.
 * It updates the logging level, default is INFO if flag argument is passed in.
 */
public class OpenMDAO {
	
	// runs in OpenMDAO mode
	public static void main(String[] args) {
		try {
			// Create OpenVSP3Plugin
			OpenVSP3Plugin plugin = new OpenVSP3Plugin();
			Path state = Paths.get("State.xml");
			for (String arg : args) {
				if (arg.startsWith("-")) {
					if (arg.length() > 1) {
						Logger.setLogLevel(arg.substring(1));
					}
				} else {
					state = Paths.get(arg);
				}
			}
			// call initOpenVSP3Plugin after logging level has been set
			plugin.initOpenVSP3Plugin(false);
			// create the UI and load the state file if it exists
			plugin.loadOpenMDAO(state);
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}
}
