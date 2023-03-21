package de.m_marvin.logicsim.logic.parts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.univec.impl.Vec2f;
import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.univec.impl.Vec4i;

public class SubCircuitComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAINSURBVFhHxZexTsMwGIQb2kJhRIgFIYSQ2BgYWBADEiMLT8Gj0IdgYmLgDVhAzGxsDIiJZ0AqYO7snOs6URonNHzSKXbS+L9c7CTNjDGjLMs+e44BZFzTErZJ1bEexlrGZsn16hEb+IEy10ymrvE+5GvEBiYQU/i2vaKZsN/UKJmeSwN5k9BAIdo/pDC+7hevmq7aXFUdNL7qeQN0xvvP+9MFqucN6J53ha8nA4uOXqiOrycDX7m6YlovWgWMZpGroDC+EqjLNrTlmgW4/whatb26RAlwZlYlcAs9uabnFHqDeB7FMfi7MgrjMwG7HBpyAD1Aj9AexMl1DG1Aa1AyqQncQXEiVZQmQPhgoFKX4wV07Zq10Piq5w3w0UilsAvxnHfbS8PXS10FISv5NjW1GdoYeM23O/m2ETLAq1C7ahKGcELdQ5e2Nx8lxfF9vTYJkDF0At1Ah9AmdA49Q/tQEnRFd1XPhbIH0Rn0AvFc6gO6gmLi8ZWIh2nwB01fzUNo3TVLicefSZ9Lgl+0bQzMIzSgehYdkLowoFqzMfwHNBBPButsAaiOHx9v4r4S4M4uv4hUb0gDvCfcchZ3ha+nBIh9OQBFxE90Hg/FGKVUtP41vq0XDsQd9hXZEH9vQdgm4UXZ6KEB5sCgzEC4DOMrDftNUiA0wzSdgeibMGSCP60pz4Qqc/ExO+GNMaNfKXmU5jgmYXQAAAAASUVORK5CYII=";
	
	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new SubCircuitComponent(circuit, LogicSim.getInstance().getCircuit()));
	}
	
	/* End of factory methods */
	
	public static final int NODE_RASTER = EditorArea.RASTER_SIZE * 2;
	public static final int MIN_COMPONENT_SIZE = 40;
	public static final int MAX_COMPONENT_SIZE = 400;
	
	public static interface ISubCircuitIO {
		
		public Node makeNode(Component subCircuitComponent, int id, Vec2i offset, boolean connectToCircuit);
		
	}
	
	protected int width;
	protected int height;
	protected int nodeCount;
	protected final Circuit subCircuit;
	protected final Map<Integer, ISubCircuitIO> node2subComponent = new HashMap<>();
	
	public SubCircuitComponent(Circuit circuit, Circuit subCurcuit) {
		super(circuit);
		this.subCircuit = subCurcuit;
		
		this.label = "sub_circuit";
		updatePinout(true);
	}
	
	public void updatePinout(boolean connectToCircuit) {
		this.node2subComponent.clear();
		this.inputs.clear();
		this.outputs.clear();
		this.passives.clear();
		
		Vec4i circuitBounds = this.subCircuit.getCircuitBounds(component -> component instanceof ISubCircuitIO);
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
		
		this.width = Math.max(MIN_COMPONENT_SIZE, this.height / 3);
		this.width -= this.width % EditorArea.RASTER_SIZE;
		
		this.nodeCount = 0;
		subCircuitIOs.forEach((subIO, position) -> {
			
			if (ypositions.size() == 1) position.y = 0.5F;
			Vec2f offset = position.mul((float) this.width, (float) this.height - EditorArea.RASTER_SIZE * 2);
			if (ypositions.size() > 1) offset.subI(offset.module((float) NODE_RASTER)); 
			offset.y += EditorArea.RASTER_SIZE;
			offset.x += offset.x > 0 ? +10 : -10;
			
			Node node = subIO.makeNode(this, this.nodeCount++, new Vec2i(offset), connectToCircuit);
			
			if (node instanceof InputNode input) {
				this.inputs.add(input);
			} else if (node instanceof OutputNode output) {
				this.outputs.add(output);
			} else if (node instanceof PassivNode passive) {
				this.passives.add(passive);
			}
			
		});
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

	@Override
	public void render() {
		
		EditorArea.swapColor(1, 1, 0, 0.4F);
		EditorArea.drawComponentFrame(this.visualPosition.x, this.visualPosition.y, this.width, this.height);
		
	}

	@Override
	public void updateIO() {
		// TODO Auto-generated method stub
		
	}

}
