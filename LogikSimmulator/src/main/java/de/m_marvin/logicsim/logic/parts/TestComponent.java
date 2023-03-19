package de.m_marvin.logicsim.logic.parts;

import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.InputNode;
import de.m_marvin.logicsim.logic.OutputNode;
import de.m_marvin.logicsim.ui.EditorArea;

public class TestComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABfSURBVDhPY6AUMELp/1CaVMCIMOC/FpRJJGC8BiZBBkBshxkAkcANUNUxMoE5FAD8XoC5Brcc5S6g1ID/A+4C6oQBuakQDPC7ABR92KIQCYAMgKUFsgDFmQlKkwsYGACUohNq0IMdxAAAAABJRU5ErkJggg==";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new TestComponent(circuit));
	}
	
	/* End of factory methods */

	public TestComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "test_component";
		this.inputs.add(new InputNode(this, 0, "in1", new Vec2i(0, 10)));
		this.inputs.add(new InputNode(this, 1, "in2", new Vec2i(0, 30)));
		this.outputs.add(new OutputNode(this, 2, "out", new Vec2i(40, 20)));
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
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void render()  {
		
		EditorArea.drawRectangle(VISUAL_LINE_WIDTH, visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		
	}
	
}
