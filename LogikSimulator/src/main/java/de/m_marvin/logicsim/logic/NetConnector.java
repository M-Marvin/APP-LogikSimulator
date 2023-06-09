package de.m_marvin.logicsim.logic;

import java.util.Map;

import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.univec.impl.Vec2i;

/**
 * Just an abstract base class for the non-functional components that are only relevant on network-compile time.
 * Components that extend this class should not implement any input/output nodes or any functionality.
 * This class is only used by components like wires.
 * 
 * @author Marvin K.
 *
 */
public abstract class NetConnector extends Component {
		
	public NetConnector(Circuit circuit) {
		super(circuit);
	}
	
	@Override
	public void updateIO() {}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {}
	
	public abstract Map<String, NetState> getLaneData();
	
}
