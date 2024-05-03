package de.m_marvin.logicsim.ui.windows;

import java.awt.Color;
import java.awt.TextField;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent;
import de.m_marvin.logicsim.logic.simulator.CircuitProcessor;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.util.CircuitSerializer;
import de.m_marvin.logicsim.util.Registries.ComponentEntry;
import de.m_marvin.openui.core.components.Compound;
import de.m_marvin.openui.core.layout.BorderLayout.BorderSection;
import de.m_marvin.openui.core.layout.BorderLayout.CornerStretch;
import de.m_marvin.openui.core.layout.GridLayout;
import de.m_marvin.openui.flatmono.Window;
import de.m_marvin.openui.flatmono.components.ButtonComponent;
import de.m_marvin.openui.flatmono.components.GraphComponent;
import de.m_marvin.openui.flatmono.components.GroupBox;
import de.m_marvin.openui.flatmono.components.LabelComponent;
import de.m_marvin.renderengine.resources.defimpl.ResourceLocation;
import de.m_marvin.simplelogging.printing.LogType;
import de.m_marvin.simplelogging.printing.Logger;
import de.m_marvin.univec.impl.Vec2i;

/**
 * An editor represents one window in which a circuit can be displayed and modified.
 * 
 * @author Marvin K.
 */
public class Editor extends Window {
	
	public static final String SET_MAIN_CIRCUIT_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAA6SURBVChTY2Qo3/6fAR0ofWNgSA9mBDGZwBx0TAoAG2NhYYFhzdevXxkuX77MyATjoGOiAaVWMDAAAGWnNDjnjwLQAAAAAElFTkSuQmCC";
	public static final String START_SIMULATION_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABESURBVChTY2D4z/EfjHEARgxJxh+MUBYYYCqAAahC3AqgAKcC3vM/wDSGApjEZyOgHBDAFaBLwAHvOYb/IAzlogEGBgAn2R2n/6vpZQAAAABJRU5ErkJggg==";
	public static final String PAUSE_SIMULATION_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAsSURBVChTYzxyjuE/AxBcucTAkJHAwAhiI4sxgRj4wNBQAPIKCCMDhBgDAwCAMw/f3Kx0cwAAAABJRU5ErkJggg==";
	public static final String STOP_SIMULATION_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAsSURBVChTYyAK/Odg+I+Ot7Mx/AfJMYFV4AF0UMAIImAOQgeevyDyeAADAwCL3A7k6GJNugAAAABJRU5ErkJggg==";
	
	protected Shell shell;
	protected Menu titleBar;
	protected EditorArea editorArea;
	protected ButtonComponent setMainButton;
	protected ButtonComponent simulationStartButton;
	protected ButtonComponent simulationPauseButton;
	protected ButtonComponent simulationStopButton;
	protected GroupBox partSelector;
	protected EditorArea subCircuitView;
	protected SubCircuitComponent viewComponent;
	protected GraphComponent statusGraph;
	protected long lastGrapghTime;
	protected LabelComponent tpsLabel;
	protected LabelComponent executionTimeLabel;
	protected LabelComponent simulationStatusLabel;
	protected boolean simulationStatus;
	protected TextField tpsLimitField;
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}

	public static void showErrorInfo(Shell shell, String messageKey, Throwable e) {
		String message = null;
		if (e instanceof FileNotFoundException) {
			message = e.getMessage();
		} else {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			message = writer.toString();
		}
		MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		msg.setText(Translator.translate("editor.window.error.title"));
		msg.setMessage(Translator.translate(messageKey, message));
		msg.open();
	}
	
	@Override
	protected void initUI() {
		
		getRootComponent().setLayout(new de.m_marvin.openui.core.layout.BorderLayout(CornerStretch.NONE));
		
		GroupBox sidebarGroup = new GroupBox();
		sidebarGroup.setLayoutData(new de.m_marvin.openui.core.layout.BorderLayout.BorderLayoutData(BorderSection.LEFT));
		sidebarGroup.setLayout(new GridLayout());
		getRootComponent().addComponent(sidebarGroup);
		
		Compound<ResourceLocation> controllBar = new Compound<>();
		controllBar.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(0, 0));
		controllBar.setLayout(new de.m_marvin.openui.core.layout.GridLayout());
		sidebarGroup.addComponent(controllBar);
		
		this.setMainButton = new ButtonComponent("main");
		this.setMainButton.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(0, 0));
		this.setMainButton.setAction(() -> {
			CircuitProcessor processor = LogicSim.getInstance().getCircuitProcessor();
			if (!processor.isExecuting(getCurrentCurcit())) {
				processor.addProcess(null, getCurrentCurcit());
			} else {
				MessageBox msg = new MessageBox(shell, SWT.ICON_WARNING);
				msg.setMessage(Translator.translate("editor.tool.set_main_circuit.is_already_running"));
				msg.setText(Translator.translate("editor.window.warning"));
				msg.open();
			}
		});
		controllBar.addComponent(this.setMainButton);
		this.simulationStartButton = new ButtonComponent("start");
		this.simulationStartButton.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(1, 0));
		this.simulationStartButton.setAction(() -> LogicSim.getInstance().getCircuitProcessor().start());
		controllBar.addComponent(this.simulationStartButton);
		this.simulationPauseButton = new ButtonComponent("pause");
		this.simulationPauseButton.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(2, 0));
		this.simulationPauseButton.setAction(() -> LogicSim.getInstance().getCircuitProcessor().pause());
		controllBar.addComponent(this.simulationPauseButton);
		this.simulationStopButton = new ButtonComponent("stop");
		this.simulationStopButton.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(3, 0));
		this.simulationStopButton.setAction(() -> LogicSim.getInstance().getCircuitProcessor().stop());
		controllBar.addComponent(this.simulationStopButton);
		
		controllBar.autoSetMaxAndMinSize();
		
//		setMainCircuit.setImage(decodeImage(SET_MAIN_CIRCUIT_ICON_B64));
//		startSimulation.setImage(decodeImage(START_SIMULATION_ICON_B64));
//		pauseSimulation.setImage(decodeImage(PAUSE_SIMULATION_ICON_B64));
//		stopSimulation.setImage(decodeImage(STOP_SIMULATION_ICON_B64));
		
		this.partSelector = new GroupBox(Color.WHITE, Color.GRAY);
		this.partSelector.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(0, 1));
		this.partSelector.setSizeMin(new Vec2i(200, 200));
		sidebarGroup.addComponent(this.partSelector);
		
		GroupBox statusGroup = new GroupBox();
		statusGroup.setLayoutData(new de.m_marvin.openui.core.layout.GridLayout.GridLayoutData(0, 2));
		statusGroup.setLayout(new de.m_marvin.openui.core.layout.GridLayout());
		sidebarGroup.addComponent(statusGroup);
		
		this.statusGraph = new GraphComponent(0, 100, 0, 100);
		this.statusGraph.setLayoutData(new GridLayout.GridLayoutData(0, 0));
		statusGroup.addComponent(this.statusGraph);
		
		statusGroup.autoSetMaxAndMinSize();
		
		sidebarGroup.autoSetMaxAndMinSize();
		
		Compound<ResourceLocation> centerGroup = new Compound<>();
		centerGroup.setLayoutData(new de.m_marvin.openui.core.layout.BorderLayout.BorderLayoutData(BorderSection.CENTERED));
		centerGroup.setLayout(new de.m_marvin.openui.core.layout.BorderLayout());
		getRootComponent().addComponent(centerGroup);
		
		this.editorArea = new EditorArea();
		this.editorArea.setLayoutData(new de.m_marvin.openui.core.layout.BorderLayout.BorderLayoutData(BorderSection.CENTERED));
		centerGroup.addComponent(this.editorArea);
		
		centerGroup.autoSetMaxAndMinSize();
		
		getRootComponent().autoSetMaxAndMinSize();
		autoSetMinAndMaxSize();
		
	}
	
	public Editor(Display display, Circuit circuit) {
		
		super("DDD");
		
		this.shell = new Shell(display);
		this.shell.setImage(decodeImage(LogicSim.LOGIC_SIM_ICON));
		this.shell.setLayout(new BorderLayout());
		this.shell.addListener(SWT.FocusIn, (e) -> LogicSim.getInstance().setLastInteracted(Editor.this));
		
		if (circuit == null) circuit = new Circuit();
		
		// Top menu bar
		
		this.titleBar = new Menu(shell, SWT.BAR);
		this.shell.setMenuBar(titleBar);
		
//		MenuItem fileTab = new MenuItem (titleBar, SWT.CASCADE);
//		fileTab.setText (Translator.translate("editor.menu.file"));
//		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
//		fileTab.setMenu(fileMenu);
//		MenuItem newOpt = new MenuItem(fileMenu, SWT.PUSH);
//		newOpt.setText(Translator.translate("editor.menu.file.new_file"));
//		newOpt.addListener(SWT.Selection, (e) -> newCircuit());
//		MenuItem saveAsOpt = new MenuItem(fileMenu, SWT.PUSH);
//		saveAsOpt.setText(Translator.translate("editor.menu.file.save_as"));
//		saveAsOpt.addListener(SWT.Selection, (e) -> saveCircuit(true));
//		MenuItem saveOpt = new MenuItem(fileMenu, SWT.PUSH);
//		saveOpt.setText(Translator.translate("editor.menu.file.save"));
//		saveOpt.addListener(SWT.Selection, (e) -> saveCircuit(false));
//		MenuItem loadOpt = new MenuItem(fileMenu, SWT.PUSH);
//		loadOpt.setText(Translator.translate("editor.menu.file.load"));
//		loadOpt.addListener(SWT.Selection, (e) -> loadCircuit(false));
//		MenuItem loadInEditorOpt = new MenuItem(fileMenu, SWT.PUSH);
//		loadInEditorOpt.setText(Translator.translate("editor.menu.file.load_in_editor"));
//		loadInEditorOpt.addListener(SWT.Selection, (e) -> loadCircuit(true));
//		MenuItem refreshComponents = new MenuItem(fileMenu, SWT.PUSH);
//		refreshComponents.setText(Translator.translate("editor.menu.file.refresh_components"));
//		refreshComponents.addListener(SWT.Selection, (e) -> updatePartSelector());
//		
//		MenuItem viewsTab = new MenuItem (titleBar, SWT.CASCADE);
//		viewsTab.setText (Translator.translate("editor.menu.views"));
//		Menu viewsMenu = new Menu(shell, SWT.DROP_DOWN);
//		viewsTab.setMenu(viewsMenu);
//		MenuItem circuitsView = new MenuItem(viewsMenu, SWT.PUSH);
//		circuitsView.setText(Translator.translate("editor.menu.views.circuit_viewer"));
//		circuitsView.addListener(SWT.Selection, (e) -> LogicSim.getInstance().openCircuitViewer());
//		MenuItem circuitOpt = new MenuItem(viewsMenu, SWT.PUSH);
//		circuitOpt.setText(Translator.translate("editor.menu.views.circuit_options"));
//		circuitOpt.addListener(SWT.Selection, (e) -> LogicSim.getInstance().openCircuitOptions(this.editorArea.getCircuit()));
//		MenuItem editorWindow = new MenuItem(viewsMenu, SWT.PUSH);
//		editorWindow.setText(Translator.translate("editor.menu.views.circuit_editor"));
//		editorWindow.addListener(SWT.Selection, (e) -> LogicSim.getInstance().openEditor(null));
//		
//		MenuItem languageTab = new MenuItem(titleBar, SWT.CASCADE);
//		languageTab.setText(Translator.translate("editor.menu.language"));
//		Menu languageMenu = new Menu(shell, SWT.DROP_DOWN);
//		languageTab.setMenu(languageMenu);
//		Translator.getAvailableLanguages().forEach(lang -> {
//			MenuItem languageSelecor = new MenuItem(languageMenu, SWT.PUSH);
//			languageSelecor.setText(Translator.resolveLangName(lang));
//			languageSelecor.addListener(SWT.Selection, e -> {
//				LogicSim.getInstance().setLanguage(lang);
//				MessageBox msg = new MessageBox(shell, SWT.ICON_WARNING);
//				msg.setMessage(Translator.translate("editor.window.info.language_change"));
//				msg.setText(Translator.translate("editor.window.info.title"));	
//				msg.open();
//			});
//		});
//		
//		// Left tool group
//		
//		Composite groupLeft = new Composite(shell, SWT.NONE);
//		groupLeft.setLayoutData(new BorderData(SWT.LEFT, SWT.DEFAULT, SWT.DEFAULT));
//		groupLeft.setLayout(new BorderLayout());
//		

//		
//		this.partSelector = new Tree(groupLeft, SWT.SINGLE);
//		this.partSelector.setLayoutData(new BorderData(SWT.CENTER));
//		this.partSelector.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent event) {
//				if (Editor.this.editorArea.getActivePlacement() == event.item.getData()) {
//					if (!(event.item.getData() instanceof Class) || Editor.this.editorArea.getActivePlacement().componentClass() == event.item.getData()) {
//						Editor.this.editorArea.removeActivePlacement();
//						return;
//					}
//				} else if (event.item.getData() instanceof ComponentEntry entry) {
//					Editor.this.editorArea.setActivePlacement(entry);
//					Editor.this.editorArea.getGlCanvas().forceFocus();
//				} else {
//					Editor.this.editorArea.setActivePlacement(null);
//				}
//			}
//		});
//		
//		// Additional sub-groups
//		
//		Composite subGroups = new Composite(groupLeft, SWT.NONE);
//		subGroups.setLayoutData(new BorderData(SWT.BOTTOM));
//		subGroups.setLayout(new GridLayout(1, true));
//		
//		// Sub-circuit view
//		
//		Group groupIO = new Group(subGroups, SWT.NONE);
//		groupIO.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		groupIO.setLayout(new BorderLayout());
//		groupIO.setText(Translator.translate("editor.sub_circuit_view.title"));
//		
//		this.subCircuitView = new EditorArea(groupIO, false);
////		this.subCircuitView.setLayoutData(new BorderData(SWT.CENTER, 200, 200));
//		this.subCircuitView.setCircuit(new Circuit(true));
//		this.subCircuitView.setAllowEditing(false);
//		this.subCircuitView.setAreaSize(new Vec2i(200, 200));
//		
//		// Simulation control view
//		
//		Group groupSimulation = new Group(subGroups, SWT.NONE);
//		groupSimulation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		groupSimulation.setLayout(new RowLayout());
//		groupSimulation.setText(Translator.translate("editor.simulation_view.title"));
//		
//		this.statusGraph = new ValueHistoryGraph(groupSimulation, 0F, 10F, 20);
//		this.statusGraph.setLayoutData(new RowData(200, 100));
//		this.statusGraph.addVariable("CPU", new Color(255, 0, 0));
//		this.statusGraph.addVariable("TPS", new Color(0, 255, 0));
//		
//		Composite tpsStatus = new Composite(groupSimulation, SWT.NONE);
//		
//		this.tpsLabel = new Label(tpsStatus, SWT.NONE);
//		this.tpsLabel.setBounds(5, 10, 150, 16);
//		this.tpsLabel.setText("N/A tps (N/A ms)");
//
//		this.executionTimeLabel = new Label(tpsStatus, SWT.NONE);
//		this.executionTimeLabel.setBounds(5, 30, 150, 16);
//		this.executionTimeLabel.setText("Execution time N/Ams");
//		
//		Label tpsLimitLabel = new Label(tpsStatus, SWT.NONE);
//		tpsLimitLabel.setBounds(5, 50, 150, 16);
//		tpsLimitLabel.setText(Translator.translate("editor.simulation_view.tps_limit"));
//		tpsLimitLabel.pack();
//		
//		this.tpsLimitField = new Spinner(tpsStatus, 0);
//		this.tpsLimitField.setBounds(80, 48, 80, 20);
//		this.tpsLimitField.setMaximum(10000);
//		this.tpsLimitField.setMinimum(0);
//		this.tpsLimitField.setSelection(LogicSim.getInstance().getSimulationMonitor().getTPSLimit());
//		
//		this.simulationStatusLabel = new Label(tpsStatus, SWT.NONE);
//		this.simulationStatusLabel.setBounds(5, 70, 150, 30);
//		this.simulationStatusLabel.setText("N/A");
//		FontData font = this.simulationStatusLabel.getFont().getFontData()[0];
//		font.setHeight(12);
//		font.setStyle(SWT.BOLD);
//		this.simulationStatusLabel.setFont(new Font(display, font));
//		this.simulationStatus = !LogicSim.getInstance().getSimulationMonitor().isProcessorActive(); // Will cause the update method to change the text
//		
//		// Editor area
//		
//		this.editorArea = new EditorArea(shell);
//		this.editorArea.getGlCanvas().addMouseListener(new MouseListener() {
//			public void mouseUp(MouseEvent e) {
//				updateComponentView();
//			}
//			public void mouseDown(MouseEvent e) {
//				updateComponentView();
//			}
//			public void mouseDoubleClick(MouseEvent e) {}
//		});
//		this.editorArea.getGlCanvas().addKeyListener(new KeyListener() {
//			public void keyPressed(KeyEvent e) {
//				updateComponentView();
//			}
//			public void keyReleased(KeyEvent e) {}
//		});
//		
//		this.shell.open();
//		this.editorArea.setAreaSize(new Vec2i(10000, 10000));
//		this.editorArea.setWarningSupplier(() -> {
//			Optional<CircuitProcessInfo> processInfo = LogicSim.getInstance().getSimulationMonitor().getProcessForCircuit(this.editorArea.getCircuit());
//			if (processInfo.isEmpty()) return new ArrayList<>();
//			return processInfo.get().warnings();
//		});
//		changeCircuit(circuit);
//		updatePartSelector();
		
	}
	
	public void updateUI() {
		
//		boolean instanced = LogicSim.getInstance().getCircuitProcessor().holdsCircuit(this.editorArea.getCircuit());
//		this.shell.setText(Translator.translate("editor.title", LogicSim.getVersion()) + (this.editorArea.getCircuit().getCircuitFile() != null ? " - " + this.editorArea.getCircuit().getCircuitFile().toString() : "") + (instanced ? Translator.translate("editor.title.instanced") : ""));
//		
//		SimulationMonitor monitor = LogicSim.getInstance().getSimulationMonitor();
//		Optional<CircuitProcessInfo> processInfo = monitor.getProcessForCircuit(this.editorArea.getCircuit());
//		Optional<CircuitProcessorInfo> processorInfo = monitor.getProcessorForCircuit(this.editorArea.getCircuit());
//		
//		int executionTime = processInfo.isPresent() ? processInfo.get().executionTime().get() : 0;
//		int currentTps = processorInfo.isPresent() ? processorInfo.get().tps().get() : 0;
//		
//		this.executionTimeLabel.setText(Translator.translate("editor.simulation_view.execution_time", executionTime));
//		this.tpsLabel.setText(Translator.translate("editor.simulation_view.update_rate", currentTps));
//		
//		int tpsLimit = monitor.getTPSLimit();
//		String input = this.tpsLimitField.getText();
//		int enteredTpsLimit;
//		try {
//			enteredTpsLimit = input.isEmpty() ? 1 : Integer.parseInt(this.tpsLimitField.getText());
//		} catch (NumberFormatException e) {
//			enteredTpsLimit = 0;
//		}
//		
//		if (tpsLimit != enteredTpsLimit) {
//			if (this.tpsLimitField.isFocusControl()) {
//				monitor.setTPSLimit(enteredTpsLimit);
//				tpsLimit = enteredTpsLimit;
//			} else {
//				this.tpsLimitField.setSelection(tpsLimit);
//			}
//		}
//		
//		boolean simulationActive = processInfo.isPresent() ? processInfo.get().executing().get() : false;
//		if (simulationActive != this.simulationStatus) {
//			this.simulationStatus = simulationActive;
//			this.simulationStatusLabel.setText(Translator.translate(simulationActive ? "ACTIVE" : "INACTIVE"));
//			this.simulationStatusLabel.setForeground(this.shell.getDisplay().getSystemColor(simulationActive ? SWT.COLOR_GREEN : SWT.COLOR_RED));
//		}
		
	}

	public void updateGraphics() {
		
//		SimulationMonitor monitor = LogicSim.getInstance().getSimulationMonitor();
//		Optional<CircuitProcessorInfo> processorInfo = monitor.getProcessorForCircuit(this.editorArea.getCircuit());
//		
//		int currentTps = processorInfo.isPresent() ? processorInfo.get().tps().get() : 0;
//		int tpsLimit = monitor.getTPSLimit();
//		
//		float cpuLoad = (float) monitor.getCPULoad();
//		float tpsState = currentTps / (float) tpsLimit;
//		
//		if (System.currentTimeMillis() - lastGrapghTime >= 1000) {
//			
//			this.lastGrapghTime = System.currentTimeMillis();
//			this.statusGraph.nextColumn();
//			this.statusGraph.putData("CPU", Math.max(Math.min(cpuLoad, 1), 0) * 10F);
//			this.statusGraph.putData("TPS", Math.max(Math.min(tpsState, 1), 0) * 10F);
//			
//		}
//				
//		this.editorArea.render();
//		this.subCircuitView.render();
//		this.statusGraph.render();
		
	}
	
	public void newCircuit() {
		MessageBox msg = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		msg.setMessage(Translator.translate("editor.window.info.clear_circuit_request"));
		msg.setText(Translator.translate("editor.window.info.title"));
		if (msg.open() == SWT.NO) return;
		this.changeCircuit(new Circuit());
	}
	
	public void saveCircuit(boolean saveAs) {
		if (saveAs || this.editorArea.getCircuit().getCircuitFile() == null || !this.editorArea.getCircuit().getCircuitFile().exists()) {
			FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
			fileDialog.setFilterExtensions(new String[] {"*." + LogicSim.CIRCUIT_FILE_EXTENSION});
			String path = fileDialog.open();
			if (path != null) {
				File filePath = new File(path);
				if (filePath.exists()) {
					MessageBox msg = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
					msg.setMessage(Translator.translate("editor.window.info.override_request"));
					msg.setText(Translator.translate("editor.window.info.title"));
					if (msg.open() == SWT.NO) return;
				}
				this.editorArea.getCircuit().setCircuitFile(filePath);
				try {
					CircuitSerializer.saveCircuit(getCurrentCurcit(), this.editorArea.getCircuit().getCircuitFile());
				} catch (IOException ex) {
					showErrorInfo(this.shell, "editor.window.error.save_file", ex);
					Logger.defaultLogger().printException(LogType.WARN, ex);
				}
				LogicSim.getInstance().updateSubCircuitCache();
				updateUI();
			}
		}
		if (this.editorArea.getCircuit().getCircuitFile() == null) return;
		try {
			CircuitSerializer.saveCircuit(getCurrentCurcit(), this.editorArea.getCircuit().getCircuitFile());
		} catch (IOException ex) {
			showErrorInfo(this.shell, "editor.window.error.save_file", ex);
			Logger.defaultLogger().printException(LogType.ERROR, ex);
		}
		LogicSim.getInstance().updateSubCircuitCache();
	}
	
	public void loadCircuit(boolean newWindow) {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		fileDialog.setFilterExtensions(new String[] {"*." + LogicSim.CIRCUIT_FILE_EXTENSION});
		String path = fileDialog.open();
		if (path != null) {
			File filePath = new File(path);
			try {
				if (newWindow) {
					LogicSim.getInstance().openEditor(CircuitSerializer.loadCircuit(filePath));
				} else {
					changeCircuit(CircuitSerializer.loadCircuit(filePath));
					updateUI();
				}
			} catch (IOException ex) {
				showErrorInfo(this.shell, "info.error.load_file", ex);
				Logger.defaultLogger().printException(LogType.WARN, ex);
			}
		}
		updatePartSelector();
	}
	
	public Circuit getCurrentCurcit() {
		return this.editorArea.getCircuit();
	}
	
	public void changeCircuit(Circuit circuit) {
		this.editorArea.setCircuit(circuit);
		this.subCircuitView.getCircuit().clear();
		this.viewComponent = new SubCircuitComponent(this.subCircuitView.getCircuit());
		this.viewComponent.setSubCircuit(circuit);
		this.subCircuitView.getCircuit().add(this.viewComponent);
		this.subCircuitView.getCircuit().setCircuitFile(circuit.getCircuitFile());
		updateComponentView();
	}
	
	public void updateComponentView() {
		this.viewComponent.updatePinout();
		Vec2i position = new Vec2i(-this.viewComponent.getVisualWidth(), -this.viewComponent.getVisualHeight()).div(2).add(Vec2i.fromVec(this.subCircuitView.getSize()).div(2));
		this.viewComponent.setVisualPosition(position);
	}

	public Optional<File> getCircuitFolder() {
		File circuitPath = this.getCurrentCurcit().getCircuitFile();
		if (circuitPath != null) {
			return Optional.of(circuitPath.getParentFile());
		}
		return Optional.empty();
	}
	
	public Collection<ComponentEntry> loadUserCircuits() {
		Optional<File> circuitFolder = getCircuitFolder();
		List<ComponentEntry> userIcs = new ArrayList<>();
		if (circuitFolder.isPresent()) {
			LogicSim.scanForFiles(getCircuitFolder().get(), file -> {
				if (!LogicSim.isCircuitFile(file)) return;
				String name = Translator.translate(LogicSim.getFileName(file.getName()));
				userIcs.add(new ComponentEntry(LogicSim.getInstance().userIcFolder, SubCircuitComponent.class, Component::placeClick, (circuit, pos) -> SubCircuitComponent.coursorMove(circuit, pos, file), Component::abbortPlacement, name, SubCircuitComponent.ICON_B64));
			}, 2);
		}
		return userIcs;
	}
	
	public void updatePartSelector() {
//		this.partSelector.setRedraw(false);
//		this.partSelector.removeAll();
//		Map<ComponentFolder, TreeItem> partFolders = new HashMap<>();
//		for (ComponentFolder folder : Registries.getRegisteredFolderList()) {
//			TreeItem item = new TreeItem(this.partSelector, SWT.NONE);
//			item.setImage(decodeImage(folder.icon()));
//			item.setText(Translator.translate(folder.name()));
//			partFolders.put(folder, item);
//		}
//		for (ComponentEntry entry : Registries.getRegisteredPartsList()) {
//			TreeItem folderItem = partFolders.get(entry.folder());
//			if (folderItem != null) {
//				TreeItem item = new TreeItem(folderItem, SWT.NONE);
//				item.setImage(decodeImage(entry.icon()));
//				item.setText(Translator.translate(entry.name()));
//				item.setData(entry);
//			}
//		}
//		for (ComponentEntry entry : Registries.getCachedSubCircuitParts()) {
//			TreeItem folderItem = partFolders.get(entry.folder());
//			if (folderItem != null) {
//				TreeItem item = new TreeItem(folderItem, SWT.NONE);
//				item.setImage(decodeImage(entry.icon()));
//				item.setText(Translator.translate(entry.name()));
//				item.setData(entry);
//			}
//		}
//		for (ComponentEntry entry : loadUserCircuits()) {
//			TreeItem folderItem = partFolders.get(entry.folder());
//			if (folderItem != null) {
//				TreeItem item = new TreeItem(folderItem, SWT.NONE);
//				item.setImage(decodeImage(entry.icon()));
//				item.setText(Translator.translate(entry.name()));
//				item.setData(entry);
//			}
//		}
//		this.partSelector.setRedraw(true);
	}
	
	public SubCircuitComponent getViewComponent() {
		return viewComponent;
	}
	
	public Shell getShell() {
		return shell;
	}
	
}
