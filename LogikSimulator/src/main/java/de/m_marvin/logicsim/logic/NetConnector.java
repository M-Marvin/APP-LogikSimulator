package de.m_marvin.logicsim.logic;

import java.util.Optional;

public abstract class NetConnector extends Component {
		
	public NetConnector(Circuit circuit) {
		super(circuit);
	}

	public abstract Optional<String> getBusLabel();
	
	@Override
	public void updateIO() {}
	
}
