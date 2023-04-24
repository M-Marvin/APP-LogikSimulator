package de.m_marvin.logicsim.logic.simulator;

import java.util.Collection;
import java.util.function.Supplier;

import de.m_marvin.logicsim.logic.Circuit;

public class SimulationMonitor {
	
	protected final CircuitProcessor processor;
	protected long lastRefresh;
	
	protected Collection<CircuitProcessInfo> cachedProcessInfo;
	protected Collection<CircuitProcessorInfo> cachedProcessorInfo;
	
	public SimulationMonitor(CircuitProcessor processor) {
		this.processor = processor;
	}
	
	public CircuitProcessor getProcessor() {
		return processor;
	}
	
	public void setTPSLimit(int tps) {
		this.processor.setMinFrameTime((1 / tps) * 1000F);
	}
	
	public int getTPSLimit() {
		return (int) (1 / (this.processor.getMinFrameTime() / 1000));
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
	
	public static record CircuitProcessInfo(Circuit circuit, Circuit parentCircuit, Supplier<Integer> executionTime, Supplier<Boolean> active, Supplier<Boolean> executing) {
		public boolean isActive() {
			return this.active.get();
		}
		public boolean isExecuting() {
			return this.executing.get();
		}
	}
	
	public Collection<CircuitProcessInfo> getRunningProcesses() {
		if (this.cachedProcessInfo == null) {
			synchronized (this.processor) {
				this.cachedProcessInfo = this.processor.getProcesses().stream().map(process -> 
					new CircuitProcessInfo(process.circuit, process.parentCircuit, () -> (int) process.executionTime, () -> {
						synchronized (this.processor) { return this.processor.holdsCircuit(process.circuit); }
					}, () -> {
						synchronized (this.processor) { return this.processor.isExecuting(process.circuit); }
					})
				).toList();
			}
		}
		return this.cachedProcessInfo;
	}
	
	public static record CircuitProcessorInfo(Supplier<Collection<Circuit>> circuits, Supplier<Integer> tps, Supplier<Integer> executionTime) {}
	
	public Collection<CircuitProcessorInfo> getActiveProcessors() {
		if (this.cachedProcessorInfo == null) {
			synchronized (this.processor) {
				this.cachedProcessorInfo = this.processor.getProcessors().stream().map(processor ->
					new CircuitProcessorInfo(() -> processor.processes.stream().map(process -> process.circuit).toList(), () -> processor.tps, () -> (int) processor.executionTime)
				).toList();
			}
		}
		return this.cachedProcessorInfo;
	}
	
	public void refresh() {
		this.cachedProcessInfo = null;
		this.cachedProcessorInfo = null;
	}

	public void update() {
		long current = System.currentTimeMillis();
		if (current - lastRefresh > 1000) {
			lastRefresh = current;
			refresh();
		}
	}
	
}
