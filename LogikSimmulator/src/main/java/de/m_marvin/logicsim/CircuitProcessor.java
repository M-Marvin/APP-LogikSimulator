package de.m_marvin.logicsim;

import java.util.List;

import de.m_marvin.logicsim.logic.Circuit;

public class CircuitProcessor {
	
	protected record CircuitProcess(Circuit circuit, List<Circuit> subProcesses) {}
	
	protected List<CircuitProcessor> processes;
	
	public void start() {
		
	}
	
	public void terminate() {
		
	}
	
	public void addProcess(Circuit ownerCircuit, Circuit subCircuit) {
		
	}
	
}
