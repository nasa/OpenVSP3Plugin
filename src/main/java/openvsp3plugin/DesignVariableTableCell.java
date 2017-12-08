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

import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

/**
 * Special TableCell to implement the Input/Output coloring in the table
 */
public class DesignVariableTableCell extends TableCell<DesignVariable, String> {
	
	@Override
	@SuppressWarnings("unchecked") 
	protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty) {
			TableView<DesignVariable> tv = getTableView();
			TableRow<DesignVariable> tr = getTableRow();
			if ((tv != null) && (tr != null))  {
				if (tv.getItems().get(getTableRow().getIndex()).isOutput()) {
					setTextFill(Color.BLUE);
				} else if (tv.getItems().get(getTableRow().getIndex()).getValue().equals("INVALID")) {
					setTextFill(Color.ORANGE);
				} else {
					setTextFill(Color.GREEN);
				}
			} else {
				setTextFill(Color.YELLOW);
			}
			setText(item);
		} else {
			setText("");
		}
	}
}
