package de.m_marvin.logicsim.logic.parts;

import com.google.gson.JsonObject;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

public class ClockComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAFqSURBVFhHzZeLboMwDEVLu279/6+d1m2pT+AiYyhKEQk90hWBqOZi59UupXTruu771PNhSn0z49uw1neyWJ92Ofd3ZUQD/6aub75MqfGL6Wr65Sa6/XNXhCEvAkkRjEvE9eKl0hQyMDThbloKvheKT6kzygAP5L4mij9+pAzgjBTPU1QZGVDtmyMDtVMvnpaAKZGnRWtk4MvE3GQcNEUG1iBdUZ7SftD7xg+NBp6NBZ5LS5T0L4KB5mn3nG0f+Bnah1Bagr1hTLD4FQ3CWuT94EgDmbcyoPrHeVyDccq+dQn8ChZR39b+RTDDD2ptzYo/MaUMMCXGY1JrvLPDMnAYGIjL78ThjsyWeTuRX5QBXtrqRORLfMUAD7hyImqOHwOaBSoBR3T6vbSCzdJZgM4dkxL7QBjIW+RGfOA4jvxHUeqc7fy/lMaADPgaxS/191uyAJjJmc8Gwn9Dz91OS6+sCWvmYl8e8Cml2wMeUHNLjQvU1wAAAABJRU5ErkJggg==";

	/* Factory methods */
	
	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new ClockComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected int ontime;
	protected int offtime;
	protected int timer;
	protected long tstep;
	
	public ClockComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "clock";
		this.ontime = 1000;
		this.offtime = 1000;
		this.timer = 0;
		this.tstep = -1;
		this.inputs.add(new InputNode(this, 0, "hold", new Vec2i(-10, 40)));
		this.inputs.add(new InputNode(this, 1, "enable", new Vec2i(-10, 80)));
		this.outputs.add(new OutputNode(this, 2, "clock", new Vec2i(90, 60)));
	}

	@Override
	public int getVisualWidth() {
		return 80;
	}

	@Override
	public int getVisualHeight() {
		return 120;
	}
	
	@Override
	public void updateIO() {
		if (this.inputs.get(1).getState()) {
			if (!this.inputs.get(0).getState()) {
				if (this.tstep < 0) this.tstep = System.currentTimeMillis();
				long now = System.currentTimeMillis();
				this.timer += (int) (now - this.tstep);
				this.tstep = now;
				if (this.timer > this.ontime + this.offtime) this.timer %= (this.ontime + this.offtime);
			} else {
				this.tstep = -1;
			}
			
			this.outputs.get(0).setState(this.timer > this.offtime);
		} else {
			this.tstep = -1;
			this.timer = 0;
			this.outputs.get(0).setState(false);
		}
	}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (leftClick) {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.StringConfigField("editor.config.change_component_name", getLabel(), this::setLabel));
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		} else {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.clock.ontime", this.ontime, 1, Integer.MAX_VALUE, i -> { this.ontime = i; }));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.clock.offtime", this.offtime, 1, Integer.MAX_VALUE, i -> { this.offtime = i; }));
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		}
	}
	
	@Override
	public void reset() {}
	
	@Override
	public void render()  {
		
		EditorArea.swapColor(1, 0, 1, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(0, 0, 0, 0.4F);
		EditorArea.drawFilledRectangle(visualPosition.x + 10, visualPosition.y + 20, 60, 20);
		EditorArea.drawFilledRectangle(visualPosition.x + 10, visualPosition.y + getVisualHeight() - 33, 60, 20);
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + 12, 12, "Timer");
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + 30, 12, Long.toString(this.timer));


		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() - 40, 12, "Period");
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() - 22, 12, Long.toString(this.ontime + this.offtime));
		
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, this.label);
		
	}
	
	@Override
	public void serialize(JsonObject json) {
		super.serialize(json);
		json.addProperty("ontime", this.ontime);
		json.addProperty("offtime", this.offtime);
	}
	
	@Override
	public void deserialize(JsonObject json) {
		super.deserialize(json);
		this.ontime = json.get("ontime").getAsInt();
		this.offtime = json.get("offtime").getAsInt();
	}
	
}
