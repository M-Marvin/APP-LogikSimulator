package de.m_marvin.logicsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.m_marvin.logicsim.logic.Circuit;

public class CircuitProcessor {
	
	public class CircuitProcess implements Runnable {
		
		public long executionStart;
		public long executionEnd;
		public long executionTime;
		public long lastNotification;
		public boolean active;
		
		public final Circuit parentCircuit;
		public final Circuit circuit;
		
		public CircuitProcess(Circuit parentCircuit, Circuit circuit) {
			this.parentCircuit = parentCircuit;
			this.circuit = circuit;
			this.lastNotification = getCurrentTime();
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
		
		public List<CircuitProcess> processes = new ArrayList<>();
		public long lastExecutionTime;
		
		public String name2;
		
		public CircuitProcessorThread(String name) {
			super(name);
			name2 = name;
		}
		
		@Override
		public void run() {
			try {
				while (!requestShutdown) {
					if (allowedToExecute) {
						this.lastExecutionTime = 0;
						synchronized (this) {
							this.processes.forEach(process -> {
								if (process.active) process.run();
								CircuitProcessorThread.this.lastExecutionTime += process.executionTime;
							});
						}
						if (this.processes.isEmpty()) {
							Thread.sleep(1000);
						}
					} else {
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	protected boolean allowedToHandle = false;
	protected boolean allowedToExecute = false;
	protected boolean requestShutdown = false;
	protected List<CircuitProcessorThread> threads = new ArrayList<>();
	protected Map<Circuit, CircuitProcess> inactives = new HashMap<>();
	protected Map<Circuit, CircuitProcess> processes = new HashMap<>();
	protected CircuitProcess mainProcess = null;
	protected Thread processorMasterThread;
	
	public CircuitProcessor() {
		
		for (int i = 0; i < 1; i++) { // TODO
			this.threads.add(new CircuitProcessorThread("processor-" + i));
		}
		this.threads.forEach(Thread::start);
		
		this.processorMasterThread = new Thread(() -> {
			try {
				while (!requestShutdown) {
					this.update();
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				System.err.println("Unexpected error occured in circuit processor master thread!");
				terminate();
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
		return System.nanoTime();
	}
	
	public void update() {
		
		if (this.mainProcess != null) this.mainProcess.lastNotification = getCurrentTime();
		
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
		
		
		if (allowedToHandle) {
			
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
					} else if (getCurrentTime() - process.lastNotification > 2000000 && process.parentCircuit != null) {
						process.active = false;
						inactives.put(process.circuit, process);
					}
				}
			}
			synchronized (fastest) { fastest.processes.addAll(newProcesses); }
			
		}
		
	}
	
	public synchronized void addProcess(Circuit ownerCircuit, Circuit circuit) {
		CircuitProcess process = new CircuitProcess(ownerCircuit, circuit);
		this.processes.put(circuit, process);
		if (ownerCircuit == null) this.mainProcess = process;
	}

	public void notifyActivity(Circuit circuit) {
		CircuitProcess process = this.processes.get(circuit);
		if (process != null) {
			process.lastNotification = getCurrentTime();
		}
	}
	
	public boolean isExecuting(Circuit circuit) {
		return this.processes.containsKey(circuit) && allowedToExecute && this.processes.get(circuit).active;
	}

	public boolean holdsCircuit(Circuit circuit) {
		return this.processes.containsKey(circuit);
	}
	
	public void start() {
		this.processes.keySet().forEach(this::notifyActivity);
		this.allowedToExecute = true;
		this.allowedToHandle = true;
	}

	public void pause() {
		this.allowedToExecute = false;
		this.allowedToHandle = false;
	}

	public void stop() {
		this.allowedToExecute = false;
		this.allowedToHandle = true;
	}
	
	public void terminate() {
		stop();
		this.requestShutdown = true;
	}
	
}
