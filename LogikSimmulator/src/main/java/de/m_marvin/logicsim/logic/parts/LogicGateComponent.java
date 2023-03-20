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
	
	public static final String ICON_AND_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAJ5SURBVFhHxZe9axRBGMZnTSLBKk2aWAmChagBCUL+j9gpCLZp/AOs7EQQESztAsFAIDaKEjtBrMVWECFR8KsQMer6/Gb23czN7cfc4V0e+LEzs7PvPPfOx+4VZVnOF0Xx0wXNijIUveIy6rrnFOu4LsdCLU+pgb+iCMWRlWt8RsyJ31RSt3+iK2AohkBGKowbxI1hUGNQZKAqogPRFPx/yeIz1V6WARrM/SRl8esfaQZwRoqHUzRhmQGb+6nLDEw69abWKWBL+G0xbaXnAFOBqZyMnBNnxBexK3J2j8VnrbHm9NTgNqRDX6CL4rWg31513RcXRJ8svmV+yIAdNl16Jd6J877m3FXBMw99rVsW/zDDMsD5bcox8F48DcVab8VmKHZq2ECiHAPPBWtmydecuyV4Zs3XutUbn5t9Bi4J+uyIR1X5usiRxefggyFZhz69EfT7KJZpqHRD3AzFRln8epzD1Zgnts9LsSCeiEVxVphOimuhOJ5qZy16IL6LU77m3GNB/8u+5twLsR2KjbL4jeOwMmlkobTph9gIxVpm4n51vSKaFMcnk/Ur2ZRj4Ksg9ameCZ7lfGhTb/wcA3cFfe6JFXFarItfgsG5tyVWRare+CxIOvS9mu+Ib4K+8EncFicEC/CD+CxSdcZnPjgRcwwggnEQ2WEUi1/a1N5qwG4YOQbGUaMBGo9UGCBlsXA5Cdk4dXy9CGcsAzQexRfRHAaYE678W5m64jVgJ5OliNOK+zGk0RhVtv8HpjgOhIHGV2Sm4sADg0jxj2Kqfbb99yiFSmYg3ibpL43r42QBYcZn3htIvgljHehreZQzoctces8v+LIs5/8Bm93ThQdLg6UAAAAASUVORK5CYII=";
	public static final String ICON_OR_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIeSURBVFhHxZc9SsRQFIUnjqPT2IgLEDtB3IurELQStLGzsRDBRgXtBF2F6xBsBLWw8AdLhVHjOe/NCTcvyThx8nPgIzcvyc3JfT9JojiO+1EUfXa8pkHsQycbU6OOdZBrBpspvzeeQgM/IPJhaY1rvAt64Is7odtvsyU0ZGEiEYrGBfNaeFORFiswDKkByEtelZSfXe2kCrBB7uuU8icPKQN0xhJnS1SzZEB937hkoO7SS4VdwCnhpkXTkoFZwLnJcdCoZKA1hQbsWJgDGz6sTJkxoPVb0mpHLQDGpCojyp88aDj6eQLb1L4INsG22/PxqQ8TXQGOoTy9gXUfOoX5R3YBdQ92wDzYAyfgDtiKrIDnAl6BlfJz4SMZqeRFouEjwHMu2VBSyp/cJ3xi1wiF7RTHxDlYA49gH5yBZVDUBe/gwYdOyi9l7pM4M1oCF4DtT2ALWN0AXRdyDazC4ykHjDVKNTaOAQfeCzgEByDUKih6iX2AWx+m8nPR435q9WUDD/IkiiVnCXcBL5hUYf6M8k6wHyuT6k8DLDtPqOvVnJtffc0vouQzqWnJmWilAq2JBjg4rOiyDuk+SX68CLuqABvb+CLq0QD7hNsq5npp2TGgWaAScXXjcQvLKMpK8z/VxTYRDeS+IseUTZy6CWQfil3tqu3+SxkMJQN2moRPavf/UwWKZlzlnYHg39BqgL/mMmvCKHPhMTfg4zju/wLBR6nbYeJIXgAAAABJRU5ErkJggg==";
	public static final String ICON_NAND_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAJvSURBVFhHvZfPq05BHIfnuOhmZWPDStnJj5KUrX8BO0rZ2vgDrOykJGVpp0QpNkTZKVnLVkldlLCQXByfZ+Z8z513zrxz5rz3vp567pk5P77nc+b8mPc2bduuNk3z0wW2yzY0PXEbStucau3UYlvo1ZEG+Cub0JxMbfAVuUP+ppOm/RMtkUCxFDJTCG5SN5aTmrMwAl0T1mWu+FZh9bnVHhsBVlj6ZWL1+4u0ACRjiIdDtGQsgN37/44FWPbQG3NvAa+Efy0KcNAU60jeAm5F/cHTsfp24RuNBTgkT8tTcvFbmIyAfWxKHJOvJfutdcuP8ogcw+pvBO6+30ZNgFfynTzse86dlxxzx/fKDAMk1AR4L5+GZs9beS80i4zWZ+NYgOeSyWuv7zl3VXLMGd8rY/X58OGAmgAnJPs8kve79kVZg9U3B8zdkPBGst8neZQVHZflldDMYvX780x9DZkrXsrd8oncIw9KY5+8EJrT4ckkFQ/KPG7L73K/7zn3WHLMWd9z7oV8GJpZrD4X0k/JRk2AH/JuaPZYiFvd8pycR7F+TYCvkqFPeSY5lu9DiWJ9ngd2KE3NNyT73JTH5QF5Sf6SnJxtD+RJmWNufe4HX8SxAHBdfpPsi5/lNblL8gB+kF9kjmx9u3JzLMBmGNTfzGy4JRAgnRhIuUz6+poIV2wEWDn2iyiFY5kPbE6I4aLS9bkfvPyDMsMiQWohwEx9fovEz4B9mWyIOIDtsVyZORV7//tbAHEhAmSnyEriwjMnEfFFMQJ+6BmBXID4NUmvNO4vMgpAGD/yPgB/6GRY13/NU74JpXDpNv8ctG27+g9sjtwwPNFImgAAAABJRU5ErkJggg==";
	public static final String ICON_NOR_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIbSURBVFhHxZe7SsRAGIWN6+o2NuIDiJ0gvotPIWglaGNnYyGCjQraCfoUPodgI6iFhRcsFVaN50z2hD+zmZhsNtkDH5lL+Ofkn0uSKI7jXhRFX1OJZkCcFJ1smSrqm0KsWVymk1o5+QZ+QZQUK6us8Q7ogm9WfLc/5kpoyMJAwheNC8a1cFCRFTMwKFJ9kBd8XFJ8TrWTMsAGuW9Sip8+pAzQGVM8nKKGJQOa+9YlA02nXgpOAbeE2xZtSwbmAPcm10GrkoEiMV11Cco30Po21Pkt6bRrSooffFDfwDzYTIpj0b8PyE57wyJQ2ziMKBYPPjKUCg1u25fAFthxtaR8lhRTXQPupDy9g42kmMaXojK74AHsggWwD07BPbAZWQUvAd5AaSlFRaLpY8D7rthQQYqfjmNTzbIWSSgzXBMXYB08gQNwDlZAaAo+wCOw8XnosZ45fdnATt7kaxlcAvY/g21gdQvYl8cNoHLjs1GyDm0GTgAX3is4AofA1xoIvco/wR0IxU/FRnbaVzNTzhTuAaatjvLip+IXEU/EvBvsJ1sdBQ2oQzT1cZJrgI0TFQ3YhUjRZRPSOGl8vAg7ygAbJ/FF1KUBzgmvdVf5SLJrQD8LShH3NfstTKOoKh1AmSm2gWjAvSJHlA2cGQSyD8Wpdtl2/6UsDCQDdpv4T2rro2SBohmXeWfA+ze06uOvucqZUGTO73MLPo7j3h+ulr7CosSXgAAAAABJRU5ErkJggg==";
	public static final String ICON_XOR_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAJ9SURBVFhHxZc/aNVQFMYTa7WjiIsIDh0cxYKIi//QSacODnV16yCCoI6iuDi4izoo6OyiYieh0EWlUCjiYEUHEVwKLqVV4/ednK/vJC8vTYov7wc/zj034ebk3tzkvTTLsok0TdeSnJ0wy5tGbJO6YwnG2oWwI8+aUS7gL0zzZmuaFj4Gx+FvJuVq/4RIWVCUA8kyLFxy3CgvKotwBrxJNmDV4P8Ljc+lNjQD7FD1w0Tjb96kCmBlnOL+KRoyKkBr3zkqYNhTLwYuAbeEbYuuUQG7Ifcmn4NOUQEjo1zASLZh1bS/gufyprEPsu+eZTkX4WO4CN/A2zByDL6GU/AW/Ai3vEG9br/CT+xwHsJfcL9lSfIU8rwn8Dnkcea8oDgM2fcWfoMPPKcD0QkzHh/B896+AskRyPyaZT1uQvaftaxXwBd4kB2AOeWLj/ahE8hLqHyeHQ4LYd8py3och+y/YVmvgLuW5Wg8WbsLrnsk9z2SSY/vPIoPHnVcvPdYSV0Bsx7JZY9k1eNej2KPx+8eGxELiFvkBGQBVyHv/gLkU09WPJ7xKE56XPZYJo7Pry9ffAV4Ag9yJyzAuO5L8HPeNHiRH5BFnYaX4E/4Ago9A9OWFcevJJ7AeBQKXoR9dyxLkkNwDurcdfgMxs956wK4HDyh7af5gMetqB2fa8JftNspoCkDC9AB2WkB7BwpLEDbQ7DKYaDrbI6PX+RjmgF2juIX0TgL4Jow9r0YuiA+A/qzoCninubxKKdRtkX7v7DEcSAWUPmJbEgcuHAREG+KS22zbf9L2XBUQNwm5TuN+XZmgbAYm3kroPTfMLKBf81t3gl1xZWP2QOfZdnEP0hTu5cb2D3MAAAAAElFTkSuQmCC";
	
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
