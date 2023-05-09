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
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAF3SURBVFhHzZcNbsMgDIVLf6aeYfc/ZKVuo34kLzKGpEAI3SdZhVKbZ0NIcd77u3PucZq4ivmpGdBtsDV2klhf8nGeemVYAX9ibmpWUyr8InYT+0HHTvYUQxV+Qy8d1/1WoWARYIGApLQdYXwkGbDrtSerEhh/SdIKwPoMpWrHHsFoAW+XYDhWAM6BoZxxEM3tj2Ar8PHHsBUEXILuAXugNhAnL/FL4tuSc7B0KXSwEh/+nu+B2x4BtZMD7QNc6x5omTxLi4Buk4PcOcAJ8JkzsjZ5zgdG0MbrGP8JoiBoc5eiMtrJspX5mh98MMb4CfzBkcdxEl8rYVZrGfQiiq8FHJn5Ki1PQVf+pYBhe0COgMueCsD3ezYLNrT9PveHN5wFGqjLXhg6AAFRfByCugK8LLBEcMC4NmRGq4VPWbTEOhAE4ObSig4cTSLopFCBUPpwL0VjhgJ4LwQ2U91vqQKAmFD5ICDzMiJPuTVrMe/YEmfHwj7w3t9f/w54lcD7KeEAAAAASUVORK5CYII=";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new BusInputComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected Optional<InputNode> subCircuitInput = Optional.empty();
	protected Map<String, NetState> laneReferenceCache;
	protected int value;
	protected int indexOffset = 0;
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
	
	public void rewriteCache() {
		writeLaneCache.clear();
		for (int i = 0; i < bitCount; i++) {
			boolean state = (value & (1 << i + indexOffset)) > 0;
			writeLaneCache.put(getOutputBus().split("(?<=\\D)(?=\\d)")[0] + (i + indexOffset), state ? NetState.HIGH : NetState.LOW);
		}
	}
	
	@Override
	public void updateIO() {
		if (this.subCircuitInput.isPresent() && this.laneReferenceCache != null) {
			// Read the bus value specified by the lane tag in the parent circuit
			String laneTag = this.subCircuitInput.get().getLaneTag().split("(?<=\\D)(?=\\d)")[0];
			this.value = 0;
			for (String lane : this.laneReferenceCache.keySet()) {
				String[] busTag = lane.split("(?<=\\D)(?=\\d)");
				if (busTag.length == 2 && busTag[0].equals(laneTag)) {
					int bitIndex = Integer.parseInt(busTag[1]);
					if (bitIndex >= this.indexOffset && bitIndex < this.indexOffset + this.bitCount && Circuit.safeLaneRead(laneReferenceCache, lane).getLogicState()) this.value |= (1 << bitIndex);
				}
			}
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
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.bus_input.index_offset", this.indexOffset, 0, 64, i -> this.indexOffset = i));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.bus_input.bit_count", this.bitCount, 1, 64, i -> this.bitCount = i));
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
		json.addProperty("indexOffset", this.indexOffset);
		json.addProperty("bitCount", this.bitCount);
	}
	
	@Override
	public void deserialize(JsonObject json) {
		super.deserialize(json);
		this.indexOffset = json.get("indexOffset").getAsInt();
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
