package de.m_marvin.logicsim.logic;

import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.logicsim.logic.Circuit.NetState;

public class PassivNode extends Node {

	public PassivNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}

	public boolean getState() {
		return this.getCircuit().getNetState(this).getLogicState();
	}

	public void setState(boolean state) {
		this.getCircuit().setNetState(this, state ? NetState.HIGH : NetState.LOW);
	}
	
}
