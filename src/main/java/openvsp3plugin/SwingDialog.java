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

import java.awt.BorderLayout;
import java.awt.Dimension;
import javafx.embed.swing.JFXPanel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * This is the Swing dialog (JFrame works better) that contains the JavaFX UI
 * in a JFXPanel.
 * All calls to SwingUtilities.invokeAndWait() are made in this class.
 */
@SuppressWarnings("serial") 
class SwingDialog extends JFrame {
	
	private static final Logger LOG = new Logger(SwingDialog.class.getSimpleName());
	final int width = 1200;
	final int height = 800;
	final String NOTICE	= "Copyright 2017 United States Government as represented by the\n"
						+ "Administrator of the National Aeronautics and Space Administration.\n\n"
						+ "All Rights Reserved.\n\n"
						+ "The OpenVSP3Plugin framework is licensed under the Apache License, Version 2.0\n"
						+ "(the \"License\"); you may not use this application except in compliance with the License.\n"
						+ "You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.\n\n"
						+ "Unless required by applicable law or agreed to in writing, software\n"
						+ "distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n"
						+ "WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the\n"
						+ "License for the specific language governing permissions and limitations\n"
						+ "under the License.\n";

	private String title;
	protected JavaFXUI controller;
	protected OpenVSP3Plugin plugin;
	
	static void invokeAndWait(Runnable action) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			action.run();
		});
	}
	
	static public String argString(SwingDialog dialog) {
		return (dialog == null) ? "null" : "not null";
	}
	
	SwingDialog(String title, OpenVSP3Plugin plugin) {
		this.plugin = plugin;
		this.title = title;
	}
	
	void initSwingDialog() {
		LOG.trace("initSwingDialog()");
		setTitle(title);
		setMinimumSize(new Dimension(600, 300));
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog(true);
			}
		});
		JFXPanel fxContainer = createJavaFXUI();
		fxContainer.setPreferredSize(new Dimension(width, height));
		add(fxContainer, BorderLayout.CENTER);
		pack();
	} 
	
	protected JFXPanel createJavaFXUI() {
		LOG.trace("createJavaFXUI()");
		JFXPanel fxContainer = new JFXPanel();
		// This must be constructed in the JavaFX application thread
		JavaFXUI.PlatformRunAndWait(() -> {
			controller = new JavaFXUI();
			controller.initJavaFXUI(plugin, this, fxContainer);
		});
		return fxContainer;
	}
	
	void closeDialog(boolean inEDT) {
		LOG.trace("closeDialog()");
		if (plugin.isOpenMDAO()) {
			LOG.trace("closeDialog() - OpenMDAO so exit");
			plugin.exit();
		} else if (inEDT) { // can't call invokeAndWait() if in the EDT Dialog "X" 
			setVisible(false);
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					setVisible(false);
				});
			} catch (Exception ex) {
				LOG.fatal(ex.toString());
			}
		}
	}
	
	void restoreDialog(PluginState state, boolean loadFile, boolean setVisible) throws Exception {
		LOG.trace(String.format("restoreDialog(state %s, loadFile %b, setVisible %b)",
				(state == null) ? "null" : "not null", loadFile, setVisible));
		if (setVisible) {
			SwingUtilities.invokeAndWait(() -> {
				setVisible(true);
				repaint();
			});
		}
		controller.restoreUI(state, loadFile);
	}
	
	boolean checkIfLoadingFile() {
		boolean value = controller.checkIfLoadingFile();
		LOG.trace("checkIfLoadingFile(" + value + ")");
		return value;
	}
	
	void checkIfStateDirty(PluginState state) {
		LOG.trace("checkIfStateDirty()");
		try {
			controller.checkIfStateDirty(state);
		} catch (Exception ex) {
			showErrorPopup(ex);
		}
	}
	
	boolean showPopup(String text) {

		class ShowPopupRunnable implements Runnable {
			String text;
			SwingDialog dialog;
			boolean showPopupValue;

			ShowPopupRunnable(String text, SwingDialog dialog) {
				this.text = text;
				this.dialog = dialog;
			}

			@Override
			public void run() {
				showPopupValue = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(dialog, text, dialog.title, JOptionPane.YES_NO_OPTION));
			}
		}
		
		ShowPopupRunnable spr = new ShowPopupRunnable(text, this);
		try {
			SwingUtilities.invokeAndWait(spr);
		} catch (Exception ex) {
			LOG.fatal(ex.toString());
		}
		return spr.showPopupValue;
	}
	
	void showErrorPopup(Exception ex) {
		showErrorPopup(ex.toString());
	}
	
	void showErrorPopup(String message) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				LOG.warn(message);
				JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
			});
		} catch (Exception ex) {
			LOG.fatal(message + "\n" + ex.toString());
		}
	}
	
	void showAboutDialog() {
		JOptionPane.showMessageDialog(this, title + "\n\n" + NOTICE, "About OpenVSP3Plugin", JOptionPane.INFORMATION_MESSAGE);
	}
}
