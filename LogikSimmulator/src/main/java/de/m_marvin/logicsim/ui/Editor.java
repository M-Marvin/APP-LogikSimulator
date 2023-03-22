package de.m_marvin.logicsim.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent;
import de.m_marvin.logicsim.util.CircuitSerializer;
import de.m_marvin.logicsim.util.Registries;
import de.m_marvin.logicsim.util.Registries.ComponentEntry;
import de.m_marvin.logicsim.util.Registries.ComponentFolder;
import de.m_marvin.univec.impl.Vec2i;

public class Editor {
	
	protected File openFile;
	protected Shell shell;
	protected Menu titleBar;
	protected EditorArea editorArea;
	protected ToolBar toolBar;
	protected Tree partSelector;
	protected EditorArea subCircuitView;
	protected SubCircuitComponent viewComponent;
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public Editor(Display display, Circuit circuit) {
		this.shell = new Shell(display);
		this.shell.setLayout(new BorderLayout());
		this.shell.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {}
			public void focusGained(FocusEvent e) {
				LogicSim.getInstance().setLastInteracted(Editor.this);
			}
		});
		
		if (circuit == null) circuit = new Circuit();
		
		// Top menu bar
		
		this.titleBar = new Menu(shell, SWT.BAR);
		this.shell.setMenuBar(titleBar);
		
		MenuItem fileTab = new MenuItem (titleBar, SWT.CASCADE);
		fileTab.setText (Translator.translate("editor.menu.file"));
		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileTab.setMenu(fileMenu);
		MenuItem saveAsOpt = new MenuItem(fileMenu, SWT.PUSH);
		saveAsOpt.setText(Translator.translate("editor.menu.file.save_as"));
		saveAsOpt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveCircuit(true);
			}
		});
		MenuItem saveOpt = new MenuItem(fileMenu, SWT.PUSH);
		saveOpt.setText(Translator.translate("editor.menu.file.save"));
		saveOpt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveCircuit(false);
			}
		});
		MenuItem loadOpt = new MenuItem(fileMenu, SWT.PUSH);
		loadOpt.setText(Translator.translate("editor.menu.file.load"));
		loadOpt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadCircuit();
			}
		});
		
		// Left tool group
		
		Composite groupLeft = new Composite(shell, SWT.NONE);
		groupLeft.setLayoutData(new BorderData(SWT.LEFT, SWT.DEFAULT, SWT.DEFAULT));
		groupLeft.setLayout(new BorderLayout());
		
		this.toolBar = new ToolBar(groupLeft, SWT.NONE);
		this.toolBar.setLayoutData(new BorderData(SWT.TOP));
		ToolItem placePartTool = new ToolItem(this.toolBar, SWT.PUSH);
		placePartTool.setText("TEST");
		
		this.partSelector = new Tree(groupLeft, SWT.SINGLE);
		this.partSelector.setLayoutData(new BorderData(SWT.CENTER));
		this.partSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (Editor.this.editorArea.getActivePlacement() == event.item.getData()) {
					if (!(event.item.getData() instanceof Class) || Editor.this.editorArea.getActivePlacement().componentClass() == event.item.getData()) {
						Editor.this.editorArea.removeActivePlacement();
						return;
					}
				} else if (event.item.getData() instanceof ComponentEntry entry) {
					Editor.this.editorArea.setActivePlacement(entry);
					Editor.this.editorArea.forceFocus();
				} else {
					Editor.this.editorArea.setActivePlacement(null);
				}
			}
		});
		updatePartSelector();
		
		// Sub-circuit view
		
		Group groupIO = new Group(groupLeft, SWT.NONE);
		groupIO.setLayoutData(new BorderData(SWT.BOTTOM));
		groupIO.setLayout(new BorderLayout());
		groupIO.setText(Translator.translate("editor.sub_circuit_view.title"));
		
		this.subCircuitView = new EditorArea(groupIO);
		this.subCircuitView.setLayoutData(new BorderData(SWT.CENTER, 200, 200));
		this.subCircuitView.setCircuit(new Circuit());
		this.subCircuitView.setAllowEditing(false);
		
		// Editor area
		
		this.editorArea = new EditorArea(shell);
		this.editorArea.setSize(300, 300);
		this.editorArea.setLocation(50, 20);
		this.editorArea.setBackground(new Color(128, 128, 0));
		this.editorArea.getGlCanvas().addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				updateComponentView();
			}
			public void mouseDown(MouseEvent e) {
				updateComponentView();
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		this.editorArea.getGlCanvas().addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				updateComponentView();
			}
			public void keyReleased(KeyEvent e) {}
		});
		
		this.shell.open();
		changeCircuit(circuit);
		updateTitle();
	}
	
	public void updateTitle() {
		this.shell.setText("LogikSimmulator - alpha" + (this.openFile != null ? " - " + this.openFile.toString() : ""));
	}
	
	public void saveCircuit(boolean saveAs) {
		if (saveAs || this.openFile == null || !this.openFile.exists()) {
			FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
			// TODO File-type specific dialog
			String path = fileDialog.open();
			if (path != null) {
				File filePath = new File(path);
				if (filePath.exists()) {
					MessageBox msg = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
					msg.setMessage(Translator.translate("editor.window.info.override_request"));
					msg.setText(Translator.translate("editor.window.info"));
					if (msg.open() == SWT.NO) return;
				}
				this.openFile = filePath;
				try {
					CircuitSerializer.saveCircuit(getCurrentCurcit(), this.openFile);
				} catch (IOException ex) {
					showErrorInfo(this.shell, "editor.window.error.save_file", ex);
					ex.printStackTrace();
				}
				updateTitle();
				LogicSim.getInstance().updateSubCircuitCache();
			}
		}
	}
	
	public void loadCircuit() {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		// TODO File-type specific dialog
		String path = fileDialog.open();
		if (path != null) {
			File filePath = new File(path);
			try {
				changeCircuit(CircuitSerializer.loadCircuit(filePath));
				this.openFile = filePath;
				updateTitle();
			} catch (IOException ex) {
				showErrorInfo(this.shell, "info.error.load_file", ex);
				ex.printStackTrace();
			}
		}
	}

	public static void showErrorInfo(Shell shell, String messageKey, Exception e) {
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		msg.setText(Translator.translate("editor.window.error.title"));
		msg.setMessage(Translator.translate(messageKey, writer.toString()));
		msg.open();
	}
	
	public Circuit getCurrentCurcit() {
		return this.editorArea.getCircuit();
	}
	
	public void changeCircuit(Circuit circuit) {
		this.editorArea.setCircuit(circuit);
		this.subCircuitView.getCircuit().clear();
		this.viewComponent = new SubCircuitComponent(this.subCircuitView.getCircuit(), circuit);
		this.subCircuitView.getCircuit().add(this.viewComponent);
		updateComponentView();
	}
	
	public void updateComponentView() {
		this.viewComponent.updatePinout(false);
		Vec2i position = new Vec2i(-this.viewComponent.getVisualWidth(), -this.viewComponent.getVisualHeight()).div(2).add(Vec2i.fromVec(this.subCircuitView.getSize()).div(2));
		this.viewComponent.setVisualPosition(position);
	}
	
	public void updatePartSelector() {
		this.partSelector.setRedraw(false);
		this.partSelector.removeAll();
		Map<ComponentFolder, TreeItem> partFolders = new HashMap<>();
		for (ComponentFolder folder : Registries.getRegisteredFolderList()) {
			TreeItem item = new TreeItem(this.partSelector, SWT.NONE);
			item.setImage(decodeImage(folder.icon()));
			item.setText(Translator.translate(folder.name()));
			partFolders.put(folder, item);
		}
		for (ComponentEntry entry : Registries.getRegisteredPartsList()) {
			TreeItem folderItem = partFolders.get(entry.folder());
			if (folderItem != null) {
				TreeItem item = new TreeItem(folderItem, SWT.NONE);
				item.setImage(decodeImage(entry.icon()));
				item.setText(Translator.translate(entry.name()));
				item.setData(entry);
			}
		}
		for (ComponentEntry entry : Registries.getCachedSubCircuitParts()) {
			TreeItem folderItem = partFolders.get(entry.folder());
			if (folderItem != null) {
				TreeItem item = new TreeItem(folderItem, SWT.NONE);
				item.setImage(decodeImage(entry.icon()));
				item.setText(Translator.translate(entry.name()));
				item.setData(entry);
			}
		}
		this.partSelector.setRedraw(true);
	}
	
	public SubCircuitComponent getViewComponent() {
		return viewComponent;
	}
	
	public Shell getShell() {
		return shell;
	}

	public void render() {
		this.editorArea.render();
		this.subCircuitView.render();
	}
	
}
