package de.m_marvin.logicsim.logic.nodes;

import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Represents an passive node on an component in an circuit.
 * Has no additional functionality and servers only as a do-nothing node to identify other nodes on network-compilation.
 * Is used by wires to make the ends of them connected to each other by simply giving them the same id.
 * Input and output nodes could also be used, the passive node just clarifies that the node does not affect the simulation.
 * It also has no lane tag window, so no lane tag can be set since this would not affect the net work at all.
 * 
 * @author Marvin K.
 *
 */
public class PassivNode extends Node {

	public PassivNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}

	@Override
	public boolean click(Vec2i mousePosition) {
		return false;
	}
	
}
