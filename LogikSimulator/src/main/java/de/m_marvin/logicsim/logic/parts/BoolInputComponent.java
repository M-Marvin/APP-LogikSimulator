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

public class BoolInputComponent extends Component implements ISubCircuitIO {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsEAAA7BAbiRa+0AAAHzSURBVFhHzZY9TsQwFIQJf1oJQQUUdFyAE3AAbkBLhURJy0HoKWloOQenoKACUSAWCDNOxnrrdRLbZHf5pFGy8fp58vxiu6rrelJV1cdawyZUN7cOe0/62tYQaxuX9eZXGjLwhXvqh8/YUECq8Q1oC+J4c26/zZWiISsGkkJoXGJcKw4qzcIM4MLUkykUCz4Wiq/xfAY0aGn6U1F8/5IyIOZTtGBCA7kcQXfQPXTOB9m0NWCnIrUGrqA3SH2uoSFY2Pyvf/GSDJxAj9AttMsHLUXZDDvxU+vjAnqCzqBnyKZ9FANDHEMcmOnn/D9AoqyA2xrQ56HFposDiKuY0NJN3fDBAIrvP3dmYCjtlheIi0mMsinAPvCJa99bp5JjwI8XdvrLSphiQPGZRZfJorR1kGOeteP2gzENFPEvDYxRkDE0RYzP9LvPeVUZ4J4QPREtnVVNgWflGSDWBN3pYBrjEnqH+L8usf8rdApZOI7aPbkZ4I6309x2wph7UNLuyHnhH+VKDv2pNULqthtmkv1Y+T4+dmIXywakAfd5LACOMxOfRwGmK3S6VGI1QJeEjtluxSmTyCG039wOonOH4jsUSMw0ZmL7hnHsS3EK3DLMKYgZsFMSttvfYVsqHMNl3hloz4Qxpjgt5dRHn7mwzRViXdeTX8wGjZeCBEJuAAAAAElFTkSuQmCC";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new BoolInputComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected Optional<InputNode> subCircuitInput = Optional.empty();
	protected Map<String, NetState> laneReferenceCache;
	protected boolean toggle;
	
	public BoolInputComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "bool_in";
		this.outputs.add(new OutputNode(this, 2, "out", new Vec2i(50, 20)));
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
		if (this.subCircuitInput.isPresent() && this.laneReferenceCache != null) {
			// If the node in the parent circuit only receives a one lane signal or has a lane label to filter out that specific
			// lane then only read that one lane and write it as normal output from this button (applying the lane tag of this button)
			// If not, copy all received lanes one to one
			String laneTag = this.subCircuitInput.get().getLaneTag();
			if (laneReferenceCache.size() == 1 || !laneTag.equals(Circuit.DEFAULT_BUS_LANE)) {
				this.toggle = Circuit.safeLaneRead(laneReferenceCache, laneTag).getLogicState();
				this.outputs.get(0).setState(this.toggle);
			} else {
				this.outputs.get(0).writeLanes(laneReferenceCache);
			}
		} else {
			this.outputs.get(0).setState(this.toggle);
		}
	}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {
		super.click(clickPosition, leftClick);
		if (!leftClick) {
			if (this.subCircuitInput.isEmpty()) this.toggle = !this.toggle;
		}
	}
	
	@Override
	public void reset() {
		this.toggle = false;
	}
	
	@Override
	public void render()  {
		
		if (toggle) {
			EditorArea.swapColor(0, 0, 1, 0.4F);
		} else {
			EditorArea.swapColor(0, 1, 1, 0.4F);
		}
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12,  this.subCircuitInput.isPresent() ? "IN" : this.toggle ? "ON" : "OFF");
		
		int tw = TextRenderer.drawText(visualPosition.x - 10, visualPosition.y + getVisualHeight() / 2 - 7, 12, this.label, TextRenderer.ORIGIN_RIGHT | TextRenderer.RESIZED);
		
		EditorArea.swapColor(1F, 0.4F, 0, 1);
		EditorArea.drawLine(1, visualPosition.x - tw / 2 - 10, visualPosition.y + getVisualHeight() / 2, visualPosition.x, visualPosition.y + getVisualHeight() / 2);
		
	}
	
	@Override
	public void queryIO() {
		if (this.subCircuitInput.isPresent()) {
			this.laneReferenceCache = this.subCircuitInput.get().getLaneReference();
		}
	}
	
	@Override
	public Node makeNode(Component subCircuitComponent, int id, Vec2i offset, boolean connectToCircuit) {
		InputNode overrideNode = new InputNode(subCircuitComponent, id, this.label, offset);
		if (this.subCircuitInput.isEmpty() || !isTransProcessNodeValid(this.subCircuitInput.get())) {
			if (connectToCircuit) {
				this.subCircuitInput = Optional.of(overrideNode);
			} else {
				this.subCircuitInput = Optional.empty();
			}
		}
		return overrideNode;
	}
	
}
