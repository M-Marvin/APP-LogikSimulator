package de.m_marvin.logicsim.logic.nodes;

import java.util.Objects;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.univec.impl.Vec2i;

public class Node {
	
	protected final Component component;
	protected final int nodeNr;
	protected final String label;
	protected final Vec2i visualOffset;
	
	public Node(Component component, int nodeNr, String label, Vec2i visualOffset) {
		this.component = component;
		this.nodeNr = nodeNr;
		this.label = label;
		this.visualOffset = visualOffset;
	}
	
	public Component getComponent() {
		return component;
	}
	
	public int getNodeNr() {
		return nodeNr;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Vec2i getVisualOffset() {
		return visualOffset;
	}
	
	public Vec2i getVisualPosition() {
		return this.component.getVisualPosition().add(visualOffset);
	}
	
	public Circuit getCircuit() {
		return this.component.getCircuit();
	}
	
	public String makeIdentifier() {
		return this.component.makeIdentifier() + "/" + this.nodeNr + "'" + this.label + "'";
	}
	
	@Override
	public String toString() {
		return makeIdentifier();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node other) return 
				other.component.getComponentNr() == this.component.getComponentNr() && 
				other.nodeNr == this.nodeNr;
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.component, this.nodeNr);
	}
	
}
