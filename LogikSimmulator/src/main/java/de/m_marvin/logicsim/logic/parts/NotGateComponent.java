package de.m_marvin.logicsim.logic.parts;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.univec.impl.Vec2i;

public class NotGateComponent extends Component {

	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABfSURBVDhPY6AUMELp/1CaVMCIMOC/FpRJJGC8BiZBBkBshxkAkcANUNUxMoE5FAD8XoC5Brcc5S6g1ID/A+4C6oQBuakQDPC7ABR92KIQCYAMgKUFsgDFmQlKkwsYGACUohNq0IMdxAAAAABJRU5ErkJggg==";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new NotGateComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected boolean state;
	
	public NotGateComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "button";
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
		this.outputs.get(0).setState(this.state);
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
