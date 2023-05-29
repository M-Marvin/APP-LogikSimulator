package de.m_marvin.logicsim.logic.parts;

import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.parts.SubCircuitComponent.ISubCircuitIO;
import de.m_marvin.logicsim.logic.simulator.FastAsyncMap;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

public class BusInputComponent extends Component implements ISubCircuitIO {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsAAAA7AAWrWiQkAAALmSURBVFhHvZc9rAxRFMd3feUVCoWPRhAN4jPRiYaGjkZCoVAqtCiEQiFK8VEhUXk90SMakQgRiUQlOhEhIXgY/9+5c96euXN33+x6zy/5vbnn3rtnz9y5M7OvX1XVVL/f/95LLJFVahqxDaPGesq1TIdFKepGXsAf2U/Nsela+GK5VP4iyKv9HY5IQVESuTkU7pI3ype6TViBugkzspR8vvD8XGojX4FJl78rnn/2JPMC2ku0wOQF/Hfyu8CXJr8U6+UOuVy+k0/kJLCxfVOyqVubkALyTXhder/7SR6V40IBfH6w8lkBPsG5LYmfyvPyiLxW9+F2OQ5jFbBX0n4jV9ARuCUZu2hRd+YswB82cEXSPmFRkz3Si3N2y52p2YIx8PyDPaYCeH47sYC3kvZGi9r8lIyvtShtTmIuU+Sk9JztAjJiAV+lPa+H8Foy18/6uCR+b1FilSQnQsxfhEHkmnP8IofxTDJnn0WJ+5K+Oxb1etOS+JxFqY08krGFT9hcHz/IYTySzDlkUWKb9Bxn6uMr6fiY28IHWDqOXQrgbolckp4HD0on9mMLH+BJxdGvXYkXkjlbLBqwS3oe9knE+90WdPqXfpbEJCzhd8FqiwY8lvT/qI9npeP5OcHZV3LEJ4BvoNMWNdkvGXtg0YBTkn5W50Ddxg0SaI9a1caEw5KYZVxDR+ChZOyYRQnmcOvSv5UO4Sdx16Jm/iJM4HHp8NajjzO6IS/L53XfTRnh1qP/gkUJHnIfJf2Q52+RT+Apd0/S736TV2UOYy9Ts4E/oIBjq4D4o6Q4QayTbMZNkl+0JVbKmCvCGExcwHzRys9LgVvCO31C8Rap6fq7sXQijfx6EVqumJAJo15A/0ojPz8FWP5SpQtBceVKm4YqgQ8wHuWSucBT0DfYXPj97/mN/IdBY3BM4mfzPPGkuAR2J3EJSgXES5KPxzgf6wrfYStvBfCHoMCM/l8YZ3+MKi4fs41YVdXUX3hSJuCCNgQGAAAAAElFTkSuQmCC";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new BusInputComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected Optional<InputNode> subCircuitInput = Optional.empty();
	protected Map<String, NetState> laneReferenceCache;
	protected int value;
	protected int bitCount = 8;
	
	protected Map<String, NetState> writeLaneCache = new FastAsyncMap<>();
	
	public BusInputComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "bus_in";
		this.outputs.add(new OutputNode(this, 2, "out", new Vec2i(90, 20)));
	}
	
	@Override
	public int getVisualWidth() {
		return 80;
	}

	@Override
	public int getVisualHeight() {
		return 40;
	}
	
	public String getOutputBus() {
		return this.outputs.get(0).getLaneTag();
	}
	
	public int getOutputBitOffset() {
		String[] s = getOutputBus().split("(?<=\\D)(?=\\d)");
		if (s.length > 1) return Integer.valueOf(s[1]);
		return 0;
	}
	
	@Override
	public void nodeChanged() {
		rewriteCache();
	}
	
	public void rewriteCache() {
		writeLaneCache.clear();
		String[] s = getOutputBus().split("(?<=\\D)(?=\\d)");
		String outputBus = s[0];
		int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
		for (int i = 0; i < bitCount; i++) {
			boolean state = (value & (1 << i + indexOffset)) > 0;
			writeLaneCache.put(outputBus + (i + indexOffset), state ? NetState.HIGH : NetState.LOW);
		}
	}
	
	@Override
	public void updateIO() {
		if (this.subCircuitInput.isPresent() && this.laneReferenceCache != null) {
			// Read the bus value specified by the lane tag in the parent circuit
//			String[] s = this.subCircuitInput.get().getLaneTag().split("(?<=\\D)(?=\\d)");
//			String inputBus = s[0];
//			int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
//			
//			// TODO Optimize
//			this.value = 0;
//			for (String lane : this.laneReferenceCache.keySet()) {
//				String[] busTag = lane.split("(?<=\\D)(?=\\d)");
//				if (busTag.length == 2 && busTag[0].equals(inputBus)) {
//					int bitIndex = Integer.parseInt(busTag[1]);
//					if (bitIndex >= indexOffset && bitIndex < indexOffset + this.bitCount && Circuit.safeLaneRead(laneReferenceCache, lane).getLogicState()) this.value |= (1 << (bitIndex - indexOffset));
//				}
//			}
			
			this.value = this.subCircuitInput.get().readBusValue(this.bitCount);
			rewriteCache();
		}
		this.outputs.get(0).writeLanes(writeLaneCache);
	}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (leftClick) {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.StringConfigField("editor.config.change_component_name", getLabel(), this::setLabel));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.bus_input.bit_count", this.bitCount, 1, 64, i -> { this.bitCount = i; rewriteCache(); }));
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		} else if (this.subCircuitInput.isEmpty()) {
			InputDialog configDialog = new InputDialog(editor.getShell(), "editor.config.bus_input.value", this.value, 0, Integer.MAX_VALUE, i -> { this.value = i; rewriteCache(); });
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		}
	}
	
	@Override
	public void serialize(JsonObject json) {
		super.serialize(json);
		json.addProperty("bitCount", this.bitCount);
	}
	
	@Override
	public void deserialize(JsonObject json) {
		super.deserialize(json);
		this.bitCount = json.get("bitCount").getAsInt();
	}
	
	@Override
	public void reset() {
		this.value = 0;
		rewriteCache();
	}
	
	@Override
	public void render()  {

		EditorArea.swapColor(0, 1, 1, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(0, 0, 0, 0.4F);
		EditorArea.drawFilledRectangle(visualPosition.x + 10, visualPosition.y + 10, 60, 20);
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12,  this.subCircuitInput.isPresent() ? "IN" : Long.toHexString(this.value));
		
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
