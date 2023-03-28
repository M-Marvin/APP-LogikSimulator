package de.m_marvin.logicsim;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import de.m_marvin.commandlineparser.CommandLineParser;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.parts.ButtonComponent;
import de.m_marvin.logicsim.logic.parts.LampComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.AndGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NandGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NorGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.OrGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.XorGateComponent;
import de.m_marvin.logicsim.logic.simulator.CircuitProcessor;
import de.m_marvin.logicsim.logic.parts.NotGateComponent;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent;
import de.m_marvin.logicsim.logic.wires.ConnectorWire;
import de.m_marvin.logicsim.ui.CircuitViewer;
import de.m_marvin.logicsim.ui.Editor;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.util.Registries;
import de.m_marvin.logicsim.util.Registries.ComponentFolder;

public class LogicSim {
	
	private static LogicSim INSTANCE;
	
	protected File subCircuitFolder;
	
	protected boolean shouldTerminate;
	protected Display display;
	protected CircuitProcessor processor;
	protected List<Editor> openEditors = new ArrayList<>();
	protected CircuitViewer circuitWindow;
	protected Editor lastInteractedEditor;
	
	public static void main(String... args) {
		
		LogicSim logicSim = new LogicSim();
		
		CommandLineParser parser = new CommandLineParser();
		parser.parseInput(args);
		logicSim.subCircuitFolder = new File(parser.getOption("sub-circuit-folder"));
		
		logicSim.start();
		
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
		return openEditors;
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
	
	private void start() {

		registerIncludedParts();
		updateSubCircuitCache();
		
		Translator.changeLanguage("lang_en");
		
		this.display = new Display();
		this.processor = new CircuitProcessor();
		
		openEditor(null);
		
		while (!shouldTerminate()) {
			update();
			render();
		}
		
		this.display.dispose();
		this.processor.terminate();
		
	}
	
	public void openEditor(Circuit circuit) {
		this.openEditors.add(new Editor(display, circuit));
	}
	
	public void openCircuitViewer() {
		if (this.circuitWindow == null || this.circuitWindow.getShell().isDisposed()) {
			this.circuitWindow = new CircuitViewer(display);
		}
	}
	
	public void triggerMenuUpdates() {
		this.openEditors.forEach(Editor::updatePartSelector);
	}
	
	public void setLastInteracted(Editor editor) {
		this.lastInteractedEditor = editor;
	}
	
	public Editor getLastInteractedEditor() {
		return lastInteractedEditor;
	}
	
	private void update() {
		
		List<Editor> disposedEditors = new ArrayList<>();
		this.openEditors.forEach(editor -> {
			if (editor.getShell().isDisposed()) disposedEditors.add(editor);
		});
		this.openEditors.removeAll(disposedEditors);
		
		this.openEditors.forEach(editor -> editor.updateTitle());
		if (this.circuitWindow != null && !this.circuitWindow.getShell().isDisposed()) this.circuitWindow.updateView();
		
		if (this.openEditors.isEmpty()) this.terminate();
		
		this.display.readAndDispatch();
		
	}
	
	private void render() {
		
		this.openEditors.forEach(Editor::render);
		
	}
	
	public static final String ICON_PART_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAGKSURBVFhHzZU7LwVBGIYPoaLmB0hEQaEhQkkodQqNoDhRSUQn8Rc0otT4EwoKEjoFlQSdQic0BOF553LInN2zK7uT3Sd5Mpfd7HzzzWUbVdPlSs+3K2PTGrfblbVBGYiZhbbvV54BH0DsmadSmwxURm0C0LkM74SyGHdlImkXUdi/isP4icd4jnlYxzWcMq2E7+dZglPcxi9UEGc4i1EIj+MIqj1nWpY73LLVTJSBS1s1hN/PzECvKwdduYhDeG9aEWiLEA7wEXdQz5qYl7QMJI1jSHqwgup7w0l1OPZwwVZTyQwgawm0gw9xF/XuMnqWsMdWy6MVGfThC26Ylt3577iPM6j3BrAThZZAm031edOyKAj1PeOROjIodAq0029xE0fVAa94hUq9AhzDQmTtAc2gH29QkZ/gBWrwB7zGaSyNthQ5dA9Ify94/P2QRqEl+MuT88O0flFfIXwAaTMvgwlXJuL/SuHgsX7Nfpx//Q2jUpsAlJJYae9I5RkIZx3rJITUZxNWTKPxA3CzXvc4mpIqAAAAAElFTkSuQmCC";
	public static final String ICON_WIRE_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAGUExURQAAAAAAAKVnuc8AAAACdFJOU/8A5bcwSgAAAAlwSFlzAAAOwwAADsMBx2+oZAAAAFpJREFUOE/djsESgCAIROX/f1qUxchwKGM69C7q7lMpFJApeCpnRvCY70lmN+kCF5cNVvCtgDm3hcFDAc9a+Nx/O0Ch3BEMoRAO+RvhREukUFAoLZFizVuBqALkLQNcVg88CgAAAABJRU5ErkJggg==";
	public static final String ICON_IC_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAGzSURBVFhHxZeNcoMgEIRD07R5/6dtJ20Je7rOegEBJek3syOKnsvxI4YY4zWE8HWaeE+KU9HQMtiqO6VYH+nwNp214Q38JYWp2E2r8XPSJekHJ97trxwhGFIhEOWBcQpxVXgptQYZmIvglpQLPgrGR1cbzAAu0P0zYfylkTQAZ0jxY4qeDA2w7wn7uSSSq4OaoYHe1OuL2HXUFqxfTNIApoRNixkflBoODXwmYW5iHLSghpgNzUozNLAXnxl/XsUb6Hp4B4y/GgOtaX8JXG5LlPq5dN3zEP+/ugALH/TwQrorGWlpZQ/h6Cw4TG8GSrQ+5zO4ygAfHp1movHx9cXCd3gh8iC4qgQ+ftkd0csZ3QWIURoH2fg0gD5ZtkkH0RdUGwMDEBaFb1wYxFYmwGohggHdEWG51K7pAfHQ6my6ZzS+TUPvtJq2gyzx0478TCe42LIjqikH63Jj7ML042gLwyDQoK1MZhciOuSD2KKjXlVrLdB79D7uOxB/WYj0BhiwkbkTbbFvvTYKL7bW238pCjM0oDNC64Ge+7pWYMYybwbcv6FyS3/NaqbGljlfZ+mPMV7ve+GNTka+RrwAAAAASUVORK5CYII=";
	
	protected ComponentFolder builtinIcFolder;
	
	public void registerIncludedParts() {
		ComponentFolder wireFolder = Registries.registerFolder("circuit.folders.wires", ICON_WIRE_GROUP);
		ComponentFolder partFolder = Registries.registerFolder("circuit.folders.basic", ICON_PART_GROUP);
		this.builtinIcFolder = Registries.registerFolder("circuit.folders.ics", ICON_IC_GROUP);
		
		Registries.registerPart(partFolder, AndGateComponent.class, Component::placeClick, AndGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.and_gate", LogicGateComponent.ICON_AND_B64 );
		Registries.registerPart(partFolder, OrGateComponent.class, Component::placeClick, OrGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.or_gate",LogicGateComponent.ICON_OR_B64);
		Registries.registerPart(partFolder, NandGateComponent.class, Component::placeClick, NandGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.nor_gate",LogicGateComponent.ICON_NAND_B64);
		Registries.registerPart(partFolder, NorGateComponent.class, Component::placeClick, NorGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.nand_gate",LogicGateComponent.ICON_NOR_B64);
		Registries.registerPart(partFolder, XorGateComponent.class, Component::placeClick, XorGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.xor_gate",LogicGateComponent.ICON_XOR_B64);
		Registries.registerPart(partFolder, NotGateComponent.class, Component::placeClick, NotGateComponent::coursorMove, Component::abbortPlacement, "circuit.components.not_gate", NotGateComponent.ICON_B64);
		Registries.registerPart(partFolder, ButtonComponent.class, Component::placeClick, ButtonComponent::coursorMove, Component::abbortPlacement, "circuit.components.button", ButtonComponent.ICON_B64);
		Registries.registerPart(partFolder, LampComponent.class, Component::placeClick, LampComponent::coursorMove, Component::abbortPlacement, "circuit.components.lamp", LampComponent.ICON_B64);
		Registries.registerPart(wireFolder, ConnectorWire.class, ConnectorWire::placeClick, ConnectorWire::coursorMove, ConnectorWire::abbortPlacement, "circuit.components.wire", ConnectorWire.ICON_B64);
		
		Registries.registerLangFolder("/lang");
	}
	
	public void updateSubCircuitCache() {
		Registries.clearSubCircuitCache();
		_fillSubCircuitCache(this.builtinIcFolder, this.subCircuitFolder);
		triggerMenuUpdates();
	}
	
	protected void _fillSubCircuitCache(ComponentFolder folder, File circuitFolder) {
		if (this.subCircuitFolder.list() == null) return;
		for (String entry : this.subCircuitFolder.list()) {
			File entryPath = new File(circuitFolder, entry);
			if (entryPath.isFile()) {
				Registries.cacheSubCircuit(circuitFolder, SubCircuitComponent.class, folder, Component::placeClick, (circuit, pos) -> SubCircuitComponent.coursorMove(circuit, pos, entryPath), Component::abbortPlacement, entry, SubCircuitComponent.ICON_B64);
			} else {
				_fillSubCircuitCache(folder, entryPath);
			}
		}
	}
	
}
