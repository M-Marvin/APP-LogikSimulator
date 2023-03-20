package de.m_marvin.logicsim;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Display;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.parts.ButtonComponent;
import de.m_marvin.logicsim.logic.parts.LampComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent;
import de.m_marvin.logicsim.logic.parts.NotGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.AndGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NandGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.NorGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.OrGateComponent;
import de.m_marvin.logicsim.logic.parts.LogicGateComponent.XorGateComponent;
import de.m_marvin.logicsim.logic.wires.ConnectorWire;
import de.m_marvin.logicsim.ui.Editor;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.util.Registries;
import de.m_marvin.logicsim.util.Registries.ComponentFolder;

public class LogicSim {
	
	private static LogicSim INSTANCE;
	
	protected boolean shouldTerminate;
	protected Thread renderThread;
	protected Display display;
	protected Editor mainWindow;
	protected Circuit circuit;
	
	public static void main(String... args) {
		
		new LogicSim().start();
		
	}
	
	public LogicSim() {
		INSTANCE = this;
	}
	
	public static LogicSim getInstance() {
		return INSTANCE;
	}

	public Device getDisplay() {
		return this.display;
	}
	
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
	
	public void terminate() {
		this.shouldTerminate = true;
	}
	
	public Circuit getCircuit() {
		return circuit;
	}
	
	public void setCircuit(Circuit circuit) {
		this.circuit = circuit;
		this.mainWindow.changeCircuit(circuit);
	}
	
	private void start() {

		registerIncludedParts();
		
		Translator.changeLanguage("lang_de");
		
		this.display = new Display();
		
		this.mainWindow = new Editor(this.display);
		
		setCircuit(new Circuit());
		
		// TODO Multithreading of simulation
		while (!shouldTerminate()) {
			update();
			render();
		}
		
		TextRenderer.cleanUpOpenGL();
		this.display.dispose();
		
	}
	
	private void update() {
		
		if (this.mainWindow.getShell().isDisposed()) this.terminate();
		
		if (this.circuit != null) this.circuit.updateCircuit();
		
		this.display.readAndDispatch();
		
	}
	
	private void render() {
		
		this.mainWindow.render();
		
	}
	
	public static final String ICON_PART_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAGKSURBVFhHzZU7LwVBGIYPoaLmB0hEQaEhQkkodQqNoDhRSUQn8Rc0otT4EwoKEjoFlQSdQic0BOF553LInN2zK7uT3Sd5Mpfd7HzzzWUbVdPlSs+3K2PTGrfblbVBGYiZhbbvV54BH0DsmadSmwxURm0C0LkM74SyGHdlImkXUdi/isP4icd4jnlYxzWcMq2E7+dZglPcxi9UEGc4i1EIj+MIqj1nWpY73LLVTJSBS1s1hN/PzECvKwdduYhDeG9aEWiLEA7wEXdQz5qYl7QMJI1jSHqwgup7w0l1OPZwwVZTyQwgawm0gw9xF/XuMnqWsMdWy6MVGfThC26Ylt3577iPM6j3BrAThZZAm031edOyKAj1PeOROjIodAq0029xE0fVAa94hUq9AhzDQmTtAc2gH29QkZ/gBWrwB7zGaSyNthQ5dA9Ify94/P2QRqEl+MuT88O0flFfIXwAaTMvgwlXJuL/SuHgsX7Nfpx//Q2jUpsAlJJYae9I5RkIZx3rJITUZxNWTKPxA3CzXvc4mpIqAAAAAElFTkSuQmCC";
	public static final String ICON_WIRE_GROUP = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAGUExURQAAAAAAAKVnuc8AAAACdFJOU/8A5bcwSgAAAAlwSFlzAAAOwwAADsMBx2+oZAAAAFpJREFUOE/djsESgCAIROX/f1qUxchwKGM69C7q7lMpFJApeCpnRvCY70lmN+kCF5cNVvCtgDm3hcFDAc9a+Nx/O0Ch3BEMoRAO+RvhREukUFAoLZFizVuBqALkLQNcVg88CgAAAABJRU5ErkJggg==";
	
	public void registerIncludedParts() {
		ComponentFolder wireFolder = Registries.registerFolder("circuit.folders.wires", ICON_WIRE_GROUP);
		ComponentFolder partFolder = Registries.registerFolder("circuit.folders.basic", ICON_PART_GROUP);
		
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
	
}
