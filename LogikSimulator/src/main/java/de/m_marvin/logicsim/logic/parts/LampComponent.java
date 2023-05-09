package de.m_marvin.logicsim.logic.parts;

import java.util.Map;
import java.util.Optional;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent.ISubCircuitIO;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.univec.impl.Vec2i;

public class LampComponent extends Component implements ISubCircuitIO {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAHgSURBVFhHzZY9TsQwEEYJf1oJQQUUdFyAE3AAbkBLhURJy0HoKWloOQenoKACUSAWCN9zMovX8SaON+zukz7FiePxZGbsuCjLclQUxcdaxaZUVk2H34a2vjXZ2tZlvbpLwxz4Uhv98IyODFId35Amc4QOjCWi8C1B6Ix/n+so/I3FAV2YFHCgEdoBadi3fNnDeb4qhYb9sGDIz0LpVbERjqQ76V4650Fv6hrwU5FaA1fSm2RjrqUuKO4p+zkROJEepVtplwc1WdEMB7EPtHEhPUln0rPkh30QB7o4lpiY8JP/B8nIK+C6Bmx5EIG2GjiQtqqmw7ZudMODDhr2iUBX2H1eJDaTGHkp0Db8qWvbV6cyVw3wVWienTDFgZk7Ibm0/0EuWc5nhW1IVsYBwm/La4iCjGEpaixDYI/mQLJwVrIGlpKCpeI7gXd2II1xKb1LvDdLjH+VTiUf5rH+CX0jwB9vp2rOBJt7UtLfkbzwonllHrbtiqm/3TCSjGOlTezrT+xs+QZx4L+WI/NM2ecoQLhCTxdKrAbwEvCYfl+kzASH0n7V7MTOHWbfYYaMqc6e+GNDO/5HkQK37ZOCmAN+SsJ+/z7sS4U5XOSdA/WZMMZYp6U+9dHmXNjnCrEsy9EvMnCHkSEUVn0AAAAASUVORK5CYII=";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new LampComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected Optional<OutputNode> subCircuitOutput = Optional.empty();
	protected Map<String, NetState> laneReferenceCache;
	protected boolean state;
	
	public LampComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "lamp";
		this.inputs.add(new InputNode(this, 0, "in", new Vec2i(-10, 20)));
	}
	
	@Override
	public int getVisualWidth() {
		return 40;
	}

	@Override
	public int getVisualHeight() {
		return 40;
	}

	@Override
	public void updateIO() {
		this.state = this.inputs.get(0).getState();
		if (this.subCircuitOutput.isPresent()) this.laneReferenceCache = this.inputs.get(0).getLaneReference();
	}
	
	@Override
	public void render()  {
		
		if (state) {
			EditorArea.swapColor(0, 0, 1, 0.4F);
		} else {
			EditorArea.swapColor(0, 1, 1, 0.4F);
		}
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, this.state ? "ON" : "OFF");
		
		int tw = TextRenderer.drawText(visualPosition.x + getVisualWidth() + 10, visualPosition.y + getVisualHeight() / 2 - 6, 12, this.label, TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
		
		EditorArea.swapColor(1F, 0.4F, 0, 1);
		EditorArea.drawLine(1, visualPosition.x + getVisualWidth(), visualPosition.y + getVisualHeight() / 2, visualPosition.x + getVisualHeight() + tw / 2 + 10, visualPosition.y + getVisualHeight() / 2);
		
	}
	
	@Override
	public void reset() {
		this.state = false;
	}
	
	@Override
	public void queryIO() {
		if (this.subCircuitOutput.isPresent() && this.laneReferenceCache != null) {
			// If the node in the sub circuit only receives a one lane signal or has a lane label to filter out that specific
			// lane then only read that one lane and write it as normal output from the parent-component (applying the lane tag of the node)
			// If not, copy all received lanes one to one
			String laneTag = this.inputs.get(0).getLaneTag();
			if (laneReferenceCache.size() == 1 || !laneTag.equals(Circuit.DEFAULT_BUS_LANE)) {
				this.subCircuitOutput.get().setState(Circuit.safeLaneRead(laneReferenceCache, laneTag).getLogicState());
			} else {
				this.subCircuitOutput.get().writeLanes(laneReferenceCache);
			}
		}
	}
	
	@Override
	public Node makeNode(Component subCircuitComponent, int id, Vec2i offset, boolean connectToCircuit) {
		OutputNode overrideNode = new OutputNode(subCircuitComponent, id, this.label, offset);
		if (this.subCircuitOutput.isEmpty() || !isTransProcessNodeValid(this.subCircuitOutput.get())) {
			if (connectToCircuit) {
				this.subCircuitOutput = Optional.of(overrideNode);
			} else {
				this.subCircuitOutput = Optional.empty();
			}
		}
		return overrideNode;
	}
	
}
