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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
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

import de.m_marvin.logicsim.CircuitSerializer;
import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.Registries;
import de.m_marvin.logicsim.Registries.ComponentEntry;
import de.m_marvin.logicsim.Registries.ComponentFolder;
import de.m_marvin.logicsim.logic.Circuit;

public class Editor {
	
	protected Shell shell;
	protected EditorArea editorArea;
	protected Tree partSelector;
	protected ToolBar toolBar;
	protected Menu titleBar;
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public Editor(Display display) {
		this.shell = new Shell(display);
		this.shell.setLayout(new BorderLayout());
		
		// Top menu bar
		
		this.titleBar = new Menu(shell, SWT.BAR);
		this.shell.setMenuBar(titleBar);
		
		MenuItem fileTab = new MenuItem (titleBar, SWT.CASCADE);
		fileTab.setText (Translator.translate("editor.menu.file"));
		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileTab.setMenu(fileMenu);
		MenuItem saveOpt = new MenuItem(fileMenu, SWT.PUSH);
		saveOpt.setText(Translator.translate("editor.menu.file.save"));
		saveOpt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
				// TODO
				String filePath = fileDialog.open();
				if (filePath != null) {
					try {
						CircuitSerializer.saveCircuit(LogicSim.getInstance().getCircuit(), new File(filePath));
					} catch (IOException ex) {
						showErrorInfo("info.error.save_file", ex);
						ex.printStackTrace();
					}
				}
			}
		});
		MenuItem loadOpt = new MenuItem(fileMenu, SWT.PUSH);
		loadOpt.setText(Translator.translate("editor.menu.file.load"));
		loadOpt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
				// TODO
				String filePath = fileDialog.open();
				if (filePath != null) {
					try {
						LogicSim.getInstance().setCircuit(CircuitSerializer.loadCircuit(new File(filePath)));
					} catch (IOException ex) {
						showErrorInfo("info.error.load_file", ex);
						ex.printStackTrace();
					}
				}
			}
		});
		
		// Left tool group
		
		Group groupLeft = new Group(shell, SWT.SHADOW_NONE);
		groupLeft.setLayoutData(new BorderData(SWT.LEFT));
		groupLeft.setLayout(new RowLayout(SWT.VERTICAL));
		
		this.toolBar = new ToolBar(groupLeft, SWT.NONE);
		this.toolBar.setLayoutData(new RowData(200, 50));
		ToolItem placePartTool = new ToolItem(this.toolBar, SWT.PUSH);
		placePartTool.setText("TEST");
		
		this.partSelector = new Tree(groupLeft, SWT.SINGLE);
		this.partSelector.setLayoutData(new RowData(200, 400));
		this.partSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (Editor.this.editorArea.getActivePlacement() != null) {
					if (!(event.item.getData() instanceof Class) || Editor.this.editorArea.getActivePlacement().componentClass() == event.item.getData()) {
						Editor.this.editorArea.removeActivePlacement();
						return;
					}
				}
				if (event.item.getData() instanceof Class) {
					Editor.this.editorArea.setActivePlacement(Registries.getPartEntry((Class<?>) event.item.getData()));
					Editor.this.editorArea.forceFocus();
				}
			}
		});
		updatePartSelector();
		
		// Editor area
		
		this.editorArea = new EditorArea(shell);
		this.editorArea.setSize(300, 300);
		this.editorArea.setLocation(50, 20);
		this.editorArea.setBackground(new Color(128, 128, 0));
		this.editorArea.setCircuit(LogicSim.getInstance().getCircuit());
		
		this.shell.open();
	}
	
	public void changeCircuit(Circuit circuit) {
		this.editorArea.setCircuit(circuit);
	}
	
	public void updatePartSelector() {
		this.partSelector.setRedraw(false);
		this.partSelector.clearAll(true);
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
				item.setData(entry.componentClass());
			}
		}
		this.partSelector.setRedraw(true);
	}
	
	public Shell getShell() {
		return shell;
	}

	public void render() {
		this.editorArea.render();
	}
	
	public void showErrorInfo(String messageKey, Exception e) {
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		msg.setText("An Exception was thrown!");
		msg.setMessage(Translator.translate(messageKey, writer.toString()));
		msg.open();
	}
	
}
