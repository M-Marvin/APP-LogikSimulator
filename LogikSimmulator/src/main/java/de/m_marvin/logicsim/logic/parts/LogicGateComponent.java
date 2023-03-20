package de.m_marvin.logicsim.logic.parts;

import java.util.function.BiFunction;

import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.logicsim.ui.TextRenderer;

public abstract class LogicGateComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABfSURBVDhPY6AUMELp/1CaVMCIMOC/FpRJJGC8BiZBBkBshxkAkcANUNUxMoE5FAD8XoC5Brcc5S6g1ID/A+4C6oQBuakQDPC7ABR92KIQCYAMgKUFsgDFmQlKkwsYGACUohNq0IMdxAAAAABJRU5ErkJggg==";

	/* Factory methods */
	
	public static class AndGateComponent extends LogicGateComponent {
		public AndGateComponent(Circuit circuit) {
			super(circuit, (a, b) -> a && b);
		}
		public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
			return Component.coursorMove(circuit, coursorPosition, () -> new AndGateComponent(circuit));
		}
		public String getTextLabel() {
			return "AND";
		}
	}

	public static class OrGateComponent extends LogicGateComponent {
		public OrGateComponent(Circuit circuit) {
			super(circuit, (a, b) -> a || b);
		}
		public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
			return Component.coursorMove(circuit, coursorPosition, () -> new OrGateComponent(circuit));
		}
		public String getTextLabel() {
			return "OR";
		}
	}

	public static class NandGateComponent extends LogicGateComponent {
		public NandGateComponent(Circuit circuit) {
			super(circuit, (a, b) -> !a && !b);
		}
		public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
			return Component.coursorMove(circuit, coursorPosition, () -> new NandGateComponent(circuit));
		}
		public String getTextLabel() {
			return "NAND";
		}
	}

	public static class NorGateComponent extends LogicGateComponent {
		public NorGateComponent(Circuit circuit) {
			super(circuit, (a, b) -> !a || !b);
		}
		public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
			return Component.coursorMove(circuit, coursorPosition, () -> new NorGateComponent(circuit));
		}
		public String getTextLabel() {
			return "NOR";
		}
	}

	public static class XorGateComponent extends LogicGateComponent {
		public XorGateComponent(Circuit circuit) {
			super(circuit, (a, b) -> a || b && (!a || !b));
		}
		public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
			return Component.coursorMove(circuit, coursorPosition, () -> new XorGateComponent(circuit));
		}
		public String getTextLabel() {
			return "XOR";
		}
	}

	/* End of factory methods */

	public static enum GateType {
		OR((a, b) -> a || b),
		AND((a, b) -> a && b),
		XOR((a, b) -> a || b && (!a || !b)),
		NOR((a, b) -> !a || !b),
		NAND((a, b) -> !a && !b);
		
		private final BiFunction<Boolean, Boolean, Boolean> logicalFuntion;
		
		private GateType(BiFunction<Boolean, Boolean, Boolean> logicalFuntion) {
			this.logicalFuntion = logicalFuntion;
		}
		
		public boolean apply(boolean logicA, boolean logicB) {
			return this.logicalFuntion.apply(logicA, logicB);
		}
	}
	
	protected final BiFunction<Boolean, Boolean, Boolean> logicalFuntion;
	
	public LogicGateComponent(Circuit circuit, BiFunction<Boolean, Boolean, Boolean> logicalFuntion) {
		super(circuit);
		this.logicalFuntion = logicalFuntion;
		
		this.label = "logic_gate";
		this.inputs.add(new InputNode(this, 0, "in1", new Vec2i(-10, 10)));
		this.inputs.add(new InputNode(this, 1, "in2", new Vec2i(-10, 30)));
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
	
	public abstract String getTextLabel();
	
	@Override
	public void updateIO() {
		
		this.outputs.get(0).setState(this.logicalFuntion.apply(this.inputs.get(0).getState(), this.inputs.get(1).getState()));
		
	}
	
	@Override
	public void render()  {
		
		EditorArea.swapColor(0, 1, 0, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, this.getTextLabel());
		
	}
	
}
