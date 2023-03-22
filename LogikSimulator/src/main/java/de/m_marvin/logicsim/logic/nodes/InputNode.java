package de.m_marvin.logicsim.logic.nodes;

import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.univec.impl.Vec2i;

public class InputNode extends Node {
	
	public InputNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}
	
	public boolean getState() {
		return this.getCircuit().getNetState(this).getLogicState();
	}
	
}
