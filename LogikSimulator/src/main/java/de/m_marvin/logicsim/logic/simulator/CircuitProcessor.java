package de.m_marvin.logicsim.logic.simulator;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.ui.windows.Editor;

/**
 * This class handles the creation and load balancing of the multiple simulation threads.
 * The objects returned by the getProcesses and getProcessors methods are <b>not thread safe</b> and should not be used directly.
 * Informations about these can be obtained by the SimulationMonitor.
 * 
 * @author Marvin K.
 */
public class CircuitProcessor {
	
	/**
	 * Represents an circuit that is executed on an thread
	 * Contains execution-time data and a reference to the parent circuit (which is null if this is the main process).
	 * 
	 * @author Marvin K.
	 */
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
			executionEnd = getCurrentTime();
			executionTime = executionEnd - executionStart;
			executionStart = getCurrentTime();
			circuit.updateCircuit();
		}
		
	}
	
	/**
	 * Represents a thread that is running some circuit simulations.
	 * Contains a list of circuit process that are executed and some execution time data.
	 * Normally there should not be more simulation threads as CPU cores.
	 * 
	 * @author Marvin K.
	 */
	public class CircuitProcessorThread extends Thread {
		
		public final List<CircuitProcess> processes = new ArrayList<>();
		
		public long executionStart;
		public long executionEnd;
		public long executionTime;
		public int tps;
		
		public CircuitProcessorThread(String name) {
			super(name);
		}
		
		@Override
		public void run() {
			try {
				float frameDelta = 0;
				long lastFrameTime = 0;
				long frameTime = 0;
				int frameCount = 0;
				long secondTimer = getCurrentTime();
				while (!requestShutdown) {
					try {
						if (allowedToExecute) {
							
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
							}
						} else {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
						frameDelta = 0;
						frameTime = getCurrentTime();
						secondTimer = getCurrentTime();
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				LogicSim.getInstance().getDisplay().asyncExec(() -> {
					if (!LogicSim.getInstance().getLastInteractedEditor().getShell().isDisposed()) Editor.showErrorInfo(LogicSim.getInstance().getLastInteractedEditor().getShell(), "editor.window.error.processor_crash", e);
				});
				this.processes.forEach(process -> process.active = false);
			}
		}
		
	}
	
	protected boolean resetCircuits = true;
	protected boolean allowedToExecute = false;
	protected boolean requestShutdown = false;
	protected List<CircuitProcessorThread> threads = new ArrayList<>();
	protected Map<Circuit, CircuitProcess> inactives = new HashMap<>();
	protected Map<Circuit, CircuitProcess> processes = new HashMap<>();
	protected CircuitProcess mainProcess = null;
	protected Thread processorMasterThread;
	protected float minFrameTime = 50;
	protected double cpuLoad = 0;
	protected OperatingSystemMXBean osBean;
	protected long cpuLoadTimer = 0;
	
	public CircuitProcessor() {
		
		System.out.println("Create new circuit processor ...");
		
		try {
			this.osBean = ManagementFactory.newPlatformMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);	
		} catch (IOException e) {
			System.err.println("Failed to create mx os bean");
			this.osBean = null;
			e.printStackTrace();
		}

		System.out.println("Start processor threads for " + getAvailableCores() + " cores ...");
		for (int i = 0; i < getAvailableCores(); i++) {
			this.threads.add(new CircuitProcessorThread("processor-" + i));
		}
		this.threads.forEach(Thread::start);

		System.out.println("Start processor master thread");
		this.processorMasterThread = new Thread(() -> {
			while (!requestShutdown) {
				try {
					this.update();
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			System.out.println("Processor master thread terminated!");
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
	
	@SuppressWarnings("deprecation")
	protected void update() {
		
		// Find least and most loaded thread and remove inactive processes
		
		long leastExecutions = -1;
		long mostExecutions = 0;
		CircuitProcessorThread slowest = null;
		CircuitProcessorThread fastest = null;
		for (CircuitProcessorThread thread : this.threads) {
			if (mostExecutions < thread.processes.size()) {
				mostExecutions = thread.processes.size();
				slowest = thread;
			}
			if (leastExecutions > thread.processes.size() || leastExecutions == -1) {
				leastExecutions = thread.processes.size();
				fastest = thread;
			}
			synchronized (thread) { thread.processes.removeAll(inactives.values()); }
		}
		
		this.inactives.forEach(this.processes::remove);
		
		this.inactives.clear();
		
		if (allowedToExecute || resetCircuits) {
			
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
					if (this.resetCircuits) process.circuit.resetNetworks();
					if (!process.active) {
						newProcesses.add(process);
						process.active = true;
					} else if (process.parentCircuit != null ? !holdsCircuit(process.parentCircuit) : this.mainProcess != process) {
						process.active = false;
						inactives.put(process.circuit, process);
					}
				}
				this.resetCircuits = false;
			}
			synchronized (fastest) { fastest.processes.addAll(newProcesses); }
			
		}
		
		if (getCurrentTime() - cpuLoadTimer > 1000) {
			cpuLoad = osBean == null ? -1 : (float) osBean.getSystemCpuLoad();;
		}
		
	}
	
	public synchronized void removeProcess(Circuit circuit) {
		if (processes.containsKey(circuit)) this.processes.remove(circuit).active = false;		
	}
	
	public synchronized void addProcess(Circuit ownerCircuit, Circuit circuit) {
		CircuitProcess process = new CircuitProcess(ownerCircuit, circuit);
		this.processes.put(circuit, process);
		circuit.resetNetworks();
		if (ownerCircuit == null) {
			this.mainProcess = process;
			this.stop();
		}
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
	
	public void setMinFrameTime(float frameTime) {
		minFrameTime = frameTime;
	}
	
	public float getMinFrameTime() {
		return minFrameTime;
	}
	
	public void start() {
		this.allowedToExecute = true;
		this.threads.forEach(Thread::interrupt);
	}

	public void pause() {
		this.allowedToExecute = false;
	}

	public void stop() {
		this.allowedToExecute = false;
		this.resetCircuits = true;
	}
	
	public void terminate() {
		System.out.println("Shutdown circuit processor ...");
		stop();
		this.requestShutdown = true;
	}
	
}
