package de.m_marvin.logicsim.logic.nodes;

import java.util.Map;
import java.util.Objects;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Represents an IO node on an component in an circuit.
 * This is the abstract base class of all types of node.
 * Currently there exist three types of nodes
 * <br>- passive nodes
 * <br>- input node
 * <br>- output nodes
 * 
 * @author Marvin K.
 *
 */
public abstract class Node {
	
	protected Map<String, NetState> stateMapReference;
	protected final Component component;
	protected final int nodeNr;
	protected final String label;
	protected final Vec2i visualOffset;
	
	protected String laneTag;
	
	public Node(Component component, int nodeNr, String label, Vec2i visualOffset) {
		this.component = component;
		this.nodeNr = nodeNr;
		this.label = label;
		this.visualOffset = visualOffset;
		this.laneTag = Circuit.DEFAULT_BUS_LANE;
	}
	
	public void setLaneTag(String laneTag) {
		this.laneTag = laneTag;
		this.component.nodeChanged();
	}
	
	public String getLaneTag() {
		return laneTag;
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
	
	public Map<String, NetState> getLaneReference() {
		if (this.stateMapReference == null) return this.stateMapReference = this.getCircuit().getLaneMapReference(this);
		return this.stateMapReference;
	}
	
	public void disconnect() {
		this.stateMapReference = null;
	}
	
	/**
	 * Gets called when the user clicks on the node in the editor.
	 * Used to open the lane tag window.
	 * 
	 * @param mousePosition The position of the courser on the screen.
	 * @return true if the event got consumed and no other nodes on this position should be receive it (prevent multiple windows to pop up).
	 */
	public abstract boolean click(Vec2i mousePosition);
	
	public String makeIdentifier() {
		return this.component.makeIdentifier() + "/" + this.nodeNr + "'" + this.label + "'";
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

	@Override
	public String toString() {
		return "Node{ident=" + makeIdentifier() + "}";
	}
	
}
