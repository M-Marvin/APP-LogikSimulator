package de.m_marvin.logicsim.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.logic.simulator.AsyncArrayList;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

/**
 * A component is a part placed in the circuit.
 * Every component can have multiple input, output and passive nodes.
 * It contains the updateIO method which implements the logic behind the part and also some event-specific methods like create, dispose and reset.
 * The nodes are most at the time constructed in the constructor of the component and do normally not change during the simulation.
 * 
 * @author Marvin K.
 *
 */
public abstract class Component {

	/* Factory methods */

	protected static Component currentComponent;
	protected static Vec2i placeOffset;
	
	public static void placeClick(Circuit circuit, Vec2i coursorPosition) {
		if (currentComponent != null) {
			circuit.reconnect(false, currentComponent);
			currentComponent.reset();
		}
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
	
	protected Vec2i visualPosition = new Vec2i(0, 0);
	protected String label = "unnamed";
	
	protected final List<InputNode> inputs = new AsyncArrayList<>();
	protected final List<OutputNode> outputs = new AsyncArrayList<>();
	protected final List<PassivNode> passives = new AsyncArrayList<>();
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
	
	/**
	 * Gets triggered when the component is clicked in the editor
	 * @param clickPosition The position of the courser on the editor-area
	 * @param clickPosition True if an double left click was performed instead of a single right click
	 */
	public void click(Vec2i clickPosition, boolean leftClick) {
		if (leftClick) {
			Editor editor = LogicSim.getInstance().getLastInteractedEditor();
			InputDialog configDialog = new InputDialog(editor.getShell(), "editor.config.change_component_name", getLabel(), this::setLabel);
			configDialog.setLocation(clickPosition.x, clickPosition.y);
			configDialog.open();
		}
	}
	/**
	 * Gets triggered aster the component got constructed the first time (when loading a file, or placing the component)
	 */
	public void created() {}
	/**
	 * Gets triggered directly before the component gets removed from the circuit.
	 */
	public void dispose() {}
	/**
	 * Gets triggered if the component should to reset its state or rebuild some internal structures.
	 * For example, the button component resets its toggle-state to "false", the sub-circuit component checks if the process of its sub-circuit is still running, and if not, restarts it.
	 */
	public void reset() {}
	
	/**
	 * Store additional component-specific data into the components JSON entry.
	 * @param json
	 */
	public void serialize(JsonObject json) {
		json.addProperty("label", this.label);
		json.addProperty("x", this.visualPosition.x);
		json.addProperty("y", this.visualPosition.y);
		JsonObject laneTags = new JsonObject();
		this.getAllNodes().forEach(node -> {
			if (!node.getLaneTag().equals(Circuit.DEFAULT_BUS_LANE)) laneTags.addProperty(Integer.toString(node.getNodeNr()), node.getLaneTag());
		});
		if (json.size() > 0) json.add("laneTags", laneTags);
	}
	
	/**
	 * Read additional component-specific data from the components JSON entry.
	 * @param json
	 */
	public void deserialize(JsonObject json) {
		this.label = json.get("label").getAsString();
		this.visualPosition.x = json.get("x").getAsInt();
		this.visualPosition.y = json.get("y").getAsInt();
		JsonElement laneTags = json.get("laneTags");
		if (laneTags != null) {
			this.getAllNodes().forEach(node -> {
				if (laneTags.getAsJsonObject().has(Integer.toString(node.getNodeNr()))) node.setLaneTag(laneTags.getAsJsonObject().get(Integer.toString(node.getNodeNr())).getAsString());
			});
		}
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
	
	@Override
	public String toString() {
		return "Component{ident=" + makeIdentifier() + ",pos=" + this.visualPosition + "}";
	}

	public boolean isInBounds(Vec2i pos) {
		return	getVisualPosition().x <= pos.x &&
				getVisualPosition().x + getVisualWidth() >= pos.x &&
				getVisualPosition().y <= pos.y &&
				getVisualPosition().y + getVisualHeight() >= pos.y;
	}

	public void nodeChanged() {};
	
}
