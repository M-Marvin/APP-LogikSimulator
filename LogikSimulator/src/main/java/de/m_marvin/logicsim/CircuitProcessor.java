package de.m_marvin.logicsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.ui.Editor;

public class CircuitProcessor {
	
	public class CircuitProcess implements Runnable {
		
		public long executionStart;
		public long executionEnd;
		public long executionTime;
		protected boolean active;
		
		public final Circuit parentCircuit;
		public final Circuit circuit;
		
		public CircuitProcess(Circuit parentCircuit, Circuit circuit) {
			this.parentCircuit = parentCircuit;
			this.circuit = circuit;
		}
		
		@Override
		public void run() {
			executionStart = getCurrentTime();
			circuit.updateCircuit();
			executionEnd = getCurrentTime();
			executionTime = executionEnd - executionStart;
		}
		
	}
	
	public class CircuitProcessorThread extends Thread {
		
		public final List<CircuitProcess> processes = new ArrayList<>();
		public long lastExecutionTime;
		
		public CircuitProcessorThread(String name) {
			super(name);
		}
		
		@Override
		public void run() {
			try {
				while (!requestShutdown) {
					try {
						if (allowedToExecute) {
<<<<<<< Updated upstream:LogikSimulator/src/main/java/de/m_marvin/logicsim/CircuitProcessor.java
							this.lastExecutionTime = 0;
							synchronized (this) {
								this.processes.forEach(process -> {
									if (process.active) process.run();
									CircuitProcessorThread.this.lastExecutionTime += process.executionTime;
								});
							}
							if (this.processes.isEmpty()) {
								Thread.sleep(1000);
=======
							if (minFrameTime > 0) {
								lastFrameTime = frameTime;
								frameTime = getCurrentTime();
								frameDelta += (frameTime - lastFrameTime) / minFrameTime;
								if (frameDelta > 20 || frameDelta < 0) frameDelta = 0;
							}
							if (getCurrentTime() - secondTimer > 1000) {
								secondTimer = getCurrentTime();
								tps = frameCount;
								frameCount = 0;
							}
							if (minFrameTime <= 0 || frameDelta >= 1) {
								frameDelta--;
								frameCount += 1;
								this.executionEnd = getCurrentTime();
								this.executionTime = this.executionEnd - this.executionStart;
								this.executionStart = getCurrentTime();
								for (int i = 0; i < this.processes.size(); i++) {
									CircuitProcess process = processes.size() > i ? processes.get(i) : null;
									if (process != null ? process.active : false) process.run();
								}
								if (this.processes.isEmpty()) {
									Thread.sleep(1000);
								}
							} else {
								Thread.sleep((long) (minFrameTime / 2));
>>>>>>> Stashed changes:LogikSimulator/src/main/java/de/m_marvin/logicsim/logic/simulator/CircuitProcessor.java
							}
						} else {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {}
				}
			} catch (Throwable e) {
				LogicSim.getInstance().getDisplay().asyncExec(() -> {
					if (!LogicSim.getInstance().getLastInteractedEditor().getShell().isDisposed()) Editor.showErrorInfo(LogicSim.getInstance().getLastInteractedEditor().getShell(), "editor.window.error.processor_crash", e);
				});
				this.processes.forEach(process -> process.active = false);
			}
		}
		
	}
	
	protected boolean allowedToExecute = false;
	protected boolean requestShutdown = false;
	protected List<CircuitProcessorThread> threads = new ArrayList<>();
	protected Map<Circuit, CircuitProcess> inactives = new HashMap<>();
	protected Map<Circuit, CircuitProcess> processes = new HashMap<>();
	protected CircuitProcess mainProcess = null;
	protected Thread processorMasterThread;
	
	public CircuitProcessor() {
		
		for (int i = 0; i < getAvailableCores(); i++) { // TODO
			this.threads.add(new CircuitProcessorThread("processor-" + i));
		}
		this.threads.forEach(Thread::start);
		
		this.processorMasterThread = new Thread(() -> {
			while (!requestShutdown) {
				try {
					this.update();
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}, "processor-master");
		this.processorMasterThread.start();
		
	}
	
	public Collection<CircuitProcess> getProcesses() {
		return processes.values();
	}
	
	public Collection<CircuitProcessorThread> getProcessors() {
		return threads;
	}
	
	public static int getAvailableCores() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	public static long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	public void update() {
		
		// Find slowest and fastest thread and remove inactive processes
		
		long fastestExecution = -1;
		long slowestExecution = 0;
		CircuitProcessorThread slowest = null;
		CircuitProcessorThread fastest = null;
		for (CircuitProcessorThread thread : this.threads) {
			if (slowestExecution < thread.lastExecutionTime) {
				slowestExecution = thread.lastExecutionTime;
				slowest = thread;
			}
			if (fastestExecution > thread.lastExecutionTime || fastestExecution == -1) {
				fastestExecution = thread.lastExecutionTime;
				fastest = thread;
			}
			synchronized (thread) { thread.processes.removeAll(inactives.values()); }
		}
		
		this.inactives.forEach(this.processes::remove);
		
		this.inactives.clear();
		
		if (allowedToExecute) {
			
			// Distribute processing load over active threads
			
			if (fastest == null) return;
			
			if (slowest != fastest && slowest != null && slowest.processes.size() > 1) {
				
				CircuitProcess process;
				synchronized (slowest) { process = slowest.processes.remove(0); }
				synchronized (fastest) { fastest.processes.add(process); }
				
			}
			
			// Put queued processes in active threads
			
			List<CircuitProcess> newProcesses = new ArrayList<>();
			synchronized (this) {
				for (CircuitProcess process : this.processes.values()) {
					if (!process.active) {
						newProcesses.add(process);
						process.active = true;
					} else if (process.parentCircuit != null ? !holdsCircuit(process.parentCircuit) : this.mainProcess != process) {
						process.active = false;
						inactives.put(process.circuit, process);
					}
				}
			}
			synchronized (fastest) { fastest.processes.addAll(newProcesses); }
			
		}
		
<<<<<<< Updated upstream:LogikSimulator/src/main/java/de/m_marvin/logicsim/CircuitProcessor.java
=======
		if (getCurrentTime() - cpuLoadTimer > 1000) {
			cpuLoad = (float) osBean.getSystemCpuLoad();
		}
		
>>>>>>> Stashed changes:LogikSimulator/src/main/java/de/m_marvin/logicsim/logic/simulator/CircuitProcessor.java
	}
	
	public void removeProcess(Circuit circuit) {
		if (processes.containsKey(circuit)) this.processes.remove(circuit).active = false;		
	}
	
	public synchronized void addProcess(Circuit ownerCircuit, Circuit circuit) {
		CircuitProcess process = new CircuitProcess(ownerCircuit, circuit);
		this.processes.put(circuit, process);
		if (ownerCircuit == null) this.mainProcess = process;
	}
	
	public boolean isExecuting(Circuit circuit) {
		return this.processes.containsKey(circuit) && allowedToExecute && this.processes.get(circuit).active;
	}

	public boolean holdsCircuit(Circuit circuit) {
		return this.processes.containsKey(circuit);
	}
	
	public CircuitProcessorThread getProcessorThreadOf(CircuitProcess process) {
		for (CircuitProcessorThread processor : this.threads) {
			if (processor.processes.contains(process)) return processor;
		}
		return null;
	}
	
	public void start() {
		this.threads.forEach(Thread::interrupt);
		this.allowedToExecute = true;
	}

	public void pause() {
		this.allowedToExecute = false;
	}

	public void stop() {
		this.allowedToExecute = false;
		this.processes.keySet().forEach(circuit -> circuit.resetNetworks());
	}
	
	public void terminate() {
		stop();
		this.requestShutdown = true;
	}
	
}
