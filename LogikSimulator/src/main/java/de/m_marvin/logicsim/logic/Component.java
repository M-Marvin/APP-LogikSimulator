package de.m_marvin.logicsim.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.JsonObject;

import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.univec.impl.Vec2i;

public abstract class Component {

	/* Factory methods */

	protected static Component currentComponent;
	protected static Vec2i placeOffset;
	
	public static void placeClick(Circuit circuit, Vec2i coursorPosition) {
		if (currentComponent != null) circuit.reconnect(false, currentComponent);
		currentComponent = null;
	}
	
	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition, Supplier<Component> constructor) {
		if (currentComponent == null) {
			currentComponent = constructor.get();
			circuit.add(currentComponent);
			placeOffset = new Vec2i(-currentComponent.getVisualWidth(), -currentComponent.getVisualHeight()).div(EditorArea.RASTER_SIZE).div(2).mul(EditorArea.RASTER_SIZE);
		}
		coursorPosition.x = Math.max(-placeOffset.x, coursorPosition.x);
		coursorPosition.y = Math.max(-placeOffset.y, coursorPosition.y);
		currentComponent.setVisualPosition(coursorPosition.add(placeOffset));
		return true;
	}
	
	public static void abbortPlacement(Circuit circuit) {
		if (currentComponent != null) {
			circuit.remove(currentComponent);
			currentComponent = null;
		}
	}
	
	/* End of factory methods */

	public static final float VISUAL_LINE_WIDTH = 2.0F;
	
	protected Vec2i visualPosition = new Vec2i(0, 0);
	protected String label = "unnamed";
	
	protected final List<InputNode> inputs = new ArrayList<>();
	protected final List<OutputNode> outputs = new ArrayList<>();
	protected final List<PassivNode> passives = new ArrayList<>();
	protected final Circuit circuit;
	protected final int componentNr;
	
	public Component(Circuit circuit) {
		this.circuit = circuit;
		this.componentNr = circuit.nextFreeId();
	}
	
	public List<InputNode> getInputs() {
		return inputs;
	}
	
	public List<OutputNode> getOutputs() {
		return outputs;
	}
	
	public List<PassivNode> getPassives() {
		return passives;
	}
	
	public List<Node> getAllNodes() {
		List<Node> nodes = new ArrayList<>();
		nodes.addAll(this.getInputs());
		nodes.addAll(this.getOutputs());
		nodes.addAll(this.getPassives());
		return nodes;
	}
	
	public int getComponentNr() {
		return componentNr;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public Circuit getCircuit() {
		return circuit;
	}
	
	public String makeIdentifier() {
		return this.componentNr + "'" + this.label + "'";
	}
	
	public Vec2i getVisualPosition() {
		return visualPosition;
	}
	
	public void setVisualPosition(Vec2i visualPosition) {
		this.visualPosition = visualPosition;
	}
	
	public abstract int getVisualWidth();
	public abstract int getVisualHeight();
	
	public abstract void render();
	
	public abstract void updateIO();
	
	public void click(Vec2i clickPosition) {}
	public void created() {}
	public void dispose() {}
	
	public void serialize(JsonObject json) {
		json.addProperty("label", this.label);
		json.addProperty("x", this.visualPosition.x);
		json.addProperty("y", this.visualPosition.y);
	}
	
	public void deserialize(JsonObject json) {
		this.label = json.get("label").getAsString();
		this.visualPosition.x = json.get("x").getAsInt();
		this.visualPosition.y = json.get("y").getAsInt();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Component other) return
				other.componentNr == this.componentNr &&
				other.visualPosition.equals(this.visualPosition);
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.componentNr, this.visualPosition);
	}
	
}
