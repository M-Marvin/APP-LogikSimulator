package de.m_marvin.logicsim.logic.nodes;

import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.ui.Editor;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Represents an input node on an component in an circuit.
 * Has additional methods to allow the component to read the state of the network the node is connected to and to get the ciruit-internal map that stores the states of each lane.
 * 
 * @author Marvin K.
 *
 */
public class InputNode extends Node {
	
	public InputNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}

	@Override
	public boolean click(Vec2i mousePosition) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		Shell shell = Editor.showTextDialog(editor.getShell(), "editor.window.change_tag", getLaneTag(), this::setLaneTag);
		shell.setLocation(mousePosition.x, mousePosition.y);
		return true;
	}
	
	public Map<String, NetState> getLaneReference() {
		return this.getCircuit().getLaneMapReference(this);
	}
	
	public boolean getState() {
		return this.getCircuit().getNetState(this, getLaneTag()).getLogicState();
	}
	
}
