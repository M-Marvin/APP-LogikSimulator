package de.m_marvin.logicsim;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.swt.widgets.Display;

import de.m_marvin.commandlineparser.CommandLineParser;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.parts.BoolConstComponent;
import de.m_marvin.logicsim.logic.parts.BoolInputComponent;
import de.m_marvin.logicsim.logic.parts.BoolOutputComponent;
import de.m_marvin.logicsim.logic.parts.BusConstComponent;
import de.m_marvin.logicsim.logic.parts.BusInputComponent;
import de.m_marvin.logicsim.logic.parts.BusOutputComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.AndGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NandGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NorGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.OrGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.XorGateComponent;
import de.m_marvin.logicsim.logic.parts.NotGateComponent;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent;
import de.m_marvin.logicsim.logic.simulator.CircuitProcessor;
import de.m_marvin.logicsim.logic.simulator.SimulationMonitor;
import de.m_marvin.logicsim.logic.wires.ConnectorWire;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.ui.windows.CircuitOptions;
import de.m_marvin.logicsim.ui.windows.CircuitViewer;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.logicsim.util.CircuitSerializer;
import de.m_marvin.logicsim.util.ConfigFile;
import de.m_marvin.logicsim.util.Registries;
import de.m_marvin.logicsim.util.Registries.ComponentFolder;

public class LogicSim {
	
	// TODO Features planed
	// - Zoom
	// - Cache für Werte-Abfrage (?)
	// - Komponenten zur interaktion mit Dateien, Grphischer Darstellung, Tastatureingabe etc
	
	public static final String LOGIC_SIM_ICON = "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAA40lEQVRoge2YwQ2DMAxF3Yqhshy3LsIQrNFzJJbIBu0BoVZIUeLEIf7B78IFJH/5/9iEyDCqeGS+92laRZxkfc8rqmiJCejNVPhdbna4sLMWE9ArtGxua6EfrzW/W9t7fy6zmAXhOwAvoN5Chy06ERNw9qjaU6l0F7pqDtgupJ76EP/hnMvOSgiBiIi891V2hO8AvABRCx22ECR5KsF3QPscODNeB+AFwM8BUQENGH8X0j4HksB3QPscGD8D8AJEb+bsf6AAeAE2B3oTCxB7LxeCPQfgb+ZKM6BGEHwGTIBhgPMFj3A0g04XsBUAAAAASUVORK5CYII=";
	public static final String CIRCUIT_FILE_EXTENSION = "lcf";
	
	private static LogicSim INSTANCE;
	
	protected File subCircuitFolder;
	protected File configFile;
	
	protected boolean shouldTerminate;
	protected Display display;
	protected CircuitProcessor processor;
	protected SimulationMonitor simulationMonitor;
	protected Thread uiLogicThread;
	protected List<Editor> openEditorWindows = new ArrayList<>();
	protected CircuitViewer circuitViewWindow;
	protected Editor lastInteractedEditor;
	
	public static void main(String... args) {
		
		LogicSim logicSim = new LogicSim();
		
		CommandLineParser parser = new CommandLineParser();
		parser.addOption("config-file", "");
		parser.addOption("sub-circuit-folder", "");
		parser.addOption("open-files", "");
		parser.parseInput(args);
		logicSim.subCircuitFolder = new File(parser.getOption("sub-circuit-folder"));
		logicSim.configFile = new File(parser.getOption("config-file"));
		
		String[] circuitFileList = parser.getOption("open-files").split(",");
		List<File> filesToOpen = new ArrayList<>();
		for (String fileToOpen : circuitFileList) {
			if (fileToOpen.isEmpty()) continue;
			File file = new File(fileToOpen);
			if (isCircuitFile(file)) filesToOpen.add(file);
		}
		
		logicSim.start(filesToOpen.toArray(new File[] {}));
		
	}
	
	public static String getVersion() {
		try {
			URL url = LogicSim.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(url.openStream());
			Attributes attr = manifest.getMainAttributes();
			return attr.getValue("Implementation-Version");
		} catch (IOException e) {
			return "N/A";
		}
	}
	
	public LogicSim() {
		INSTANCE = this;
	}
	
	public File getSubCircuitFolder() {
		return subCircuitFolder;
	}
	
	public static LogicSim getInstance() {
		return INSTANCE;
	}

	public Display getDisplay() {
		return this.display;
	}
	
	public List<Editor> getOpenEditors() {
		return openEditorWindows;
	}
	
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
	
	public void terminate() {
		this.shouldTerminate = true;
	}
	
	public CircuitProcessor getCircuitProcessor() {
		return processor;
	}
	
	public SimulationMonitor getSimulationMonitor() {
		return simulationMonitor;
	}
	
	public void setLanguage(String lang) {
		ConfigFile.setValue(configFile, "language", lang);
		Translator.changeLanguage(lang);
	}
		
	private void start(File... filesToOpen) {

		registerIncludedParts();
		updateSubCircuitCache();
		
		Translator.changeLanguage(ConfigFile.getValue(configFile, "language", "lang_en"));
		
		this.display = new Display();
		this.processor = new CircuitProcessor();
		this.simulationMonitor = new SimulationMonitor(processor);
		
		if (filesToOpen.length == 0) {
			System.out.println("Open default editor window");
			openEditor(null);
		} else {
			System.out.println("Open editor windows for passed files");
			for (int i = 0; i < filesToOpen.length; i++) {
				try {
					openEditor(CircuitSerializer.loadCircuit(filesToOpen[i]));
				} catch (IOException e) {
					System.err.println("Failed to load file '" + filesToOpen[i] + "'!");
					e.printStackTrace();
				}
			}
		}

		System.out.println("Start ui-logic thread");
		this.uiLogicThread = new Thread(() -> {
			long lastTickTime = 0;
			long tickTime = 0;
			float tickRateDelta = 0;
			while (!this.shouldTerminate()) {
				try {
					lastTickTime = tickTime;
					tickTime = System.currentTimeMillis();
					tickRateDelta += (tickTime - lastTickTime) / 50F;
					if (tickRateDelta > 20 || tickRateDelta < 0) tickRateDelta = 0;
					if (tickRateDelta > 1) {
						tickRateDelta -= 1;
						updateGraphics();
					} else {
						Thread.sleep(25);
					}
				} catch (InterruptedException e) {}
			}
			TextRenderer.cleanUpOpenGL();
			System.out.println("ui-logic thread terminated!");
		}, "ui-logic");
		this.uiLogicThread.start();
		
		System.out.println("Enter ui main loop");
		long lastTickTime = 0;
		long tickTime = 0;
		float tickRateDelta = 0;
		while (!shouldTerminate()) {
			try {
				lastTickTime = tickTime;
				tickTime = System.currentTimeMillis();
				tickRateDelta += (tickTime - lastTickTime) / 10F;
				if (tickRateDelta > 20 || tickRateDelta < 0) tickRateDelta = 0;
				if (tickRateDelta > 1) {
					tickRateDelta -= 1;
					updateUI();
				} else {
					Thread.sleep(5);
				}
			} catch (InterruptedException e) {}
		}
		System.out.println("Exit ui main loop!");
		
		this.display.dispose();
		this.processor.terminate();
		
		System.out.println("Main thread terminated!");
		
	}
	
	public void openEditor(Circuit circuit) {
		synchronized (this.openEditorWindows) {
			this.openEditorWindows.add(new Editor(display, circuit));
		}
	}
	
	public void openCircuitViewer() {
		if (this.circuitViewWindow == null || this.circuitViewWindow.getShell().isDisposed()) {
			this.circuitViewWindow = new CircuitViewer(display);
		}
	}
	
	public void openCircuitOptions(Circuit circuit) {
		new CircuitOptions(display, circuit);
	}
	
	public void triggerMenuUpdates() {
		synchronized (this.openEditorWindows) {
			this.openEditorWindows.forEach(Editor::updatePartSelector);
		}
	}
	
	public void setLastInteracted(Editor editor) {
		this.lastInteractedEditor = editor;
	}
	
	public Editor getLastInteractedEditor() {
		if (this.lastInteractedEditor == null || this.lastInteractedEditor.getShell().isDisposed()) {
			if (this.openEditorWindows.isEmpty()) return null;
			setLastInteracted(this.openEditorWindows.get(this.openEditorWindows.size() - 1));
		}
		return lastInteractedEditor;
	}
	
	private void updateUI() {
		
		synchronized (this.openEditorWindows) { // Prevent iteration of graphic update thread while removing editors
			List<Editor> disposedEditors = new ArrayList<>();
			this.openEditorWindows.forEach(editor -> {
				if (editor.getShell().isDisposed()) disposedEditors.add(editor);
			});
			
			if (!disposedEditors.isEmpty()) {
				this.openEditorWindows.removeAll(disposedEditors);
			}

			this.openEditorWindows.forEach(Editor::updateUI);
		}
		
		if (this.circuitViewWindow != null && !this.circuitViewWindow.getShell().isDisposed()) this.circuitViewWindow.updateUI();
		
		if (this.openEditorWindows.isEmpty()) this.terminate();
		
		this.display.readAndDispatch();
		
	}
	
	private void updateGraphics() {
		
		this.simulationMonitor.update();
		
		try {
			synchronized (this.openEditorWindows) { // Prevent iteration of graphic update thread while removing editors
				this.openEditorWindows.forEach(Editor::updateGraphics);
			}
		} catch (Exception e) {
			System.err.println("Error while updateing graphics!");
			e.printStackTrace();
		}
		
	}
	
	public static final String ICON_LOGIC_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAMgSURBVFhHtZfLq01RHMfv8epGXpE3ZSClFAOGHhNKZGLEH8DAVCmZEzEwdUPJwEQGMpKUkrwVAxOKMpGIvC6272ef/T3nd9be65xzxac+d+/12L/9O2utvfa+raIoRlut1reRNlNk0T4tiefQr21EsabpMKldGo40gd+y1T6dMMMmPllOlT8ppNn+CkckoSiBbAqJW+JGuanthRGoTmFcNgX/Vzg+U13iEaDC2f9PHL/zI50AmTHE9SHKs0rG0fs7qilgQXhuc1NAvxPymYz938iTcpEchK/r/tAqARacA2LKevlWxj6ptA9KwvfxyKvUTuC79ALBlDvSbYflCkmQ3fKMdNtF2Y9sAsYdIvx63+AYFQ28k7S/Lkt5agl0M8mzpjrCp+qY4htPr45DkybQ9Bh+rY7wuTpGVst17dORx9UxR+0xZArYv413u8hcSR0+pyIwQ16Rbt8k++H42f2mKQE4JX2To1SILfKRdP1BOYhc/A4OljJTMs9uZ7X7/KXcLofB1/DEYQ13aIJHzu12TDYxW66U6b6QXl8j17BHfpDx4o+S9dHEQ0mfW2WpS7weazQ1MLeufy+vh/I12cQLSfvlstTF19keWJlUslDMXunO9+RiCTek6y9RkcAHDm28O0yMz7ug80o2TQk8kL4Rc2pmybvSbeek2Spdv4OKiqb4PaQd2AEdqGmPXyqfSPfxSJyXlJkuYpqBCbAr0oH9GnZKBz9CRQPLZUziaTg/LSNp/B6YD3bE2GGDdLCrVGRYION0INv3HBnJJuAG6w48Yj+k6zfKHNtkjIH7ZGTCCQDD6Hq+fA7IJRIYMUbpuEy/qCxbOGsFsgnwSMSLyu/1wAUZ2zF+vFjehJvlLpl+PR2Svk8nvl6E5acZf1iZDpomAPvlfRmD2tuSkYnMk2el++QSGI2PCdCBIaptEhUEXiYXSqbklfwic6yV8+VNSQLcvBM/+RorK0nAbykuYN6iJGwniveBzlswHQESaHxFDgnBTTwHlz0K/G+YTSCu0vSXxnLaNiwkw2i2E0jnITCu/5prj0wf+iWXtpULsSiK0T/ALWuwWP6fnQAAAABJRU5ErkJggg==";
	public static final String ICON_IO_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAJnSURBVFhHxZe7jtUwEIZPltsCHULAchNCNPRQIFEsT4DEG221r7Ad2o6elhKBRIOoqZGAgg4E7EL2/2xPjk/iJOOzXD7pP7ZjZzIZj2Ofpm3bzaZpvi8iJ6U2VgN5Hab6FrJ1WsVGbPnoO/BbamK1Gq/jJ6RT0iGNvre/shLhUC4MmfrguAm7uXioaRUikKpwIJWM/ynMPlMdsAhwwbyvpcZhs9/dYw7gGSEehmicB5IZuprKetIUkBAYM82xIzFuL5XXJQ/2nO5FLQK1of8mnZeehZafwRRYBH5IliDLznm2pZoIsLIYby/eVc5IrE3y4J/SefK/6DuwzjKsobgMjxN2+3KuzYb2gZ+pXsNZ6bZ0ObQWi2vSzVh1MZrodIx2ZuxLNjbXQ2kKG8eKQ4M5pxP+Vi6YfaPxOtC/0cucncGLMqDmYVvSPckbMbNffA5GuOhZFZekl1Ju7LE0RW6fvaDbko0aB15JzyX7juxK7A9TkZi173WA5ce4R6EVuSBx7UlolSnaz7+E5j2DpribyjephC/SJ+lOaJUp2jcHmI/BnIxA4rF7fg2tJZ+lK7HqBwcQHwWMeuANSicn7HjyZ4V8Crx8lIjWudBaQmQ+xKofHLC5MeZy4H0q76cSbkkk4uvQKjPIAR2GQiT5IXR2Igp/GGYgAV9IbEAczZ5Kb6UpeM6K/d5fgoDXAR6ME4xH76Qb0hSzDjCvDAi7lOAGS1ITYTQBX8SLsToL9+T2gwNmCHCg61wDjBt5HazNSxEBzp+jDuSnnLwf8na/zwvOEM3oQCkREgc6LdUcuaac6/eFPGjbdvMIlOzBMgTj56QAAAAASUVORK5CYII=";
	public static final String ICON_WIRE_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAGUExURQAAAAAAAKVnuc8AAAACdFJOU/8A5bcwSgAAAAlwSFlzAAAOwwAADsMBx2+oZAAAAFpJREFUOE/djsESgCAIROX/f1qUxchwKGM69C7q7lMpFJApeCpnRvCY70lmN+kCF5cNVvCtgDm3hcFDAc9a+Nx/O0Ch3BEMoRAO+RvhREukUFAoLZFizVuBqALkLQNcVg88CgAAAABJRU5ErkJggg==";
	public static final String ICON_IC_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAGzSURBVFhHxZeNcoMgEIRD07R5/6dtJ20Je7rOegEBJek3syOKnsvxI4YY4zWE8HWaeE+KU9HQMtiqO6VYH+nwNp214Q38JYWp2E2r8XPSJekHJ97trxwhGFIhEOWBcQpxVXgptQYZmIvglpQLPgrGR1cbzAAu0P0zYfylkTQAZ0jxY4qeDA2w7wn7uSSSq4OaoYHe1OuL2HXUFqxfTNIApoRNixkflBoODXwmYW5iHLSghpgNzUozNLAXnxl/XsUb6Hp4B4y/GgOtaX8JXG5LlPq5dN3zEP+/ugALH/TwQrorGWlpZQ/h6Cw4TG8GSrQ+5zO4ygAfHp1movHx9cXCd3gh8iC4qgQ+ftkd0csZ3QWIURoH2fg0gD5ZtkkH0RdUGwMDEBaFb1wYxFYmwGohggHdEWG51K7pAfHQ6my6ZzS+TUPvtJq2gyzx0478TCe42LIjqikH63Jj7ML042gLwyDQoK1MZhciOuSD2KKjXlVrLdB79D7uOxB/WYj0BhiwkbkTbbFvvTYKL7bW238pCjM0oDNC64Ge+7pWYMYybwbcv6FyS3/NaqbGljlfZ+mPMV7ve+GNTka+RrwAAAAASUVORK5CYII=";
	
	public ComponentFolder builtinIcFolder;
	public ComponentFolder userIcFolder;
	
	public void registerIncludedParts() {
		
		System.out.println("Register components ...");
		
		ComponentFolder wireFolder = Registries.registerFolder("circuit.folders.wires", ICON_WIRE_GROUP);
		ComponentFolder logicFolder = Registries.registerFolder("circuit.folders.logic", ICON_LOGIC_GROUP);
		ComponentFolder ioFolder = Registries.registerFolder("circuit.folders.io", ICON_IO_GROUP);
		this.builtinIcFolder = Registries.registerFolder("circuit.folders.ics", ICON_IC_GROUP);
		this.userIcFolder = Registries.registerFolder("circuit.folders.user", ICON_IC_GROUP);
		
		Registries.registerPart(logicFolder, AndGateComponent.class, Component::placeClick, AndGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.and_gate", LogicGateComponent.ICON_AND_B64 );
		Registries.registerPart(logicFolder, OrGateComponent.class, Component::placeClick, OrGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.or_gate",LogicGateComponent.ICON_OR_B64);
		Registries.registerPart(logicFolder, NandGateComponent.class, Component::placeClick, NandGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.nand_gate",LogicGateComponent.ICON_NAND_B64);
		Registries.registerPart(logicFolder, NorGateComponent.class, Component::placeClick, NorGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.nor_gate",LogicGateComponent.ICON_NOR_B64);
		Registries.registerPart(logicFolder, XorGateComponent.class, Component::placeClick, XorGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.xor_gate",LogicGateComponent.ICON_XOR_B64);
		Registries.registerPart(logicFolder, NotGateComponent.class, Component::placeClick, NotGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.not_gate", NotGateComponent.ICON_B64);
		Registries.registerPart(ioFolder, BoolInputComponent.class, Component::placeClick, BoolInputComponent::coursorMove, Component::abbortPlacement, "circuit.components.bool_input", BoolInputComponent.ICON_B64);
		Registries.registerPart(ioFolder, BoolOutputComponent.class, Component::placeClick, BoolOutputComponent::coursorMove, Component::abbortPlacement, "circuit.components.bool_output", BoolOutputComponent.ICON_B64);
		Registries.registerPart(ioFolder, BusInputComponent.class, Component::placeClick, BusInputComponent::coursorMove, Component::abbortPlacement, "circuit.components.bus_input", BusInputComponent.ICON_B64);
		Registries.registerPart(ioFolder, BusOutputComponent.class, Component::placeClick, BusOutputComponent::coursorMove, Component::abbortPlacement, "circuit.components.bus_output", BusOutputComponent.ICON_B64);
		Registries.registerPart(ioFolder, BoolConstComponent.class, Component::placeClick, BoolConstComponent::coursorMove, Component::abbortPlacement, "circuit.components.bool_const", BoolConstComponent.ICON_B64);
		Registries.registerPart(ioFolder, BusConstComponent.class, Component::placeClick, BusConstComponent::coursorMove, Component::abbortPlacement, "circuit.components.bool_const", BusConstComponent.ICON_B64);
		Registries.registerPart(wireFolder, ConnectorWire.class, ConnectorWire::placeClick, ConnectorWire::coursorMove, ConnectorWire::abbortPlacement, "circuit.components.wire", ConnectorWire.ICON_B64);
		
		Translator.addLanguage("lang_en", "English");
		Translator.addLanguage("lang_de", "Deutsch");
		Translator.addLanguage("lang_fr", "Francais [En progres]");
		Translator.addLangFolder("/lang");
		
	}
	
	public void updateSubCircuitCache() {

		System.out.println("Load integrated circuits from file ...");
		
		Registries.clearSubCircuitCache();
		scanForFiles(this.subCircuitFolder, file -> {
			if (!isCircuitFile(file)) return;
			String name = Translator.translate(getFileName(file.getName()));
			Registries.cacheSubCircuit(this.subCircuitFolder, SubCircuitComponent.class, this.builtinIcFolder, Component::placeClick, (circuit, pos) -> SubCircuitComponent.coursorMove(circuit, pos, file), Component::abbortPlacement, name, SubCircuitComponent.ICON_B64);
		}, 2);
		triggerMenuUpdates();
	}
	
	public static void scanForFiles(File circuitFolder, Consumer<File> consumer, int scannDepth) {
		if (scannDepth == 0) return;
		if (circuitFolder.list() == null) return;
		for (String entry : circuitFolder.list()) {
			File entryPath = new File(circuitFolder, entry);
			if (entryPath.isFile()) {
				consumer.accept(entryPath);
			} else {
				scanForFiles(entryPath, consumer, scannDepth - 1);
			}
		}
	}
	
	public static String getFileName(String path) {
		String[] fs = path.split("/");
		String fn = fs[fs.length - 1];
		String[] fes = fn.split("\\.");
		String fe = fes[fes.length - 1];
		return fn.substring(0, fn.length() - (fes.length == 1 ? 0 : fe.length() + 1));
	}
	
	public static boolean isCircuitFile(File file) {
		String[] s = file.getName().split("\\.");
		return s[s.length - 1].equals(CIRCUIT_FILE_EXTENSION);
	}
	
}
