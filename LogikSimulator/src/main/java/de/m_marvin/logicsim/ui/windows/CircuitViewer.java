package de.m_marvin.logicsim.ui.windows;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.simulator.CircuitProcessor.CircuitProcess;
import de.m_marvin.logicsim.logic.simulator.SimulationMonitor;
import de.m_marvin.logicsim.logic.simulator.SimulationMonitor.CircuitProcessInfo;
import de.m_marvin.logicsim.ui.Translator;

public class CircuitViewer {
	
	public static final String RUNNING_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABESURBVChTY2D4z/EfjHEARgxJxh+MUBYYYCqAAahC3AqgAKcC3vM/wDSGApjEZyOgHBDAFaBLwAHvOYb/IAzlogEGBgAn2R2n/6vpZQAAAABJRU5ErkJggg==";
	public static final String SUSPENDED_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAsSURBVChTYzxyjuE/AxBcucTAkJHAwAhiI4sxgRj4wNBQAPIKCCMDhBgDAwCAMw/f3Kx0cwAAAABJRU5ErkJggg==";
	public static final String ERROR_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAA2SURBVChTY0AG/4HoBphCACYoTRiAdMMwsinEmYCsG4ZhphA0gRGkGsrGADeBmBHdW6iAgQEAcBIcKuG5HsgAAAAASUVORK5CYII=";
	
	protected Shell shell;
	
	protected Map<Circuit, TreeItem> viewItems = new HashMap<>(); 
	protected Tree treeView;
	protected ProgressBar processorLoadBar;
	protected Label processorLoadLabel;
	protected Label executionTimeLabel;
	protected Label parentProcessLabel;
	protected Button openInEditorButton;
	protected long lastViewUpdate;
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public CircuitViewer(Display display) {
		this.shell = new Shell(display);
		this.shell.setImage(decodeImage(LogicSim.LOGIC_SIM_ICON));
		this.shell.setText(Translator.translate("circuit_viewer.title"));
		this.shell.setLayout(new BorderLayout());
		
		this.treeView = new Tree(shell, SWT.SINGLE);
		this.treeView.setLayoutData(new BorderData(SWT.CENTER));
		
		Group processGroup = new Group(shell, SWT.NONE);
		processGroup.setText(Translator.translate("circuit_viewer.process_group"));
		processGroup.setLayoutData(new BorderData(SWT.BOTTOM));
		processGroup.setLayout(new GridLayout(2, false));
		
		Label processorLoadTitle = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		processorLoadTitle.setText(Translator.translate("circuit_viewer.process_group.process_load.title"));
		
		Composite processLoadComp = new Composite(processGroup, SWT.NONE);
		processLoadComp.setLayout(new GridLayout(2, false));
		this.processorLoadBar = new ProgressBar(processLoadComp, SWT.NONE);
		this.processorLoadBar.setMaximum(100);
		this.processorLoadBar.setMinimum(0);
		this.processorLoadLabel = new Label(processLoadComp, SWT.LEFT | SWT.HORIZONTAL);
		this.processorLoadLabel.setLayoutData(new GridData(40, 20));
		
		Label executionTimeTitle = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		executionTimeTitle.setText(Translator.translate("circuit_viewer.process_group.execution_time.title"));
		
		this.executionTimeLabel = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		this.executionTimeLabel.setLayoutData(new GridData(100, 20));
		
		Label parentProcessTitle = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		parentProcessTitle.setText(Translator.translate("circuit_viewer.process_group.parent_process.title"));
		
		this.parentProcessLabel = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		this.parentProcessLabel.setLayoutData(new GridData(100, 20));
		
		this.openInEditorButton = new Button(processGroup, SWT.PUSH);
		this.openInEditorButton.setText(Translator.translate("circuit_viewer.process_group.open_in_editor"));
		this.openInEditorButton.addListener(SWT.Selection, (e) -> openInEditor());
		
		updateView();
		this.shell.pack();
		this.shell.open();
	}
	
	public Shell getShell() {
		return shell;
	}
	
	protected CircuitProcessInfo getSelectedProcess() {
		TreeItem[] selection = this.treeView.getSelection();
		if (selection.length >= 1) {
			if (selection[0].getData() instanceof CircuitProcessInfo process) return process;
		}
		return null;		
	}
	
	protected void terminateProcess() {
		CircuitProcessInfo process = getSelectedProcess();
		if (process != null) LogicSim.getInstance().getCircuitProcessor().removeProcess(process.circuit());
	}
	
	protected void openInEditor() {
		CircuitProcessInfo process = getSelectedProcess();
		if (process != null) LogicSim.getInstance().openEditor(process.circuit());
	}
	
	protected void updateProcessGroupInformations(SimulationMonitor monitor) {
		
		CircuitProcessInfo process = getSelectedProcess();
		double processLoad = monitor.getCPULoad();
		int executionTime = (int) (process != null ? process.executionTime().get() : 0);
		String parentProcess = process != null ? process.parentCircuit() != null ? process.parentCircuit().getCircuitFile() != null ? process.parentCircuit().getCircuitFile().getName() : Translator.translate("circuit_viewer.process_group.parent_process.no_name") : Translator.translate("circuit_viewer.process_group.parent_process.main_process") : Translator.translate("circuit_viewer.process_group.parent_process.not_available");
		
		this.processorLoadBar.setSelection((int) (processLoad * 100));
		boolean criticalLoad = processLoad > 0.7F;
		if (this.processorLoadBar.getState() == SWT.ERROR != criticalLoad) this.processorLoadBar.setState(criticalLoad ? SWT.ERROR : SWT.NORMAL);
		this.processorLoadLabel.setText(Translator.translate("circuit_viewer.process_group.process_load", (int) (processLoad * 100)));
		this.processorLoadLabel.pack();
		this.executionTimeLabel.setText(Translator.translate("circuit_viewer.process_group.execution_time", executionTime));
		this.executionTimeLabel.pack();
		this.parentProcessLabel.setText(Translator.translate("circuit_viewer.process_group.parent_process", parentProcess));
		this.parentProcessLabel.pack();
		
	}
	
	public void updateUI() {
		
		if (System.currentTimeMillis() - this.lastViewUpdate >= 1000) {
			updateView();
			lastViewUpdate = System.currentTimeMillis();
		}
		
	}
	
	protected void updateView() {
		
		SimulationMonitor monitor = LogicSim.getInstance().getSimulationMonitor();
		
		updateProcessGroupInformations(monitor);
		
		Optional<CircuitProcessInfo> mainProcess = monitor.getRunningProcesses().stream().filter(process -> process.parentCircuit() == null).findAny();
		
		if (mainProcess.isPresent()) {
			listSubProcesses(monitor, mainProcess.get());
			listUnknownProcesses(monitor);
		}
		
		List<Circuit> removed = new ArrayList<>();
		this.viewItems.entrySet().forEach((entry) -> {
			if (!entry.getValue().isDisposed()) {
				if (!((CircuitProcessInfo) entry.getValue().getData()).isActive()) {
					removed.add(entry.getKey());
				} else {
					updateCircuitDescription(monitor, entry.getValue());
				}
			}
		});
		removed.forEach(circuit -> {
			this.viewItems.get(circuit).dispose();
			this.viewItems.remove(circuit);
		});
		
	}
		
	protected void listSubProcesses(SimulationMonitor monitor, CircuitProcessInfo process) {
		
		if (!this.viewItems.containsKey(process.circuit())) {
			Optional<TreeItem> parent = this.viewItems.values().stream().filter(item ->  !item.isDisposed() ? ((CircuitProcessInfo) item.getData()).circuit() == process.parentCircuit() : false).findAny();
			TreeItem item = parent.isPresent() ? new TreeItem(parent.get(), SWT.NONE) : new TreeItem(this.treeView, SWT.NONE);
			item.setData(process);
			item.setText("N/A");
			this.viewItems.put(process.circuit(), item);
		}
		
		monitor.getRunningProcesses().stream().filter(process1 -> process1.parentCircuit() == process.circuit()).forEach(subProcess -> {
			listSubProcesses(monitor, subProcess);
		});
		
	}

	protected void listUnknownProcesses(SimulationMonitor monitor) {
		
		monitor.getRunningProcesses().forEach(process -> {
			if (!this.viewItems.containsKey(process.circuit())) {
				Optional<TreeItem> parent = this.viewItems.values().stream().filter(item ->  !item.isDisposed() ? ((CircuitProcess) item.getData()).circuit == process.parentCircuit() : false).findAny();
				TreeItem item = parent.isPresent() ? new TreeItem(parent.get(), SWT.NONE) : new TreeItem(this.treeView, SWT.NONE);
				item.setData(process);
				item.setText("N/A");
				this.viewItems.put(process.circuit(), item);
			}
		});
		
	}
	
	protected void updateCircuitDescription(SimulationMonitor monitor, TreeItem item) {
		CircuitProcessInfo process = (CircuitProcessInfo) item.getData();
		
		String name = process.circuit().getCircuitFile() != null ? process.circuit().getCircuitFile().getName() : "unknown";
		String executionTime = Long.toString(process.executionTime().get());
		
		boolean active = process.isActive();
		boolean running = process.isExecuting();
		boolean warnings = process.hasWarnings();
		
		String activity = Translator.translate("circuit_viewer.tree_view.circuit_description." + (active ? (running ? "active" : "inactive") : ".error"));
		
		item.setText(Translator.translate("circuit_viewer.tree_view.circuit_description", name, executionTime, activity));
		if (item.getImage() != null) item.getImage().dispose();
		item.setImage(decodeImage(warnings ? ERROR_ICON_B64 : (running ? RUNNING_ICON_B64 : SUSPENDED_ICON_B64)));
	}
	
}
