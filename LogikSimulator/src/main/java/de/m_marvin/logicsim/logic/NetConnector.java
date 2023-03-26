package de.m_marvin.logicsim.logic;

public abstract class NetConnector extends Component {
		
	public NetConnector(Circuit circuit) {
		super(circuit);
	}
	
	@Override
	public void updateIO() {}
	
}
