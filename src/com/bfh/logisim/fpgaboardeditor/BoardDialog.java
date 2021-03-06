/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.fpgaboardeditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;
import com.cburch.logisim.proj.Projects;

public class BoardDialog implements ActionListener, ComponentListener {

	private static class XMLFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(XML_EXTENSION);
		}

		@Override
		public String getDescription() {
			return Strings.get("XMLFileFilter"); // TODO: language adaptation
		}
	}

	private JFrame panel;
	public LinkedList<BoardRectangle> defined_components = new LinkedList<BoardRectangle>();
	public static final String pictureError = "/resources/logisim/error.png";
	public static final String pictureWarning = "/resources/logisim/warning.png";
	private String action_id;
	boolean abort;
	private BoardInformation TheBoard = new BoardInformation();
	private JTextField BoardNameInput;
	private JButton saveButton;
	private JButton loadButton;
	private BoardPanel picturepanel;
	public static final String XML_EXTENSION = ".xml";
	public static final FileFilter XML_FILTER = new XMLFileFilter();
	private String CancelStr = "cancel";
	private String FPGAStr = "fpgainfo";
	private int DefaultStandard = 0;
	private int DefaultDriveStrength = 0;
	private int DefaultPullSelection = 0;

	private int DefaultActivity = 0;

	/* BIg TODO: Add all language strings */

	public BoardDialog() {
		GridBagConstraints gbc = new GridBagConstraints();

		panel = new JFrame(Strings.get("FPGABoardEditor"));
		panel.setResizable(false);
		panel.addComponentListener(this);
		panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x,mlocation.y);

		// Set an empty board picture
		picturepanel = new BoardPanel(this);
		panel.add(picturepanel);

		JPanel ButtonPanel = new JPanel();
		GridBagLayout ButtonLayout = new GridBagLayout();
		ButtonPanel.setLayout(ButtonLayout);

		JLabel LocText = new JLabel("Board Name:  ");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		ButtonPanel.add(LocText, gbc);

		BoardNameInput = new JTextField(32);
		BoardNameInput.setEnabled(false);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		ButtonPanel.add(BoardNameInput, gbc);

		JButton cancelButton = new JButton("Cancel");
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		cancelButton.setActionCommand(CancelStr);
		cancelButton.addActionListener(this);
		ButtonPanel.add(cancelButton, gbc);

		loadButton = new JButton("Load");
		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		loadButton.setActionCommand("load");
		loadButton.addActionListener(this);
		loadButton.setEnabled(true);
		ButtonPanel.add(loadButton, gbc);

		saveButton = new JButton("Done and save");
		gbc.gridx = 4;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		saveButton.setEnabled(false);
		ButtonPanel.add(saveButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(ButtonPanel, gbc);

		panel.pack();
		/*
		 * panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
		 * panel.getHeight()));
		 */
		panel.setLocationRelativeTo(null);
		panel.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CancelStr)) {
			this.clear();
		} else if (e.getActionCommand().equals("save")) {
			panel.setVisible(false);
			TheBoard.setBoardName(BoardNameInput.getText());
			String filename = getDirName("",
					"Select directory to save board file:");
			filename += TheBoard.getBoardName() + ".xml";
			BoardWriterClass xmlwriter = new BoardWriterClass(TheBoard,
					picturepanel.getScaledImage(picturepanel.getWidth(),
							picturepanel.getHeight()));
			xmlwriter.PrintXml(filename);
			this.clear();
		} else if (e.getActionCommand().equals("load")) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogTitle("Choose XML board description file to use");
			fc.setFileFilter(XML_FILTER);
			fc.setAcceptAllFileFilterUsed(false);
			int retval = fc.showOpenDialog(null);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				SetBoardName(file.getName());
				String FileName = file.getPath();
				BoardReaderClass reader = new BoardReaderClass(FileName);
				TheBoard = reader.GetBoardInformation();
				picturepanel.SetImage(TheBoard.GetImage());
				for (FPGAIOInformationContainer comp : TheBoard
						.GetAllComponents())
					defined_components.add(comp.GetRectangle());
				if ((TheBoard.GetNrOfDefinedComponents() > 0)
						&& TheBoard.fpga.FpgaInfoPresent())
					saveButton.setEnabled(true);
				picturepanel.repaint();
			}

		}
	}

	private String checkIfEndsWithSlash(String path) {
		if (!path.endsWith("/")) {
			path += "/";
		}
		return (path);
	}

	public void clear() {
		if (panel.isVisible())
			panel.setVisible(false);
		picturepanel.clear();
		defined_components.clear();
		TheBoard.clear();
		BoardNameInput.setText("");
		saveButton.setEnabled(false);
		loadButton.setEnabled(true);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	public int GetDefaultActivity() {
		return DefaultActivity;
	}

	public int GetDefaultDriveStrength() {
		return DefaultDriveStrength;
	}

	public int GetDefaultPullSelection() {
		return DefaultPullSelection;
	}

	public int GetDefaultStandard() {
		return DefaultStandard;
	}

	private String getDirName(String old, String window_name) {
		JFileChooser fc = new JFileChooser(old);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle(window_name);
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			old = checkIfEndsWithSlash(file.getPath());
		}
		return old;
	}

	/*
	 * private ButtonClass getButtonInformation(BoardRectangle rect) {
	 * button_info.SetId(-1); final JDialog selWindow = new
	 * JDialog(panel,"Button properties"); /* here the action listener is
	 * defined
	 */
	/*
	 * ActionListener actionListener = new ActionListener(){ public void
	 * actionPerformed(ActionEvent e){ if
	 * (e.getActionCommand().equals("cancel")) { abort = true; }
	 * selWindow.setVisible(false); } }; GridBagLayout dialogLayout = new
	 * GridBagLayout(); GridBagConstraints c = new GridBagConstraints();
	 * selWindow.setLayout(dialogLayout);
	 * 
	 * JLabel LocText = new JLabel("Specify FPGA pin location:"); c.gridx = 0;
	 * c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(LocText,c);
	 * 
	 * JTextField LocInput = new JTextField(); c.gridx = 0; c.gridy = 1; c.fill
	 * = GridBagConstraints.HORIZONTAL; selWindow.add(LocInput,c);
	 * 
	 * JLabel PullText = new JLabel("Specify FPGA pin pull behavior:"); c.gridx
	 * = 0; c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(PullText,c);
	 * 
	 * JComboBox PullInput = new JComboBox(PullBehaviors.Behavior_strings);
	 * PullInput.setSelectedIndex(DefaultPullSelection); c.gridx = 0; c.gridy =
	 * 3; c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(PullInput,c);
	 * 
	 * JLabel ActiveText = new JLabel("Specify Button activity:"); c.gridx = 0;
	 * c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(ActiveText,c);
	 * 
	 * JComboBox ActiveInput = new JComboBox(PinActivity.Behavior_strings);
	 * ActiveInput.setSelectedIndex(DefaultActivity); c.gridx = 0; c.gridy = 5;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(ActiveInput,c);
	 * 
	 * JLabel StandardText = new JLabel("Specify FPGA pin standard:"); c.gridx =
	 * 0; c.gridy = 6; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardText,c);
	 * 
	 * JComboBox StandardInput = new JComboBox(IoStandards.Behavior_strings);
	 * StandardInput.setSelectedIndex(DefaultStandard); c.gridx = 0; c.gridy =
	 * 7; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardInput,c);
	 * 
	 * JButton OkayButton = new JButton("Done and Store");
	 * OkayButton.setActionCommand("done");
	 * OkayButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 8;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(OkayButton,c);
	 * 
	 * JButton CancelButton = new JButton("Cancel");
	 * CancelButton.setActionCommand("cancel");
	 * CancelButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 9;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(CancelButton,c);
	 * 
	 * selWindow.pack(); PointerInfo mouseloc = MouseInfo.getPointerInfo();
	 * Point mlocation = mouseloc.getLocation();
	 * selWindow.setLocation(mlocation.x,mlocation.y); selWindow.setModal(true);
	 * selWindow.setResizable(false);
	 * selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	 * selWindow.setAlwaysOnTop(true); abort = false; while (!abort){
	 * selWindow.setVisible(true); if (LocInput.getText().isEmpty()&& !abort) {
	 * showDialogNotification(selWindow,"Error",
	 * "<html>You have to specify a pin location!</html>"); } else { if (!abort)
	 * { DefaultActivity = ActiveInput.getSelectedIndex(); DefaultPullSelection
	 * = PullInput.getSelectedIndex(); DefaultStandard =
	 * StandardInput.getSelectedIndex(); button_info.Set(rect,
	 * LocInput.getText(), PullInput.getSelectedItem().toString(),
	 * ActiveInput.getSelectedItem().toString(),
	 * StandardInput.getSelectedItem().toString()); abort=true; } } }
	 * selWindow.dispose(); return button_info; }
	 * 
	 * private LEDClass getLEDInformation(BoardRectangle rect) {
	 * led_info.SetId(-1); final JDialog selWindow = new
	 * JDialog(panel,"LED properties"); /* here the action listener is defined
	 */
	/*
	 * ActionListener actionListener = new ActionListener(){ public void
	 * actionPerformed(ActionEvent e){ if
	 * (e.getActionCommand().equals("cancel")) { abort = true; }
	 * selWindow.setVisible(false); } }; GridBagLayout dialogLayout = new
	 * GridBagLayout(); GridBagConstraints c = new GridBagConstraints();
	 * selWindow.setLayout(dialogLayout);
	 * 
	 * JLabel LocText = new JLabel("Specify FPGA pin location:"); c.gridx = 0;
	 * c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(LocText,c);
	 * 
	 * JTextField LocInput = new JTextField(); c.gridx = 0; c.gridy = 1; c.fill
	 * = GridBagConstraints.HORIZONTAL; selWindow.add(LocInput,c);
	 * 
	 * JLabel DriveText = new JLabel("Specify FPGA pin drive strength:");
	 * c.gridx = 0; c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(DriveText,c);
	 * 
	 * JComboBox DriveInput = new JComboBox(DriveStrength.Behavior_strings);
	 * DriveInput.setSelectedIndex(DefaultDriveStrength); c.gridx = 0; c.gridy =
	 * 3; c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(DriveInput,c);
	 * 
	 * JLabel ActiveText = new JLabel("Specify LED activity:"); c.gridx = 0;
	 * c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(ActiveText,c);
	 * 
	 * JComboBox ActiveInput = new JComboBox(PinActivity.Behavior_strings);
	 * ActiveInput.setSelectedIndex(DefaultActivity); c.gridx = 0; c.gridy = 5;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(ActiveInput,c);
	 * 
	 * JLabel StandardText = new JLabel("Specify FPGA pin standard:"); c.gridx =
	 * 0; c.gridy = 6; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardText,c);
	 * 
	 * JComboBox StandardInput = new JComboBox(IoStandards.Behavior_strings);
	 * StandardInput.setSelectedIndex(DefaultStandard); c.gridx = 0; c.gridy =
	 * 7; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardInput,c);
	 * 
	 * JButton OkayButton = new JButton("Done and Store");
	 * OkayButton.setActionCommand("done");
	 * OkayButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 8;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(OkayButton,c);
	 * 
	 * JButton CancelButton = new JButton("Cancel");
	 * CancelButton.setActionCommand("cancel");
	 * CancelButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 9;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(CancelButton,c);
	 * 
	 * selWindow.pack(); PointerInfo mouseloc = MouseInfo.getPointerInfo();
	 * Point mlocation = mouseloc.getLocation();
	 * selWindow.setLocation(mlocation.x,mlocation.y); selWindow.setModal(true);
	 * selWindow.setResizable(false);
	 * selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	 * selWindow.setAlwaysOnTop(true); abort = false; while (!abort){
	 * selWindow.setVisible(true); if (LocInput.getText().isEmpty()&& !abort) {
	 * showDialogNotification(selWindow,"Error",
	 * "<html>You have to specify a pin location!</html>"); } else { if (!abort)
	 * { DefaultActivity = ActiveInput.getSelectedIndex(); DefaultDriveStrength
	 * = DriveInput.getSelectedIndex(); DefaultStandard =
	 * StandardInput.getSelectedIndex(); led_info.Set(rect, LocInput.getText(),
	 * DriveInput.getSelectedItem().toString(),
	 * ActiveInput.getSelectedItem().toString(),
	 * StandardInput.getSelectedItem().toString()); abort=true; } } }
	 * selWindow.dispose(); return led_info; }
	 * 
	 * private PinClass getPinInformation(BoardRectangle rect) {
	 * pin_info.SetId(-1); final JDialog selWindow = new
	 * JDialog(panel,"Pin properties"); /* here the action listener is defined
	 */
	/*
	 * ActionListener actionListener = new ActionListener(){ public void
	 * actionPerformed(ActionEvent e){ if
	 * (e.getActionCommand().equals("cancel")) { abort = true; }
	 * selWindow.setVisible(false); } }; GridBagLayout dialogLayout = new
	 * GridBagLayout(); GridBagConstraints c = new GridBagConstraints();
	 * selWindow.setLayout(dialogLayout);
	 * 
	 * JLabel LocText = new JLabel("Specify FPGA pin location:"); c.gridx = 0;
	 * c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(LocText,c);
	 * 
	 * JTextField LocInput = new JTextField(); c.gridx = 0; c.gridy = 1; c.fill
	 * = GridBagConstraints.HORIZONTAL; selWindow.add(LocInput,c);
	 * 
	 * JLabel DriveText = new JLabel("Specify FPGA pin drive strength:");
	 * c.gridx = 0; c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(DriveText,c);
	 * 
	 * JComboBox DriveInput = new JComboBox(DriveStrength.Behavior_strings);
	 * DriveInput.setSelectedIndex(DefaultDriveStrength); c.gridx = 0; c.gridy =
	 * 3; c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(DriveInput,c);
	 * 
	 * JLabel PullText = new JLabel("Specify FPGA pin pull behavior:"); c.gridx
	 * = 0; c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(PullText,c);
	 * 
	 * JComboBox PullInput = new JComboBox(PullBehaviors.Behavior_strings);
	 * PullInput.setSelectedIndex(DefaultPullSelection); c.gridx = 0; c.gridy =
	 * 5; c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(PullInput,c);
	 * 
	 * JLabel StandardText = new JLabel("Specify FPGA pin standard:"); c.gridx =
	 * 0; c.gridy = 6; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardText,c);
	 * 
	 * JComboBox StandardInput = new JComboBox(IoStandards.Behavior_strings);
	 * StandardInput.setSelectedIndex(DefaultStandard); c.gridx = 0; c.gridy =
	 * 7; c.fill = GridBagConstraints.HORIZONTAL;
	 * selWindow.add(StandardInput,c);
	 * 
	 * JButton OkayButton = new JButton("Done and Store");
	 * OkayButton.setActionCommand("done");
	 * OkayButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 8;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(OkayButton,c);
	 * 
	 * JButton CancelButton = new JButton("Cancel");
	 * CancelButton.setActionCommand("cancel");
	 * CancelButton.addActionListener(actionListener); c.gridx = 0; c.gridy = 9;
	 * c.fill = GridBagConstraints.HORIZONTAL; selWindow.add(CancelButton,c);
	 * 
	 * selWindow.pack(); PointerInfo mouseloc = MouseInfo.getPointerInfo();
	 * Point mlocation = mouseloc.getLocation();
	 * selWindow.setLocation(mlocation.x,mlocation.y); selWindow.setModal(true);
	 * selWindow.setResizable(false);
	 * selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	 * selWindow.setAlwaysOnTop(true); abort = false; while (!abort){
	 * selWindow.setVisible(true); if (LocInput.getText().isEmpty()&& !abort) {
	 * showDialogNotification(selWindow,"Error",
	 * "<html>You have to specify a pin location!</html>"); } else { if (!abort)
	 * { DefaultPullSelection = PullInput.getSelectedIndex();
	 * DefaultDriveStrength = DriveInput.getSelectedIndex(); DefaultStandard =
	 * StandardInput.getSelectedIndex(); pin_info.Set(rect, LocInput.getText(),
	 * DriveInput.getSelectedItem().toString(),
	 * StandardInput.getSelectedItem().toString(),
	 * PullInput.getSelectedItem().toString()); abort=true; } } }
	 * selWindow.dispose(); return pin_info; }
	 */
	private void getFpgaInformation() {
		final JDialog selWindow = new JDialog(panel, "FPGA properties");
		/* here the action listener is defined */
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals(CancelStr)) {
					abort = true;
				}
				selWindow.setVisible(false);
			}
		};
		GridBagLayout dialogLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		selWindow.setLayout(dialogLayout);
		abort = false;

		JPanel ClockPanel = new JPanel();
		GridBagLayout ClockLayout = new GridBagLayout();
		ClockPanel.setLayout(ClockLayout);

		JLabel FreqText = new JLabel("Specify Clock frequency:");
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(FreqText, c);

		JPanel FreqPanel = new JPanel();
		GridBagLayout FreqLayout = new GridBagLayout();
		FreqPanel.setLayout(FreqLayout);

		JTextField FreqInput = new JTextField(10);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		FreqPanel.add(FreqInput, c);

		String[] freqStrs = { "Hz", "kHz", "MHz" };
		JComboBox<String> StandardInput = new JComboBox<>(freqStrs);
		StandardInput.setSelectedIndex(2);
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		FreqPanel.add(StandardInput, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(FreqPanel, c);

		JLabel LocText = new JLabel("Specify Clock pin location:");
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(LocText, c);

		JTextField LocInput = new JTextField();
		c.gridx = 0;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(LocInput, c);

		JLabel PullText = new JLabel("Specify clock pin pull behavior:");
		c.gridx = 0;
		c.gridy = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(PullText, c);

		JComboBox<String> PullInput = new JComboBox<>(
				PullBehaviors.Behavior_strings);
		PullInput.setSelectedIndex(0);
		c.gridx = 0;
		c.gridy = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(PullInput, c);

		JLabel StandardText = new JLabel("Specify clock pin standard:");
		c.gridx = 0;
		c.gridy = 6;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(StandardText, c);

		JComboBox<String> StdInput = new JComboBox<>(
				IoStandards.Behavior_strings);
		StdInput.setSelectedIndex(0);
		c.gridx = 0;
		c.gridy = 7;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(StdInput, c);

		JLabel UnusedPinsText = new JLabel("Unused FPGA pin behavior:");
		c.gridx = 0;
		c.gridy = 8;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(UnusedPinsText, c);

		JComboBox<String> UnusedPinsInput = new JComboBox<>(
				PullBehaviors.Behavior_strings);
		UnusedPinsInput.setSelectedIndex(0);
		c.gridx = 0;
		c.gridy = 9;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(UnusedPinsInput, c);

		JLabel PosText = new JLabel("Specify FPGA location in JTAG chain:");
		c.gridx = 0;
		c.gridy = 10;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(PosText, c);
		JTextField PosInput = new JTextField("1");
		c.gridx = 0;
		c.gridy = 11;
		c.fill = GridBagConstraints.HORIZONTAL;
		ClockPanel.add(PosInput, c);

		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NORTH;
		selWindow.add(ClockPanel, c);

		JPanel FPGAPanel = new JPanel();
		GridBagLayout FPGALayout = new GridBagLayout();
		FPGAPanel.setLayout(FPGALayout);

		JLabel VendorText = new JLabel("Specify FPGA vendor:");
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(VendorText, c);

		JComboBox<String> VendorInput = new JComboBox<>(FPGAClass.Vendors);
		VendorInput.setSelectedIndex(0);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(VendorInput, c);

		JLabel FamilyText = new JLabel("Specify FPGA family:");
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FamilyText, c);

		JTextField FamilyInput = new JTextField();
		c.gridx = 0;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FamilyInput, c);

		JLabel PartText = new JLabel("Specify FPGA part:");
		c.gridx = 0;
		c.gridy = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(PartText, c);

		JTextField PartInput = new JTextField();
		c.gridx = 0;
		c.gridy = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(PartInput, c);

		JLabel BoxText = new JLabel("Specify FPGA package:");
		c.gridx = 0;
		c.gridy = 6;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(BoxText, c);

		JTextField BoxInput = new JTextField();
		c.gridx = 0;
		c.gridy = 7;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(BoxInput, c);

		JLabel SpeedText = new JLabel("Specify FPGA speed grade:");
		c.gridx = 0;
		c.gridy = 8;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(SpeedText, c);

		JTextField SpeedInput = new JTextField();
		c.gridx = 0;
		c.gridy = 9;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(SpeedInput, c);

		JLabel FlashName = new JLabel("Specify flash name:");
		c.gridx = 0;
		c.gridy = 10;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FlashName,c);
		JTextField FlashNameInput = new JTextField("");
		c.gridx = 0;
		c.gridy = 11;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FlashNameInput,c);

		JLabel FlashPosText = new JLabel("Specify flash location in JTAG chain:");
		c.gridx = 0;
		c.gridy = 12;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FlashPosText,c);
		JTextField FlashPosInput = new JTextField("2");
		c.gridx = 0;
		c.gridy = 13;
		c.fill = GridBagConstraints.HORIZONTAL;
		FPGAPanel.add(FlashPosInput,c);

		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.NORTH;
		selWindow.add(FPGAPanel, c);

		JCheckBox UsbTmc = new JCheckBox("USBTMC Download");
		UsbTmc.setSelected(false);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		selWindow.add(UsbTmc, c);

		JButton CancelButton = new JButton("Cancel");
		CancelButton.addActionListener(actionListener);
		CancelButton.setActionCommand(CancelStr);
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		selWindow.add(CancelButton, c);

		JButton SaveButton = new JButton("Done and Store");
		SaveButton.addActionListener(actionListener);
		SaveButton.setActionCommand("save");
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		selWindow.add(SaveButton, c);

		selWindow.pack();
		selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(),
				selWindow.getHeight()));
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// selWindow.setLocation(mlocation.x,mlocation.y);
		selWindow.setModal(true);
		selWindow.setResizable(false);
		selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		selWindow.setAlwaysOnTop(false);
		boolean save_settings;
		while ((!abort) && (!TheBoard.fpga.FpgaInfoPresent())) {
			selWindow.setVisible(true);
			if (!abort) {
				save_settings = true;
				switch ((int) getFrequency(FreqInput.getText(), StandardInput
						.getSelectedItem().toString())) {
				case -2:
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>The clock frequency should only contain the chars</BR>"
									+ "'0'..'9' and '.'!</html>");
					break;
				case -1:
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>The clock frequency cannot be a fraction of a Hz</html>");
					break;
				case 0:
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the clock frequency!</html>");
					break;
				default:
					break;
				}
				if (save_settings && LocInput.getText().isEmpty()) {
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the clock-pin location!</html>");
				}
				if (save_settings && FamilyInput.getText().isEmpty()) {
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the FPGA family!</html>");
				}
				if (save_settings && PartInput.getText().isEmpty()) {
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the FPGA part!</html>");
				}
				if (save_settings && BoxInput.getText().isEmpty()) {
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the FPGA package!</html>");
				}
				if (save_settings && SpeedInput.getText().isEmpty()) {
					save_settings = false;
					showDialogNotification(selWindow, "Error",
							"<html>You have to specify the FPGA speed-grade!</html>");
				}
				if (save_settings) {
					TheBoard.fpga.Set(
							getFrequency(FreqInput.getText(), StandardInput
									.getSelectedItem().toString()), LocInput
									.getText(), PullInput.getSelectedItem()
									.toString(), StdInput.getSelectedItem()
									.toString(), FamilyInput.getText(),
							PartInput.getText(), BoxInput.getText(), SpeedInput
									.getText(), VendorInput.getSelectedItem()
									.toString(), UnusedPinsInput
									.getSelectedItem().toString(), UsbTmc
									.isSelected(), PosInput.getText(),
									FlashNameInput.getText(), FlashPosInput.getText());
				}
			}
		}
		selWindow.dispose();
	}

	private long getFrequency(String chars, String speed) {
		long result = 0;
		long multiplier = 1;
		boolean dec_mult = false;

		if (speed.equals("kHz"))
			multiplier = 1000;
		if (speed.equals("MHz"))
			multiplier = 1000000;
		for (int i = 0; i < chars.length(); i++) {
			if (chars.charAt(i) >= '0' && chars.charAt(i) <= '9') {
				result *= 10;
				result += (chars.charAt(i) - '0');
				if (dec_mult) {
					multiplier /= 10;
					if (multiplier == 0)
						return -1;
				}
			} else {
				if (chars.charAt(i) == '.') {
					dec_mult = true;
				} else {
					return -2;
				}

			}
		}
		result *= multiplier;

		return result;
	}

	public JFrame GetPanel() {
		return panel;
	}

	public boolean isActive() {
		return panel.isVisible();
	}

	public void SelectDialog(BoardRectangle rect) {

		/*
		 * Before doing anything we have to check that this region does not
		 * overlap with an already defined region. If we detect an overlap we
		 * abort the action.
		 */
		Iterator<BoardRectangle> iter = defined_components.iterator();
		Boolean overlap = false;
		while (iter.hasNext()) {
			overlap |= iter.next().Overlap(rect);
		}
		if (overlap) {
			showDialogNotification("Error",
					"<html>Found Overlapping regions!<br>Cannot process!</html>");
			return;
		}
		String res = ShowItemSelectWindow();
		if (res.equals(CancelStr))
			return;
		if (res.equals(FPGAStr)) {
			getFpgaInformation();
			if ((TheBoard.GetNrOfDefinedComponents() > 0)
					&& TheBoard.fpga.FpgaInfoPresent())
				saveButton.setEnabled(true);
			if (TheBoard.fpga.FpgaInfoPresent())
				defined_components.add(rect);
		} else {
			FPGAIOInformationContainer comp = new FPGAIOInformationContainer(
					IOComponentTypes.valueOf(res), rect, this);
			if (comp.IsKnownComponent()) {
				TheBoard.AddComponent(comp);
				defined_components.add(rect);
				if ((TheBoard.GetNrOfDefinedComponents() > 0)
						&& TheBoard.fpga.FpgaInfoPresent())
					saveButton.setEnabled(true);
			}
		}
	}

	public void setActive() {
		this.clear();
		panel.setVisible(true);
	}

	public void SetBoardName(String name) {
		String comps = name.toUpperCase();
		comps = comps.replaceAll(".PNG", "");
		comps = comps.replaceAll(".XML", "");
		BoardNameInput.setEnabled(true);
		BoardNameInput.setText(comps);
		TheBoard.setBoardName(comps);
		loadButton.setEnabled(false);
	}

	public void SetDefaultActivity(int value) {
		DefaultActivity = value;
	}

	public void SetDefaultDriveStrength(int value) {
		DefaultDriveStrength = value;
	}

	public void SetDefaultPullSelection(int value) {
		DefaultPullSelection = value;
	}

	public void SetDefaultStandard(int value) {
		DefaultStandard = value;
	}

	private void showDialogNotification(JDialog parent, String type,
			String string) {
		final JDialog dialog = new JDialog(parent, type);
		JLabel pic = new JLabel();
		if (type.equals("Warning")) {
			pic.setIcon(new ImageIcon(getClass().getResource(pictureWarning)));
		} else {
			pic.setIcon(new ImageIcon(getClass().getResource(pictureError)));
		}
		GridBagLayout dialogLayout = new GridBagLayout();
		dialog.setLayout(dialogLayout);
		GridBagConstraints c = new GridBagConstraints();
		JLabel message = new JLabel(string);
		JButton close = new JButton("close");
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// panel.setAlwaysOnTop(true);
				dialog.dispose();
			}
		};
		close.addActionListener(actionListener);

		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 20;
		dialog.add(pic, c);

		c.gridx = 1;
		c.gridy = 0;
		dialog.add(message, c);

		c.gridx = 1;
		c.gridy = 1;
		dialog.add(close, c);
		dialog.pack();
		dialog.setLocationRelativeTo(panel);
		dialog.setAlwaysOnTop(false);
		dialog.setVisible(true);

	}

	private void showDialogNotification(String type, String string) {
		final JFrame dialog = new JFrame(type);
		JLabel pic = new JLabel();
		if (type.equals("Warning")) {
			pic.setIcon(new ImageIcon(getClass().getResource(pictureWarning)));
		} else {
			pic.setIcon(new ImageIcon(getClass().getResource(pictureError)));
		}
		GridBagLayout dialogLayout = new GridBagLayout();
		dialog.setLayout(dialogLayout);
		GridBagConstraints c = new GridBagConstraints();
		JLabel message = new JLabel(string);
		JButton close = new JButton("close");
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// panel.setAlwaysOnTop(true);
				dialog.dispose();
			}
		};
		close.addActionListener(actionListener);

		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 20;
		dialog.add(pic, c);

		c.gridx = 1;
		c.gridy = 0;
		dialog.add(message, c);

		c.gridx = 1;
		c.gridy = 1;
		dialog.add(close, c);
		dialog.pack();
		dialog.setLocationRelativeTo(panel);
		dialog.setAlwaysOnTop(false);
		dialog.setVisible(true);

	}

	private String ShowItemSelectWindow() {
		action_id = CancelStr;
		final JDialog selWindow = new JDialog(panel, "Action Select Window");
		/* here the action listener is defined */
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				action_id = e.getActionCommand();
				selWindow.setVisible(false);
			}
		};
		GridBagLayout dialogLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		selWindow.setLayout(dialogLayout);
		JButton fpga = new JButton("Define the FPGA parameters");
		fpga.setActionCommand(FPGAStr);
		fpga.addActionListener(actionListener);
		fpga.setEnabled(!TheBoard.fpga.FpgaInfoPresent());
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		selWindow.add(fpga, c);
		JButton button;
		for (String comp : FPGAIOInformationContainer.GetComponentTypes()) {
			button = new JButton("Define a " + comp);
			button.setActionCommand(comp);
			button.addActionListener(actionListener);
			c.gridy++;
			selWindow.add(button, c);
		}
		JButton cancel = new JButton("Cancel");
		cancel.setActionCommand(CancelStr);
		cancel.addActionListener(actionListener);
		c.gridy++;
		selWindow.add(cancel, c);
		selWindow.pack();
		selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(),
				selWindow.getHeight()));
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// selWindow.setLocation(mlocation.x,mlocation.y);
		selWindow.setModal(true);
		selWindow.setResizable(false);
		selWindow.setAlwaysOnTop(false);
		selWindow.setVisible(true);
		selWindow.dispose();
		return action_id;
	}
}
