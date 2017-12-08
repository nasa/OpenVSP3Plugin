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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.Callback;

/**
 * This class contains the JavaFX code.
 * All calls to Platform.runLater() are made in this class
 */
class JavaFXUI {
	
	private static final Logger LOG = new Logger(JavaFXUI.class.getSimpleName());
	
	private BorderPane root;
	@FXML private Button applyButton;
	@FXML private Button revertButton;
	@FXML private Button showVSPButton;
	@FXML public Button resetMCtoVSPButton;
	@FXML public ToggleButton sortButton;
	@FXML private ToggleButton flatNamesButton;
	@FXML private ToggleButton addIDButton;
	@FXML private ToggleButton groupOutputsButton;
	@FXML private Label warningLabel;
	@FXML private Label openVSPFileLabel;
	@FXML private TextField openVSPFileTextField;
	@FXML private Label logLevelLabel;
	@FXML private ChoiceBox<Logger.LogLevel> logChoiceBox;
	@FXML private Label label;
	@FXML private Label epsilonLabel;
	@FXML private TextField epsilonTextField;
	@FXML private Label filterLabel;
	@FXML private TextField filterTextField;
	@FXML private ToggleButton selectedOnlyButton;
	@FXML private TreeView<DesignVariableGroup> treeView;
	@FXML private TableView<DesignVariable> tableView;
	@FXML private ChoiceBox<String> setChoiceBox;
	@FXML private ChoiceBox<String> nApplyDesChoiceBox;
	@FXML private Label vspPathLabel;
	@FXML private Label vspVersionLabel;
	@FXML private Button okButton;
	@FXML private Button cancelButton;

	protected OpenVSP3File openVSP3File;
	private ObservableList<DesignVariable> designVariableList;
	private ObservableList<DesignVariable> tableViewData;
	private TreeItem<DesignVariableGroup> rootItem;
	private boolean ignoreUpdate = false;
	protected OpenVSP3Plugin plugin;
	private SwingDialog dialog;
	private String lastLoadedFile = "";
	private Double epsilon = null;
	private FileChooser fileChooser = new FileChooser();
	private boolean loadingFile = false;
	private Pattern emptyPattern = Pattern.compile("");
	
	/**
	 * From http://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/ 
	 * Runs the specified Runnable on the
	 * JavaFX application thread and waits for completion.
	 *
	 * @param action the Runnable to run
	 * @throws NullPointerException if action is null
	 */
	static void PlatformRunAndWait(Runnable action) {
		if (action == null)
			throw new NullPointerException("action");
		
		// run synchronously on JavaFX thread
		if (Platform.isFxApplicationThread()) {
			action.run();
			return;
		}

		// queue on JavaFX thread and wait for completion
		final CountDownLatch doneLatch = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				action.run();
			} finally {
				doneLatch.countDown();
			}
		});

		try {
			doneLatch.await();
		} catch (InterruptedException e) {
			// ignore exception
		}
	}
	
	void initJavaFXUI(OpenVSP3Plugin plugin, SwingDialog dialog, JFXPanel fxContainer) {
		LOG.trace(String.format("initJavaFXUI(%s, %s, fxContainer)",
				plugin.componentName, SwingDialog.argString(dialog)));
		this.plugin = plugin;
		this.dialog = dialog;
		// Don't let JavaFX thread exit if MC model closes. (all Stages deleted)
		Platform.setImplicitExit(false);
		rootItem = new TreeItem<>(new DesignVariableGroup("Design Variables (All)"));
		try {
			FXMLLoader loader = new FXMLLoader(JavaFXUI.class.getResource("JavaFXUI.fxml"));
			loader.setController(this);
			root = loader.load();
			if (plugin.isOpenMDAO()) {
				applyButton.setText("Save");
				revertButton.setText("Reload");
				resetMCtoVSPButton.setDisable(true);
				flatNamesButton.setDisable(true);
				addIDButton.setDisable(true);
				groupOutputsButton.setDisable(true);
				warningLabel.setDisable(true);
				epsilonLabel.setDisable(true);
				epsilonTextField.setDisable(true);
			} 
			openVSPFileTextField.setOnAction((ActionEvent e) -> {switchFile(openVSPFileTextField.getText());});
			vspPathLabel.setText(plugin.getOpenVSPExe());
			vspVersionLabel.setText(plugin.getOpenVSPVersion());
			logChoiceBox.getItems().addAll(Logger.LogLevel.values());
			logChoiceBox.setValue(Logger.LogLevel.OFF);
//			setOnAction added 8u60 so can't use it in MC11. Using a listener on valueProperty instead
//			logChoiceBox.setOnAction((ActionEvent e) -> {Logger.loggingLevel.setValue(logChoiceBox.getValue());});
			logChoiceBox.valueProperty().addListener((ObservableValue<? extends Logger.LogLevel> observable, Logger.LogLevel oldValue, Logger.LogLevel newValue) -> {
				if (newValue != null) Logger.loggingLevel.setValue(newValue);
			});
		} catch (Exception ex) {
			dialog.showErrorPopup("Failed to load FXML file.\n" + ex.toString());
		}
		addFontSizeMenu(logLevelLabel, root);
		addFontSizeMenu(openVSPFileLabel, openVSPFileTextField);
		addFontSizeMenu(label);
		addFontSizeMenu(filterLabel, filterTextField);
		addFontSizeMenu(treeView);
		addFontSizeMenu(tableView);
		initialize();
		Scene scene = new Scene(root);
		fxContainer.setScene(scene);
	}
	
	void addFontSizeMenu(Control control) {
		addFontSizeMenu(control, control);
	}
	
	void addFontSizeMenu(Control menuControl, Node fontNode) {
		ContextMenu menu = new ContextMenu();
		buildFontSizeMenu(menuControl, fontNode, menu.getItems());
		menuControl.setContextMenu(menu);
	}
	
	void buildFontSizeMenu(Control menuObject, Node fontNode, ObservableList<MenuItem> items) {
		ToggleGroup toggleGroup = new ToggleGroup();
		MenuItem titleItem = new MenuItem("Font Size Reset");
		titleItem.setOnAction((ActionEvent e) -> {
			if (menuObject instanceof Control) menuObject.setStyle("");
			fontNode.setStyle("");
			Toggle selectedToggle = toggleGroup.getSelectedToggle();
			if (selectedToggle != null) selectedToggle.setSelected(false);
		});
		items.add(titleItem); 
		SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
		items.add(separatorMenuItem);
		for (int i = 10; i <= 30; i += 2) {
			Integer j = i;
			RadioMenuItem item = new RadioMenuItem(j.toString() + "pt");
			item.setToggleGroup(toggleGroup);
			item.setOnAction((ActionEvent e) -> {
				if (menuObject instanceof Control) menuObject.setStyle(String.format("-fx-font-size: %dpt", j));
				fontNode.setStyle(String.format("-fx-font-size: %dpt", j));
			});
			items.add(item);
		}
	}
	
	@FXML
	void applyAction(ActionEvent event) {
		LOG.trace("applyAction()");
		if (openVSP3File != null) {
			if (!lastLoadedFile.equals(openVSPFileTextField.getText())) {
				String message = String.format("OpenVSP file has changed from:\n%s\nto:\n%s.\nWould you like to load the new file?",
						lastLoadedFile, openVSPFileTextField.getText());
				if (dialog.showPopup(message)) {
					switchFile(openVSPFileTextField.getText());
				} else {
					openVSPFileTextField.setText(lastLoadedFile);
				}
			}
			plugin.setPluginState(getPluginState());
		} else {
			dialog.showErrorPopup("Can't " + applyButton.getText() + " because no file has been loaded.");
		}
	}
	
	@FXML
	void revertAction(ActionEvent event) {
		LOG.trace("revertAction()");
		PluginState state = plugin.getPluginState();
		if (state != null) {
			checkFile(state.getOpenVSPFilename(), state, true);
		} else {
			dialog.showErrorPopup("Can't " + revertButton.getText() + " because no state is available.");
		}
	}
	
	@FXML
	void okAction(ActionEvent event) {
		LOG.trace("okAction()");
		applyAction(null);
		dialog.closeDialog(false);
	}
	
	@FXML
	void cancelAction(ActionEvent event) {
		LOG.trace("cancelAction()");
		if (!plugin.isOpenMDAO()) revertAction(null);
		dialog.closeDialog(false);
	}
	
	@FXML
	void showOpenVSPAction(ActionEvent event) {
		LOG.trace("showOpenVSPAction()");
		try {
			checkIfStateDirty(plugin.getPluginState());
			plugin.showOpenVSP();
		} catch (Exception ex) {
			dialog.showErrorPopup(ex);
		}
	}
	
	@FXML
	void resetMCtoVSPAction(ActionEvent event) {
		LOG.trace("resetMCtoVSP()");
		try {
			checkIfStateDirty(plugin.getPluginState());
			// update MC and state design variables
			plugin.resetToOrig();
			// update OpenVSPFile (table) design variables
			designVariableList.filtered(dv -> dv.isChecked()).stream().forEach((dv) -> {
				dv.valueProperty().set(dv.getVspValue());
			});
		} catch (Exception ex) {
			dialog.showErrorPopup(ex);
		}
	}
	
	@FXML
	void loadVSPFileAction(ActionEvent event) {
		LOG.trace("loadVSPFileAction()");
		File selectedFile = selectFile();
		if (selectedFile != null) {
			String path = selectedFile.getAbsolutePath();
			String cwd = System.getProperty("user.dir");
			if (path.startsWith(cwd)) path = path.substring(cwd.length()+1);
			if ((plugin == null) || (plugin.getPluginState() == null) || plugin.getPluginState().getDesignVariables().isEmpty()) {
				checkFile(path, null, false);
			} else {
				// alternate load method saves state
				switchFile(path);
			}
		}
	}
	
	@FXML
	void updateFilterAction(ActionEvent event) {
		LOG.trace("updateFilterAction()");
		updateFilters();
	}
	
	@FXML
	void clearFilterAction(ActionEvent event) {
		LOG.trace("clearFilterAction()");
		filterTextField.clear();
	}
	
	@FXML
	void selectAllAction(ActionEvent event) {
		LOG.trace("selectAllAction()");
		ObservableList<DesignVariable> list;
		list = tableView.getItems();
		// update the label after all the changes are made
		ignoreUpdate = true;
		list.forEach((DesignVariable dv) -> dv.checkedProperty().set(true));
		ignoreUpdate = false;
		updateLabel();
	}
	
	@FXML
	void unselectAllAction(ActionEvent event) {
		LOG.trace("unselectAllAction()");
		ObservableList<DesignVariable> list;
		list = tableView.getItems();
		// update the label after all the changes are made
		ignoreUpdate = true;
		list.forEach((DesignVariable dv) -> dv.checkedProperty().set(false));
		ignoreUpdate = false;
		updateLabel();
	}
	
	@FXML
	void setEpsilonAction(ActionEvent event) {
		epsilon = null;
		try {
			epsilon = Double.parseDouble(epsilonTextField.getText());
		} catch (Exception ex) {}
		epsilonTextField.setText((epsilon == null) ? "" : epsilon.toString());
	}
	
	@FXML
	void showAbout(ActionEvent event) {
		dialog.showAboutDialog();
	}
	
	/**
	 * This is called by the dialog to update the UI
	 * The UI needs to be updated by the JavaFXApplication thread
	 * so PlatformRunAndWait is used.
	 */
	void restoreUI(PluginState state, boolean loadFile) {
		LOG.trace(String.format("restoreUI(state %s, %b)", PluginState.argString(state), loadFile));
		PlatformRunAndWait(() -> {
			if ((state == null) || (state.getOpenVSPFilename().isEmpty())) {
				LOG.debug("Start restoreUI()->PlatformRunAndWait()->loadVSPFileAction(null)");
				loadVSPFileAction(null);
			} else if (loadFile) {
				LOG.debug(String.format("Start restoreUI()->PlatformRunAndWait()->checkFile(%s, %s, true)",
						state.getOpenVSPFilename(), PluginState.argString(state)));
				checkFile(state.getOpenVSPFilename(), state, true);
			} else {
				LOG.debug(String.format("Start restoreUI()->PlatformRunAndWait()->restoreState(%s, false",
						PluginState.argString(state)));
				restoreState(state, false);
			}
			LOG.debug("Finish restoreUI()->PlatformRunAndWait()");
		});
	}
	/**
	 * This doesn't get called in the FXApplication thread, so don't use the label to check
	 */
	boolean checkIfLoadingFile() {
		return loadingFile;
	}
	
	/**
	 * This is called by the dialog to check if the UI had changed since the
	 * plugin state was set.
	 */
	void checkIfStateDirty(PluginState state) throws Exception {
		LOG.trace(String.format("checkIfStateDirty(%s)", PluginState.argString(state)));
		if (state == null) throw new Exception("Plugin state is null\nOpen UI and load file, Apply or Save.");
		PluginState currentState = getPluginState();
		PluginState.CompareStatus status = state.compareTo(currentState);
		if (status == PluginState.CompareStatus.DIFFERENT) {
			StringBuilder sb = new StringBuilder("The plugin state is not up to date.\n");
			sb.append(state.getCompareString(currentState));
			sb.append("Apply changes?");
			if (dialog.showPopup(sb.toString())) {
				applyAction(null);
			}
		}
	}
	
	private PluginState getPluginState() {
		LOG.trace("getPluginState()");
		return new PluginState(openVSPFileTextField.getText(),
			designVariableList.filtered(dv -> dv.isChecked()),
			flatNamesButton.isSelected(), addIDButton.isSelected(), groupOutputsButton.isSelected(), epsilon,
			setChoiceBox.getSelectionModel().getSelectedIndex(), nApplyDesChoiceBox.getSelectionModel().getSelectedIndex() + 1,
			OpenVSP3Plugin.VERSION, plugin.getOpenVSPVersion(), Logger.loggingLevel.getValue().toString());
	}
	
	private void switchFile(String filename) {
		LOG.debug("switchFile(" + filename + ")");
		try {
			PluginState state = null;
			if ((plugin != null) && (plugin.getPluginState() != null)) {
				state = PluginState.fromString(plugin.getPluginState().toString());
				state.setOpenVSPFilename(filename);
			}
			checkFile(filename, state, true);
		} catch (Exception ex) {
			dialog.showErrorPopup(ex);
		}
	}
	
	private void checkFile(String path, PluginState state, boolean loadFile) {
		LOG.trace(String.format("checkFile(%s, %s, %b)", path, PluginState.argString(state), loadFile));
		// check for existance before anything gets deleted.
		if (!loadFile(path, state, loadFile)) {	
			String message = String.format("Could not find the OpenVSP file:\n%s.\nWould you like to browse to it?", path);
			if (dialog.showPopup(message)) {
				File selectedFile = findFile(path);
				if (selectedFile != null) {
					checkFile(selectedFile.getAbsolutePath(), state, loadFile);
					return;
				}
			}
			openVSPFileTextField.setText(lastLoadedFile);
		}
	}
	
	private boolean loadFile(String path, PluginState state, boolean loadFile) {
		LOG.trace(String.format("loadFile(%s, %s, %b)", path, PluginState.argString(state), loadFile));
		boolean dialogIsVisible = dialog.isVisible();
		File file = new File(path);
		if (!file.exists()) return false;
		openVSP3File = new OpenVSP3File(designVariableList, addIDButton.isSelected());
		ignoreUpdate = true;
		loadingFile = true;
		label.setText("Loading File...");
		lockUI();
		if (dialogIsVisible) {
			Thread th = new Thread(new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					readFile(file, path, state, loadFile, dialogIsVisible);
					return null;
				}
			});
			th.setDaemon(true);
			th.start();
		} else {
			readFile(file, path, state, loadFile, dialogIsVisible);
		}
		return true;
	}
	
	void readFile(File file, String path, PluginState state, boolean loadFile, boolean dialogIsVisible) {
		LOG.trace(String.format("readFile(%s, %s, %s, %b)", file.getName(), path, PluginState.argString(state), loadFile));
		try {
			// read the xml file
			openVSP3File.read(file);
			lastLoadedFile = path;
			// write script file
			plugin.writeCompGeomScriptFile("OpenVSP3Plugin.vspscript");
			// run simple compgeom
			plugin.runOpenVSPScript(file.getAbsolutePath());
			// read the compgeom
			Map<String, String> compGeomMap = new LinkedHashMap<>();
			Map<String, String> tagCompGeomMap = new LinkedHashMap<>();
			plugin.readCompGeomMaps(compGeomMap, tagCompGeomMap);
			if (dialogIsVisible) {
				Platform.runLater(() -> {
					updateOpenVSP3File(compGeomMap, tagCompGeomMap, state, loadFile);
				});
			} else {
				updateOpenVSP3File(compGeomMap, tagCompGeomMap, state, loadFile);
			}
		} catch (Exception ex) {
			unlockUI(true);
			dialog.showErrorPopup(ex);
		}
	}
		
	void updateOpenVSP3File(Map<String, String> compGeomMap, Map<String, String> tagCompGeomMap, PluginState state, boolean loadFile) {
		LOG.trace(String.format("updateOpenVSP3File(compGeomMap, tagCompGeomMap, %s, %b)",
				PluginState.argString(state), loadFile));
		try {
			openVSP3File.updateCompGeom(compGeomMap);
			openVSP3File.updateTagCompGeom(tagCompGeomMap);
			updateFile();
			if (state != null) {
				restoreState(state, loadFile);
			}
		} catch (Exception ex) {
			unlockUI(true);
			dialog.showErrorPopup(ex);
		}
	}

	/**
	 * In addition to changing the cursor disable some of the functionality
	 */
	private void lockUI() {
		root.getScene().setCursor(javafx.scene.Cursor.WAIT);
		applyButton.setDisable(true);
		revertButton.setDisable(true);
		showVSPButton.setDisable(true);
		if (!plugin.isOpenMDAO()) resetMCtoVSPButton.setDisable(true);
		okButton.setDisable(true);
		cancelButton.setDisable(true);
	}

	/**
	 * In addition to changing the cursor re-enable functionality
	 */
	private void unlockUI(boolean failed) {
//		try { Thread.sleep(3000); } catch(Exception e) {}
		PlatformRunAndWait(() -> {
			if (failed) label.setText("Loading File Failed");
			applyButton.setDisable(false);
			revertButton.setDisable(false);
			showVSPButton.setDisable(false);
			if (!plugin.isOpenMDAO()) resetMCtoVSPButton.setDisable(false);
			okButton.setDisable(false);
			cancelButton.setDisable(false);
			root.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		});
	}
	
	private void loadTreeView() {
		LOG.trace("loadTreeView()");
		if (openVSP3File == null) return;
		// reset the rootItem
		rootItem.getChildren().clear();
		DesignVariableGroup rootDesignVariableGroup = rootItem.getValue();
		rootDesignVariableGroup.getDesignVariables().clear();
		for (int i = 0; i < openVSP3File.getContainerArrayList().size(); i++) {
			TreeItem<DesignVariableGroup> container = openVSP3File.getContainerArrayList().get(i);
			rootDesignVariableGroup.getDesignVariables().addAll(container.getValue().getDesignVariables());
			rootItem.getChildren().add(container);
		}
		rootItem.setExpanded(true);
		treeView.getSelectionModel().select(rootItem);
	}
	
	private void updateFile() {
		LOG.trace("updateFile()");
		ignoreUpdate = false;
		unlockUI(false);
		openVSPFileTextField.setText(lastLoadedFile);
		tableViewData = designVariableList;
		setChoiceBox.getItems().clear();
		setChoiceBox.getItems().addAll(openVSP3File.getSetNames());
		setChoiceBox.getSelectionModel().select(0);
		updateFilters();
		loadTreeView();
	}
	
	private void updateSelection() {
		LOG.trace("updateSelection()");
		ObservableList<DesignVariable> data = FXCollections.observableArrayList();
		for (TreeItem<DesignVariableGroup> item : treeView.getSelectionModel().getSelectedItems()) {
			if (item != null) data.addAll(item.getValue().getDesignVariables());
		}
		tableViewData = data;
		updateFilters();
	}
	
	private void updateFilters() {
		LOG.trace("updateFilters()");
		if (tableViewData == null) return;
		SortedList<DesignVariable> sortedList = getSortedFilteredDesignVariables(tableViewData);
		tableView.setItems(sortedList);
		sortedList.comparatorProperty().bind(tableView.comparatorProperty());
		updateLabel();
	}
	
	private SortedList<DesignVariable> getSortedFilteredDesignVariables(ObservableList<DesignVariable> designVariables) {
		LOG.trace("getSortedFilteredDesignVariables(designVariables)");
		Pattern pattern;
		try {
			pattern = Pattern.compile(filterTextField.getText());
		} catch(Exception ex) {
			pattern = emptyPattern;
		}
		Pattern finalPattern = pattern; // make lambda happy
		return new SortedList<>(designVariables.filtered((DesignVariable dv) -> {
			boolean filter = ((selectedOnlyButton.isSelected() && !dv.isChecked()) ||
					(!filterTextField.getText().isEmpty() && !finalPattern.matcher(dv.getFullName()).find()));
			return !filter;
		}));
	}
	
	private void updateLabel() {
		if (ignoreUpdate) return;
		LOG.trace("updateLabel()");
		loadingFile = false;
		label.setText(String.format("Design Variables Selected (%d/%d) total (%d/%d)",
				tableView.getItems().filtered(dv -> dv.isChecked()).size(),
				tableView.getItems().size(),
				designVariableList.filtered(dv -> dv.isChecked()).size(),
				designVariableList.size()
		));
	}
	
	private void restoreState(PluginState state, boolean loadFile) {
		LOG.trace(String.format("restoreState(%s, %b)", PluginState.argString(state), loadFile));
		ignoreUpdate = true; // just update once after restore
		flatNamesButton.setSelected(state.getFlatNames());
		addIDButton.setSelected(state.getAddID());
		groupOutputsButton.setSelected(state.getGroupOutputs());
		logChoiceBox.valueProperty().set(Logger.LogLevel.valueOf(state.getLogLevel()));
		epsilonTextField.setText((state.getEpsilon() == null) ? "" : state.getEpsilon().toString());
		setEpsilonAction(null);
		setChoiceBox.getSelectionModel().select(state.getSetID());
		nApplyDesChoiceBox.getSelectionModel().select(state.getNApplyDes()-1);
		// check versions
		boolean ignoreWarnings = false;
		String question = "\n\nIgnore all other warnings?\n";
		if (loadFile && (!state.getPluginVersion().equals(OpenVSP3Plugin.VERSION) || !state.getVSPVersion().equals(plugin.getOpenVSPVersion()))) {
			String message = String.format("WARNING"
					+ "\nThis model was created with a different version of either the OpenVSP3Plugin or OpenVSP."
					+ "\n\nPlugin:\nModel = %s\nCurrent = %s\n\nOpenVSP:\nModel = %s\nCurrent = %s",
					state.getPluginVersion(), OpenVSP3Plugin.VERSION, state.getVSPVersion(), plugin.getOpenVSPVersion());
			ignoreWarnings = dialog.showPopup(message + question);
		}
		// More robust to find the state.dv in the openVSP3File list and index for missing/misconfigured dv's
		LOG.debug(String.format("restoreState() - %d Design Variables Selected", state.getDesignVariables().size()));
		for (DesignVariable dv : state.getDesignVariables()) {
			String variableName = dv.getModelCenterName(false, true, false);
			ObservableList<DesignVariable> existing = designVariableList.filtered(v -> (v.getModelCenterName(false, true, false).equals(variableName)));
			if (existing.isEmpty()) {
				String variableNameNoID = dv.getModelCenterName(false, false, false);
				ObservableList<DesignVariable> existingNoID = designVariableList.filtered(v -> (v.getModelCenterName(false, false, false).equals(variableNameNoID)));
				if (existingNoID.size() == 1) {
					boolean useNew = dialog.showPopup(String.format("Could not find design variable\n%s\nbut found variable\n%s\n\nUse the new variable?", variableName, variableNameNoID));
					if (useNew) {
						DesignVariable dv2 = existingNoID.get(0);
						// set openVSP3File design variable to match state
						dv2.checkedProperty().set(true);
						dv2.valueProperty().setValue(dv.getValue());
						// set the vsp value on the state dv
						dv.setVspValue(dv2.getVspValue());
						dv.setXPath(dv2.getXPath());
					}
				} else if (!ignoreWarnings) {
					String message = String.format("Could not find design variable %s in vsp3 file.", variableName);
					ignoreWarnings = dialog.showPopup(message + question);
				}
			} else if (existing.size() == 1) {
				DesignVariable dv2 = existing.get(0);
				if (!dv.getState().equals(dv2.getState()) && !(dv.getState().equals("MCOutput") && dv2.getState().equals("Input"))) {
					if (!ignoreWarnings) {
						String message = String.format("Design variable %s changed from %s to %s.\nYou must open the UI and Apply these changes to ModelCenter.", variableName, dv.getState(), dv2.getState());
						ignoreWarnings = dialog.showPopup(message + question);
					}
				} else {
					dv2.setState(dv.getState());
				}
				// set openVSP3File design variable to match state
				dv2.checkedProperty().set(true);
				dv2.valueProperty().setValue(dv.getValue());
				// set the vsp value on the state dv
				dv.setVspValue(dv2.getVspValue());
				dv.setXPath(dv2.getXPath());
			} else {
				if (!ignoreWarnings) {
					String message = String.format("Found %d copies of design variable %s in vsp3 file.", existing.size(), variableName);
					ignoreWarnings = dialog.showPopup(message + question);
				}
			}
		}
		ignoreUpdate = false;
		updateFilters();
		dialog.repaint();
	}
	
	private void refreshTable() {
		LOG.trace("refreshTable()");
		// fixes table refresh only available since JavaFX 8u60
		// so doesn't work before ModelCenter 12.0
//		tableView.refresh(); // since JavaFX 8u60
		// this seems to work with older JavaFX 
		tableView.getColumns().get(0).setVisible(false);
		tableView.getColumns().get(0).setVisible(true);
	}
	
	@SuppressWarnings("unchecked") 
	private void setupTableView() {
		LOG.trace("setupTableView()");
		tableView.setRowFactory(new Callback<TableView<DesignVariable>, TableRow<DesignVariable>>() {
			@Override
			public TableRow<DesignVariable> call(TableView<DesignVariable> tableView) {
				final TableRow<DesignVariable> row = new TableRow<>();
				final ContextMenu rowMenu = new ContextMenu();
				MenuItem toggleItem = new MenuItem("Toggle Input/MCOutput");
				toggleItem.setOnAction((ActionEvent event) -> {
					row.getItem().toggleState();
					refreshTable();
				});
				rowMenu.getItems().addAll(toggleItem);
				// only display context menu for non-null items:
				row.contextMenuProperty().bind(
						Bindings.when(Bindings.isNotNull(row.itemProperty()))
						.then(rowMenu)
						.otherwise((ContextMenu) null));
				row.setOnMouseClicked(event -> {
					if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
						row.getItem().toggleState();
						refreshTable();
					}
				});
				return row;
			}
		});
		Callback<TableColumn<DesignVariable, String>, TableCell<DesignVariable, String>> cellFactory =
			(TableColumn<DesignVariable, String> p) -> new DesignVariableTableCell();
		TableColumn<DesignVariable, Boolean> selectedCol = new TableColumn<>("Selected");
		// PropertyValueFactory looks for checkedProperty() method but will use getChecked() but table won't update automatically
		selectedCol.setCellValueFactory(new PropertyValueFactory<>("checked"));
		selectedCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectedCol));
		TableColumn<DesignVariable, String> containerCol = new TableColumn<>("Container");
		containerCol.setCellValueFactory(new PropertyValueFactory<>("container"));
		containerCol.setCellFactory(cellFactory);
		containerCol.setPrefWidth(150);
		TableColumn<DesignVariable, String> groupCol = new TableColumn<>("Group");
		groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));
		groupCol.setCellFactory(cellFactory);
		groupCol.setPrefWidth(150);
		TableColumn<DesignVariable, String> nameCol = new TableColumn<>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameCol.setCellFactory(cellFactory);
		nameCol.setPrefWidth(200);
		TableColumn<DesignVariable, String> valueCol = new TableColumn<>("Value");
		valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
		valueCol.setCellFactory(cellFactory);
		valueCol.setPrefWidth(200);
		TableColumn<DesignVariable, String> idCol = new TableColumn<>("ID");
		idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
		idCol.setCellFactory(cellFactory);
		idCol.setPrefWidth(100);
		TableColumn<DesignVariable, String> stateCol = new TableColumn<>("State");
		stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
		stateCol.setCellFactory(cellFactory);
		TableColumn<DesignVariable, String> vspValCol = new TableColumn<>("VSP Value");
		vspValCol.setCellValueFactory(new PropertyValueFactory<>("vspValue"));
		vspValCol.setCellFactory(cellFactory);
		vspValCol.setPrefWidth(200);
		tableView.getColumns().setAll(selectedCol, containerCol, groupCol, nameCol, valueCol, idCol, stateCol, vspValCol);
		tableView.setEditable(true);
		tableView.setOnKeyPressed(event -> {
			DesignVariable dv = tableView.getSelectionModel().getSelectedItem();
			if (dv == null) return;
			if (event.getCode() == KeyCode.SPACE) dv.checkedProperty().set(!dv.isChecked());
			if (event.getCode() == KeyCode.O) dv.toggleState();
			if (event.getCode() == KeyCode.TAB) {
				setChoiceBox.requestFocus();
				event.consume();
			} 
		});
	}
	
	private void initialize() {
		LOG.trace("initialize()");
		designVariableList = FXCollections.<DesignVariable>observableArrayList((DesignVariable dv) -> new Observable[]{dv.checkedProperty()});
		designVariableList.addListener((Observable o) -> updateLabel());
		// initialize tableview
		setupTableView();
		// initialize treeview
		treeView.setRoot(rootItem);
		treeView.getSelectionModel().getSelectedItems().addListener((Observable o) -> updateSelection());
		filterTextField.textProperty().addListener((Observable o) -> updateFilters());
		updateLabel();
		// init FileChooser
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("OpenVSP 3", "*.vsp3"));
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
	}
	
	protected File selectFile() {
		LOG.trace("selectFile()");
		fileChooser.setTitle("Choose OpenVSP File");
		if (!openVSPFileTextField.getText().isEmpty()) {
			fileChooser.setInitialDirectory(new File(openVSPFileTextField.getText()).getParentFile());
		} else {
			fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		}
		return fileChooser.showOpenDialog(root.getScene().getWindow());
	}

	protected File findFile(String path) {
		LOG.trace("findFile(" + path + ")");
		fileChooser.setTitle(String.format("Find OpenVSP File: %s", path));
		return fileChooser.showOpenDialog(root.getScene().getWindow());
	}
}
