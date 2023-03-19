package de.m_marvin.logicsim.logic.nodes;

import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.Circuit.NetState;

public class OutputNode extends Node {

	public OutputNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}
	
	public void setState(boolean state) {
		this.getCircuit().setNetState(this, state ? NetState.HIGH : NetState.LOW);
	}
	
}
