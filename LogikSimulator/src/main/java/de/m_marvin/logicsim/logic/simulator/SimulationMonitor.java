package de.m_marvin.logicsim.logic.simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.NetConnector;
import de.m_marvin.logicsim.ui.Translator;
import de.m_marvin.logicsim.ui.widgets.EditorArea.SimulationWarning;

public class SimulationMonitor {

	public static final int WARNING_DECAY_TIME = 5000;
	
	protected final CircuitProcessor processor;
	protected long lastRefresh;
	
	protected Map<Circuit, CircuitProcessInfo> cachedProcessInfo = new HashMap<>();
	protected Collection<CircuitProcessorInfo> cachedProcessorInfo = new ArrayList<>();

	public static record CircuitProcessInfo(Circuit circuit, Circuit parentCircuit, Supplier<Integer> executionTime, Supplier<Boolean> active, Supplier<Boolean> executing, List<SimulationWarning> warnings) {
		public boolean isActive() {
			return this.active.get();
		}
		public boolean isExecuting() {
			return this.executing.get();
		}
		public boolean hasWarnings() {
			return !this.warnings.isEmpty();
		}
	}

	public static record CircuitProcessorInfo(Supplier<Collection<Circuit>> circuits, Supplier<Integer> tps, Supplier<Integer> executionTime) {}
	
	public SimulationMonitor(CircuitProcessor processor) {
		this.processor = processor;
	}
	
	public CircuitProcessor getProcessor() {
		return processor;
	}
	
	public void setTPSLimit(int tps) {
		this.processor.setMinFrameTime(tps > 0 ? (1F / tps) * 1000F : -1);
	}
	
	public int getTPSLimit() {
		return (int) ((1F / (this.processor.getMinFrameTime()) * 1000F));
	}
	
	public int getCoreCount() {
		return CircuitProcessor.getAvailableCores();
	}
	
	public int getUsedCores() {
		return this.cachedProcessorInfo != null ? this.cachedProcessorInfo.size() : 0;
	}
	
	public double getCPULoad() {
		return this.processor.cpuLoad;
	}
	
	public Optional<NetState> queryWarningState(Circuit circuit, Map<String, NetState> laneData) {
		
		if (laneData == null) return Optional.empty();
		if (laneData.isEmpty()) return Optional.of(NetState.FLOATING);
		return laneData.keySet().stream().map(v -> laneData.get(v)).filter(s -> s == null ? false : s.isErrorState()).findAny();
		
	}
	
	public void queryWarnings() {
		
		if (this.cachedProcessInfo == null) return;
		
		this.cachedProcessInfo.forEach((circuit, processInfo) -> {
			
			boolean running = getProcessor().isExecuting(circuit);

			if (!running) return;
			
			circuit.getComponents().forEach(component -> {
				
				if (component instanceof NetConnector connector) {
					
					if (connector.getPassives().size() == 0) return;
					
					Optional<NetState> state = queryWarningState(circuit, connector.getLaneData());
					
					if (state.isPresent() && state.get().isErrorState()) {
						
						// TODO Optimize
						for (SimulationWarning w : processInfo.warnings) {
							if (w.component == component) return;
						}
						String message = Translator.translate("editor_area.warning." + (state.get() == NetState.FLOATING ? "floating" : "short_circuit"));
						processInfo.warnings.add(new SimulationWarning(component, null, message, () -> {
							Optional<NetState> state2 = queryWarningState(circuit, connector.getLaneData());
							return state2.isPresent() && state2.get() == state.get() && running;
						}, System.currentTimeMillis() + WARNING_DECAY_TIME));
						
					}
					
				} else {
					
					component.getInputs().forEach(inputNode -> {
						
						NetState state = circuit.getNetState(inputNode, inputNode.getLaneTag());
						Map<String, NetState> laneData = inputNode.getLaneReference();
						if (laneData == null) return;
						
						if (state.isErrorState() && (laneData.isEmpty() || laneData.containsKey(inputNode.getLaneTag()))) {
							
							// TODO Optimize
							for (SimulationWarning w : processInfo.warnings) {
								if (w.component == component && w.node == inputNode) return;
							}
							String message = Translator.translate("editor_area.warning." + (state == NetState.FLOATING ? "floating" : "short_circuit"));
							processInfo.warnings.add(new SimulationWarning(component, inputNode, message, () -> {
								NetState state2 = circuit.getNetState(inputNode, inputNode.getLaneTag());
								return state2 == state && running && (laneData.isEmpty() || laneData.containsKey(inputNode.getLaneTag()));
							}, System.currentTimeMillis() + WARNING_DECAY_TIME));
							
						}
						
					});
					
				}
				
			});
			
			List<SimulationWarning> outdatedWarnings = new ArrayList<>();
			processInfo.warnings.forEach(warning -> {
				
				if (warning.stillValid.get()) {
					warning.decayTime = System.currentTimeMillis() + WARNING_DECAY_TIME;
				}
				if (warning.decayTime <= System.currentTimeMillis()) {
					outdatedWarnings.add(warning);
				}
				if (!circuit.getComponents().contains(warning.component)) outdatedWarnings.add(warning);
				
			});
			processInfo.warnings.removeAll(outdatedWarnings);
			
		});
		
	}
	
	public Collection<CircuitProcessInfo> getRunningProcesses() {
		return this.cachedProcessInfo.values();
	}
	
	public Optional<CircuitProcessInfo> getProcessForCircuit(Circuit circuit) {
		if (this.cachedProcessInfo == null) getRunningProcesses();
		if (circuit == null) return Optional.empty();
		return Optional.ofNullable(this.cachedProcessInfo.get(circuit));
	}
	
	public Collection<CircuitProcessorInfo> getActiveProcessors() {
		return this.cachedProcessorInfo;
	}
	
	public void refresh() {
		
		synchronized (this.processor) {
			
			this.cachedProcessInfo.keySet().stream().filter(circuit -> !this.processor.isExecuting(circuit)).toList().forEach(circuit -> this.cachedProcessInfo.remove(circuit));
			
			this.processor.getProcesses().forEach(process -> {
				
				if (!this.cachedProcessInfo.containsKey(process.circuit)) 
					this.cachedProcessInfo.put(process.circuit, 
						new CircuitProcessInfo(process.circuit, process.parentCircuit, () -> (int) process.executionTime, () -> {
							synchronized (this.processor) { return this.processor.holdsCircuit(process.circuit); }
						}, () -> {
							synchronized (this.processor) { return this.processor.isExecuting(process.circuit); }
						}, new ArrayList<>())
					);
				
			});
			
			queryWarnings();
			
			// TODO Optimize
			this.cachedProcessorInfo = this.processor.getProcessors().stream().map(processor ->
				new CircuitProcessorInfo(() -> {
					List<Circuit> circuits = new ArrayList<>();
					synchronized (processor) {
						for (int i = 0; i < processor.processes.size(); i++) {
							circuits.add(processor.processes.get(i).circuit);
						}
					}
					return circuits;
				}, () -> processor.tps, () -> (int) processor.executionTime)
			).toList();
			
		}
		
	}

	public void update() {
		long current = System.currentTimeMillis();
		if (current - lastRefresh > 1000) {
			lastRefresh = current;
			refresh();
		}
	}
	
	public Optional<CircuitProcessorInfo> getProcessorForCircuit(Circuit circuit) {
		if (this.cachedProcessorInfo == null) getActiveProcessors();
		if (circuit == null) return Optional.empty();
		for (CircuitProcessorInfo processorInfo : this.cachedProcessorInfo) {
			if (processorInfo.circuits().get().contains(circuit)) return Optional.ofNullable(processorInfo);
		}
		return Optional.empty();
	}

	public boolean isProcessorActive() {
		return this.getProcessor().allowedToExecute;
	}
	
}
