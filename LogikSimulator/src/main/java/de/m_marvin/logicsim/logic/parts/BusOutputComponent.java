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

public class BusOutputComponent extends Component implements ISubCircuitIO {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsEAAA7BAbiRa+0AAALcSURBVFhHvZc9rA1BFMfv9ZBXvELhoxFEg/hMdKKhoaORUCiUCi0KoVCIUnxUSFT0RI9oRCJEJBKV6ESEhOBh/X8ze9yzM7PX7n3v+SX/N3POzD17dufM7L5hVVXTw+Hw2yCyWKpiN+D7MG5soFhL1SyKVjcsgZ/qo9/4GJiArolPSX+vkSYwK/EUfkmQJuPtSROF0W9JQA0XBRLIHu08ksW39TLnXO6qC1n8tGBYn/9Kr4pdCKwIf6jPDmhbirXSNmlGeis9liaB4uamsyL0tZAW4VXJ/KaP0mGpLyTQjJ8kkE64KWE/kc5Kh6QrtQ9tlfrQK4HdEv3X0jIcjhsSY+eD1Z3WBGxNfB1ckugfC1aTXZIlZ+yUtsduBmPg40eUAOd3KYE3Ev31wcqhcBlfHaxYnNgsk+e4ZDHzBBL8hC8Sx3MbryTm2l0flbDfBSuyQiImgtYEOCLtmESsOe1nqY2nEnP2BCtyT8J3K1iDwR0J+0ywYh9l2IBpY92+l9p4KDHnQLAiWySLcapuX0qGjWXYgIlHR9slAXaL54LkY+2XDPNl+B8g3gm0tnYlnkvM2RSsETski0OdeMyfwQXta8gu+knCJmAJ2wUrgzXikYT/e92elgwfv4ifYAV0MlhN9kqM3Q/WiBMSfp7OvrqP1klAv3MCByVsHuMqHI4HEmNHghVhDlsX/2Ycwm7idrCa8YswgePS4K2Hjzu6Jl2UntW+65KHrYf/XLAiHHIfJPyQxs9IJ3DK3ZXwm75Kl6UUxl7EbgM7oIA2S8B/lBQniDUSxbhBWoKjwHLJx/IwBhMnMF9k8XkJsQXNaRPsK7lE1+/G0o004utFGGL5gEwY9wKaK434fArw+EuZLgTFJ1cqGrIEfsC4F0tmAk5BK7B/Yfvf4gcskNEY7In/bRrH3xRLEHYSS1BKwC9JOu7tdKwrXCM8+ZAAfzAKzOr/hT71MS65dCwUYlVV038AGEcj39FM0wIAAAAASUVORK5CYII=";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new BusOutputComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected Optional<OutputNode> subCircuitOutput = Optional.empty();
	protected Map<String, NetState> laneReferenceCache;
	protected int bitCount = 8;
	
	protected Map<String, NetState> writeLaneCache = new FastAsyncMap<>();
	
	public BusOutputComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "bus_out";
		this.inputs.add(new InputNode(this, 2, "in", new Vec2i(-10, 20)));
	}
	
	@Override
	public int getVisualWidth() {
		return 80;
	}

	@Override
	public int getVisualHeight() {
		return 40;
	}
	
	public String getInputBus() {
		return this.inputs.get(0).getLaneTag();
	}
	
	public int getInputBitOffset() {
		String[] s = getInputBus().split("(?<=\\D)(?=\\d)");
		if (s.length > 1) return Integer.valueOf(s[1]);
		return 0;
	}
	
	public int getValue() {
		if (this.laneReferenceCache == null) return 0;
		String[] s = this.inputs.get(0).getLaneTag().split("(?<=\\D)(?=\\d)");
		String inputBus = s[0];
		int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
		int value = 0;
		for (String lane : this.laneReferenceCache.keySet()) {
			String[] busTag = lane.split("(?<=\\D)(?=\\d)");
			if (busTag.length == 2 && busTag[0].equals(inputBus)) {
				int bitIndex = Integer.parseInt(busTag[1]);
				if (bitIndex >= indexOffset && bitIndex < indexOffset + this.bitCount && Circuit.safeLaneRead(laneReferenceCache, lane).getLogicState()) value |= (1 << (bitIndex - indexOffset));
			}
		}
		return value;
	}
	
	public void rewriteCache() {
		writeLaneCache.clear();
		if (this.subCircuitOutput.isEmpty()) return;
		int value = getValue();
		String[] s = this.subCircuitOutput.get().getLaneTag().split("(?<=\\D)(?=\\d)");
		String outputBus = s[0];
		int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
		for (int i = 0; i < bitCount; i++) {
			boolean state = (value & (1 << i)) > 0;
			writeLaneCache.put(outputBus + (i + indexOffset), state ? NetState.HIGH : NetState.LOW);
		}
	}
	
	@Override
	public void updateIO() {
		this.laneReferenceCache = this.inputs.get(0).getLaneReference();
	}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (leftClick) {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.StringConfigField("editor.config.change_component_name", getLabel(), this::setLabel));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.bus_input.bit_count", this.bitCount, 1, 64, i -> this.bitCount = i));
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
		rewriteCache();
	}
	
	@Override
	public void render()  {
		
		EditorArea.swapColor(0, 1, 1, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(0, 0, 0, 0.4F);
		EditorArea.drawFilledRectangle(visualPosition.x + 10, visualPosition.y + 10, 60, 20);
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12,  Long.toHexString(getValue()));
		
		int tw = TextRenderer.drawText(visualPosition.x + getVisualWidth() + 10, visualPosition.y + getVisualHeight() / 2 - 6, 12, this.label, TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
		
		EditorArea.swapColor(1F, 0.4F, 0, 1);
		EditorArea.drawLine(1, visualPosition.x + getVisualWidth(), visualPosition.y + getVisualHeight() / 2, visualPosition.x + getVisualWidth() + tw / 2 + 10, visualPosition.y + getVisualHeight() / 2);
		
	}
	
	@Override
	public void queryIO() {
		if (this.subCircuitOutput.isPresent() && this.laneReferenceCache != null) {
			// Write the current value onto the bus lanes of the parent circuit
			// TODO Optimize
			rewriteCache();
			this.subCircuitOutput.get().writeLanes(writeLaneCache);
		}
	}
	
	@Override
	public Node makeNode(Component subCircuitComponent, int id, Vec2i offset, boolean connectToCircuit) {
		OutputNode outputNode = new OutputNode(subCircuitComponent, id, this.label, offset);
		if ((this.subCircuitOutput.isEmpty() || !isTransProcessNodeValid(this.subCircuitOutput.get())) && connectToCircuit) {
			this.subCircuitOutput = Optional.of(outputNode);
		}
		return outputNode;
	}
	
}
