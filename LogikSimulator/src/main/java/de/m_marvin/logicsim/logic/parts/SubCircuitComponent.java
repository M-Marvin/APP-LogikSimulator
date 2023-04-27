package de.m_marvin.logicsim.logic.parts;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.logic.simulator.CircuitProcessor;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.logicsim.util.CircuitSerializer;
import de.m_marvin.univec.impl.Vec2f;
import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.univec.impl.Vec4i;

public class SubCircuitComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAINSURBVFhHxZexTsMwGIQb2kJhRIgFIYSQ2BgYWBADEiMLT8Gj0IdgYmLgDVhAzGxsDIiJZ0AqYO7snOs6URonNHzSKXbS+L9c7CTNjDGjLMs+e44BZFzTErZJ1bEexlrGZsn16hEb+IEy10ymrvE+5GvEBiYQU/i2vaKZsN/UKJmeSwN5k9BAIdo/pDC+7hevmq7aXFUdNL7qeQN0xvvP+9MFqucN6J53ha8nA4uOXqiOrycDX7m6YlovWgWMZpGroDC+EqjLNrTlmgW4/whatb26RAlwZlYlcAs9uabnFHqDeB7FMfi7MgrjMwG7HBpyAD1Aj9AexMl1DG1Aa1AyqQncQXEiVZQmQPhgoFKX4wV07Zq10Piq5w3w0UilsAvxnHfbS8PXS10FISv5NjW1GdoYeM23O/m2ETLAq1C7ahKGcELdQ5e2Nx8lxfF9vTYJkDF0At1Ah9AmdA49Q/tQEnRFd1XPhbIH0Rn0AvFc6gO6gmLi8ZWIh2nwB01fzUNo3TVLicefSZ9Lgl+0bQzMIzSgehYdkLowoFqzMfwHNBBPButsAaiOHx9v4r4S4M4uv4hUb0gDvCfcchZ3ha+nBIh9OQBFxE90Hg/FGKVUtP41vq0XDsQd9hXZEH9vQdgm4UXZ6KEB5sCgzEC4DOMrDftNUiA0wzSdgeibMGSCP60pz4Qqc/ExO+GNMaNfKXmU5jgmYXQAAAAASUVORK5CYII=";
	
	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition, File circuitFile) {
		return Component.coursorMove(circuit, coursorPosition, () -> {
			SubCircuitComponent component = new SubCircuitComponent(circuit);
			component.setSubCircuitFile(circuitFile);
			return component;
		});
	}
	
	/* End of factory methods */
	
	public static final int NODE_RASTER = EditorArea.RASTER_SIZE * 2;
	public static final int MIN_COMPONENT_SIZE = 40;
	public static final int MAX_COMPONENT_SIZE = 400;
	
	public static interface ISubCircuitIO {
		
		public void queryIO();
		public Node makeNode(Component subCircuitComponent, int id, Vec2i offset, boolean connectToCircuit);
		
		public default boolean isTransProcessNodeValid(Node node) {
			return LogicSim.getInstance().getCircuitProcessor().holdsCircuit(node.getCircuit());
		}
		
	}
	
	protected int width;
	protected int height;
	protected int nodeCount;
	protected Circuit subCircuit;
	
	protected final Map<Integer, ISubCircuitIO> node2subComponent = new HashMap<>();
	
	public SubCircuitComponent(Circuit circuit) {
		super(circuit);
	}
	
	public void updatePinout() {
		this.node2subComponent.clear();
		this.inputs.clear();
		this.outputs.clear();
		this.passives.clear();
		
		Vec4i circuitBounds = this.subCircuit.getCircuitBounds(component -> true, component -> component instanceof ISubCircuitIO);
		Map<ISubCircuitIO, Vec2f> subCircuitIOs = new HashMap<>();
		Set<Float> ypositions = new HashSet<Float>();
		
		this.subCircuit.getComponents().forEach(component -> {
			if (component instanceof ISubCircuitIO subCircuitIO) {
				
				Vec2i position = component.getVisualPosition();
				
				Vec2f offset = new Vec2f(
					(position.x - circuitBounds.x) > (float) (circuitBounds.z - circuitBounds.x) / 2 ? 1 : 0,
					(position.y - circuitBounds.y) / (float) (circuitBounds.w - circuitBounds.y)
				);
				
				if (!Double.isFinite(offset.y)) offset.y = 0;
				
				subCircuitIOs.put(subCircuitIO, offset);
				ypositions.add(offset.y);
				
			}
		});
		
		this.height = MIN_COMPONENT_SIZE;
		
		if (ypositions.size() > 0) {
			for (; height <= MAX_COMPONENT_SIZE; height += EditorArea.RASTER_SIZE) {
				double error = ypositions.stream().mapToDouble(ypos -> {
					float offset = (ypos * ((float) this.height - EditorArea.RASTER_SIZE * 2));
					return offset % (float) NODE_RASTER; 
				}).max().getAsDouble();
				if (error < 1) break;
			}
		}
		
		this.width = Math.max(MIN_COMPONENT_SIZE, this.height / 2);
		this.width -= this.width % NODE_RASTER;
		
		this.nodeCount = 0;
		subCircuitIOs.forEach((subIO, position) -> {
			
			if (ypositions.size() == 1) position.y = 0.5F;
			Vec2f offset = position.mul((float) this.width, (float) this.height - EditorArea.RASTER_SIZE * 2);
			if (ypositions.size() > 1) offset.subI(offset.module((float) NODE_RASTER)); 
			offset.y += EditorArea.RASTER_SIZE;
			offset.x += offset.x > 0 ? +10 : -10;
			
			Node node = subIO.makeNode(this, this.nodeCount, new Vec2i(offset), !this.circuit.isVirtual());
			this.node2subComponent.put(this.nodeCount++, subIO);
			
			if (node instanceof InputNode input) {
				this.inputs.add(input);
			} else if (node instanceof OutputNode output) {
				this.outputs.add(output);
			} else if (node instanceof PassivNode passive) {
				this.passives.add(passive);
			}
			
		});
	}
	
	public void setSubCircuitFile(File subCircuitFile) {
		try {
			subCircuit = CircuitSerializer.loadCircuit(subCircuitFile);
		} catch (Exception e) {
			Editor.showErrorInfo(LogicSim.getInstance().getLastInteractedEditor().getShell(), "editor.window.error.load_sub_circuit", e);
			subCircuit = new Circuit();
		}
		updatePinout();
	}

	public void setSubCircuit(Circuit subCircuit) {
		this.subCircuit = subCircuit;
		updatePinout();
	}
	public Circuit getSubCircuit() {
		return subCircuit;
	}
	
	@Override
	public int getVisualWidth() {
		return this.width;
	}

	@Override
	public int getVisualHeight() {
		return this.height;
	}
	
	public String getRelativeCircuitPath() {
		File subCircuitFile = getSubCircuit().getCircuitFile();
		if (subCircuitFile == null) return "";
		if (subCircuitFile.toString().startsWith(LogicSim.getInstance().getSubCircuitFolder().toString())) {
			return LogicSim.getInstance().getSubCircuitFolder().toURI().relativize(subCircuitFile.toURI()).toString();
		} else {
			return getCircuit().getCircuitFile().getParentFile().toURI().relativize(subCircuitFile.toURI()).toString();
		}
	}
	
	public String getComponentName() {
		return Translator.translate(LogicSim.getFileName(this.getRelativeCircuitPath()));
	}
	
	public void setRelativeCircuitPath(String relativeCircuitFile) {
		File absolutePath = new File(LogicSim.getInstance().getSubCircuitFolder(), relativeCircuitFile);
		if (!absolutePath.isFile()) {
			absolutePath = new File(getCircuit().getCircuitFile().getParentFile(), relativeCircuitFile);
		}
		setSubCircuitFile(absolutePath);
	}
	
	@Override
	public void serialize(JsonObject json) {
		super.serialize(json);
		json.addProperty("subCircuitFile", getRelativeCircuitPath());
	}
	
	@Override
	public void deserialize(JsonObject json) {
		setRelativeCircuitPath(json.get("subCircuitFile").getAsString());
		super.deserialize(json);
	}

	@Override
	public void render() {
		
		EditorArea.swapColor(0, 1, 1, 0.4F);
		EditorArea.drawComponentFrame(this.visualPosition.x, this.visualPosition.y, this.width, this.height);
		
		String name = getComponentName();
		boolean overComponent = TextRenderer.getTextWidth(12, name, false) - 12 > this.width;
		
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, overComponent ? (visualPosition.y - 10) : (visualPosition.y + getVisualHeight() / 2), 12, name);
		
	}

	@Override
	public void updateIO() {
		
		this.node2subComponent.values().forEach(ISubCircuitIO::queryIO);
		
	}
	
	@Override
	public void reset() {
		if (!getCircuit().isVirtual()) {
			CircuitProcessor processor = LogicSim.getInstance().getCircuitProcessor();
			if (!processor.holdsCircuit(getSubCircuit())) processor.addProcess(getCircuit(), getSubCircuit());
		}
	}
	
	@Override
	public void dispose() {
		LogicSim.getInstance().getCircuitProcessor().removeProcess(this.subCircuit);
	}
	
}
