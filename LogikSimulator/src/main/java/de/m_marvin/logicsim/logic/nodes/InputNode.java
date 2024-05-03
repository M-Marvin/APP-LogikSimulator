package de.m_marvin.logicsim.logic.nodes;

import java.util.Map;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Represents an input node on an component in an circuit.
 * Has additional methods to allow the component to read the state of the network the node is connected to and to get the ciruit-internal map that stores the states of each lane.
 * 
 * @author Marvin K.
 *
 */
public class InputNode extends Node {
	
	public InputNode(Component component, int nodeNr, String label, Vec2i visualOffset) {
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
		
	public boolean getState() {
		return Circuit.safeLaneRead(this.getLaneReference(), getLaneTag()).getLogicState();
	}
	
	public int readBusValue(int bitCount) {
		String[] s = this.getLaneTag().split("(?<=\\D)(?=\\d)");
		String inputBus = s[0];
		int indexOffset = s.length > 1 ? Integer.valueOf(s[1]) : 0;
		Map<String, NetState> laneData = this.getLaneReference();
		
		// TODO Optimize
		int value = 0;
		for (String lane : laneData.keySet()) {
			String[] busTag = lane.split("(?<=\\D)(?=\\d)");
			if (busTag.length == 2 && busTag[0].equals(inputBus)) {
				int bitIndex = Integer.parseInt(busTag[1]);
				if (bitIndex >= indexOffset && bitIndex < indexOffset + bitCount && Circuit.safeLaneRead(laneData, lane).getLogicState()) value |= (1 << (bitIndex - indexOffset));
			}
		}
		
		return value;
	}
	
}
