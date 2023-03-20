package de.m_marvin.logicsim.logic.parts;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.univec.impl.Vec2i;

public class LampComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHVSURBVFhHvZcNbsMgDIWbdj89w+5/yEndxvxIHnIcxzGk2yehBjDm2RBIp1LKfZqmz8vMi5QyP1b0M4j6LuLrTX6ucy2HFfAjZZofu8kKv0l5lfKFip3sIQVZ+K61bb+ujwoFTYAFAjapfSL0jyArdr3ORJWB/luQVgDW5wgMflqWunbsX2BTzsjQPhpltIzY3AgamcYbtyGT3ozNHhCAsS3zdglcVZ1QYErkFQfR8pxBO92boOtNshnIDIZNZAdhe/1sb+J73gIvYtumJ49ENigAJ1NmgLax9l7kaGPRtDoF4IiMLiLrQMM+b6xt29hQAC+fiMwEFogLbSggMoqiJ56NnnzXPwXganSvx4VIoNd3GDmhgHcpuKOBjiYTPdG20eSwcy89DEKnPg1HBexh/bcMePRMTrrHaAFMm3aSWseFrO1KpB3EFGGNRjIAIiHavwsMjs4E2IyK2/iP9sC/4Ak4ig4p7tkbluZfPgVuZzKAsR9LsUCgbffWnWdPA+qiE/EM3NjNPz6GdAb4Z4EpwgD068L0jywBD6DVEmtHEIBreRTteDWJoINCBmrq6/9SPCxQgH5NbKS6PpIFADE181VA8FH6kH/Nme8EEomzfXUflFLuv4u5ho4u5d23AAAAAElFTkSuQmCC";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new LampComponent(circuit));
	}
	
	/* End of factory methods */
	
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
		
	}
	
}
