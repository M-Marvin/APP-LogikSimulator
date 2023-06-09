package de.m_marvin.logicsim.logic.nodes;

import java.util.Map;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.simulator.FastAsyncMap;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Represents an output node on an component in an circuit.
 * Has additional methods to allow the component to write the states of the network the node is connected to.
 * 
 * @author Marvin K.
 *
 */
public class OutputNode extends Node {

	public OutputNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
		super(component, nodeNr, label, visualOffset);
	}

	@Override
	public boolean click(Vec2i mousePosition) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		InputDialog configDialog = new InputDialog(editor.getShell(), "editor.config.change_tag", getLaneTag(), this::setLaneTag);
		configDialog.open();
		configDialog.setLocation(mousePosition.x, mousePosition.y);
		return true;
	}
	
	public void writeLanes(Map<String, NetState> laneStates) {
		this.getCircuit().writeLanes(this, laneStates);
	}
	
	public void setState(boolean state) {
		this.getCircuit().setNetState(this, state ? NetState.HIGH : NetState.LOW, getLaneTag());
	}
	
	protected static Map<String, NetState> writeCache = new FastAsyncMap<>();
	
	public void writeBusValue(int value, int bitCount) {
		String[] s = getLaneTag().split("(?<=\\D)(?=\\d)");
		String outputBus = s[0];
		int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
		for (int i = 0; i < bitCount; i++) {
			boolean state = (value & (1 << i + indexOffset)) > 0;
			writeCache.put(outputBus + (i + indexOffset), state ? NetState.HIGH : NetState.LOW);
		}
		writeLanes(writeCache);
	}
	
}
