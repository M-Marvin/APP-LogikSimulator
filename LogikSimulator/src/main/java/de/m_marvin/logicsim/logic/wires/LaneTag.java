package de.m_marvin.logicsim.logic.wires;

import java.util.function.Supplier;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.NetConnector;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.univec.impl.Vec2i;

public class LaneTag extends NetConnector {

	/* Factory methods */
	
 public static void placeClick(Circuit circuit, Vec2i coursorPosition) {
//		if (currentComponent != null) circuit.reconnect(false, currentComponent);
//		currentComponent = null;
	}
	
	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
//		if (currentComponent == null) {
//			currentComponent = constructor.get();
//			circuit.add(currentComponent);
//			placeOffset = new Vec2i(-currentComponent.getVisualWidth(), -currentComponent.getVisualHeight()).div(EditorArea.RASTER_SIZE).div(2).mul(EditorArea.RASTER_SIZE);
//		}
//		coursorPosition.x = Math.max(-placeOffset.x, coursorPosition.x);
//		coursorPosition.y = Math.max(-placeOffset.y, coursorPosition.y);
//		currentComponent.setVisualPosition(coursorPosition.add(placeOffset));
		return true;
	}
	
	public static void abbortPlacement(Circuit circuit) {
//		if (currentComponent != null) {
//			circuit.remove(currentComponent);
//			currentComponent = null;
//		}
	}
	
	/* End of factory methods */
	
	public LaneTag(Circuit circuit) {
		super(circuit);
	}

	@Override
	public int getVisualWidth() {
		return 10;
	}

	@Override
	public int getVisualHeight() {
		return 10;
	}

	@Override
	public void render() {
		
	}

}
