package de.m_marvin.logicsim.ui;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

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
	protected ToolBar titleBar;
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public Editor(Display display) {
		this.shell = new Shell(display);
		this.shell.setLayout(new BorderLayout());
		
		this.titleBar = new ToolBar(shell, SWT.NONE);
		this.titleBar.setLayoutData(new BorderData(SWT.TOP));
		
		Group groupLeft = new Group(shell, SWT.SHADOW_NONE);
		groupLeft.setLayoutData(new BorderData(SWT.LEFT));
		groupLeft.setLayout(new RowLayout(SWT.VERTICAL));
		
		this.toolBar = new ToolBar(groupLeft, SWT.NONE);
		this.toolBar.setLayoutData(new RowData(200, 50));
		ToolItem placePartTool = new ToolItem(this.toolBar, SWT.PUSH);
		placePartTool.setText("TEST");
				
		this.partSelector = new Tree(groupLeft, SWT.SINGLE);
		this.partSelector.setLayoutData(new RowData(200, 400));
		this.partSelector.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (Editor.this.editorArea.getActivePlacement() != null) {
					if (!(event.item.getData() instanceof Class) || Editor.this.editorArea.getActivePlacement().componentClass() == event.item.getData()) {
						Editor.this.editorArea.removeActivePlacement();
						return;
					}
				}
				if (event.item.getData() instanceof Class) {
					Editor.this.editorArea.setActivePlacement(LogicSim.getInstance().getRegistries().getPartEntry((Class<?>) event.item.getData()));
					Editor.this.editorArea.forceFocus();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		updatePartSelector(LogicSim.getInstance().getRegistries());
		
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
	
	public void updatePartSelector(Registries components) {
		this.partSelector.setRedraw(false);
		this.partSelector.clearAll(true);
		Map<ComponentFolder, TreeItem> partFolders = new HashMap<>();
		for (ComponentFolder folder : components.getRegisteredFolderList()) {
			TreeItem item = new TreeItem(this.partSelector, SWT.NONE);
			item.setImage(decodeImage(folder.icon()));
			item.setText(folder.name());
			partFolders.put(folder, item);
		}
		for (ComponentEntry entry : components.getRegisteredPartsList()) {
			TreeItem folderItem = partFolders.get(entry.folder());
			if (folderItem != null) {
				TreeItem item = new TreeItem(folderItem, SWT.NONE);
				item.setImage(decodeImage(entry.icon()));
				item.setText(entry.name());
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
	
}
