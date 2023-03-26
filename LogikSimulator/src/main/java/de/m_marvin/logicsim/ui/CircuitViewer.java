package de.m_marvin.logicsim.ui;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
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

import de.m_marvin.logicsim.CircuitProcessor;
import de.m_marvin.logicsim.CircuitProcessor.CircuitProcess;
import de.m_marvin.logicsim.CircuitProcessor.CircuitProcessorThread;
import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;

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
	
	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public CircuitViewer(Display display) {
		this.shell = new Shell(display);
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
		
		Label executionTimeTitle = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		executionTimeTitle.setText(Translator.translate("circuit_viewer.process_group.execution_time.title"));
		
		this.executionTimeLabel = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		
		Label parentProcessTitle = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		parentProcessTitle.setText(Translator.translate("circuit_viewer.process_group.parent_process.title"));
		
		this.parentProcessLabel = new Label(processGroup, SWT.LEFT | SWT.HORIZONTAL);
		
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
	
	public CircuitProcess getSelectedProcess() {
		TreeItem[] selection = this.treeView.getSelection();
		if (selection.length >= 1) {
			if (selection[0].getData() instanceof CircuitProcess process) return process;
		}
		return null;		
	}
	
	protected void terminateProcess() {
		CircuitProcess process = getSelectedProcess();
		if (process != null) LogicSim.getInstance().getCircuitProcessor().removeProcess(process.circuit);
	}
	
	protected void openInEditor() {
		CircuitProcess process = getSelectedProcess();
		if (process != null) LogicSim.getInstance().openEditor(process.circuit);
	}
	
	protected void updateProcessGroupInformations() {
		
		CircuitProcess process = getSelectedProcess();
		CircuitProcessorThread thread = LogicSim.getInstance().getCircuitProcessor().getProcessorThreadOf(process);
		float processLoad = (process != null && thread != null) ? process.executionTime / Math.max(thread.lastExecutionTime, 1) : 0;
		int executionTime = (int) (process != null ? process.executionTime : 0);
		String parentProcess = process != null ? process.parentCircuit != null ? process.parentCircuit.getCircuitFile() != null ? process.parentCircuit.getCircuitFile().getName() : Translator.translate("circuit_viewer.process_group.parent_process.no_name") : Translator.translate("circuit_viewer.process_group.parent_process.main_process") : Translator.translate("circuit_viewer.process_group.parent_process.not_available");
		
		this.processorLoadBar.setSelection((int) (processLoad * 100));
		this.processorLoadBar.setState(processLoad > 0.8F ? SWT.ERROR : SWT.NORMAL);
		this.processorLoadLabel.setText(Translator.translate("circuit_viewer.process_group.process_load", (int) (processLoad * 100)));
		this.processorLoadLabel.pack();
		this.executionTimeLabel.setText(Translator.translate("circuit_viewer.process_group.execution_time", executionTime));
		this.executionTimeLabel.pack();
		this.parentProcessLabel.setText(Translator.translate("circuit_viewer.process_group.parent_process", parentProcess));
		this.parentProcessLabel.pack();
		
	}
	
	public void updateView() {
		
		updateProcessGroupInformations();
		
		CircuitProcessor processor = LogicSim.getInstance().getCircuitProcessor();
		
		synchronized (processor) {
			
			Optional<CircuitProcess> mainProcess = processor.getProcesses().stream().filter(process -> process.parentCircuit == null).findAny();
			
			if (mainProcess.isPresent()) listSubProcesses(processor.getProcesses(), mainProcess.get());
			listUnknownProcesses(processor.getProcesses());
			
			List<Circuit> removed = new ArrayList<>();
			this.viewItems.entrySet().forEach((entry) -> {
				if (!entry.getValue().isDisposed()) {
					if (!processor.holdsCircuit(((CircuitProcess) entry.getValue().getData()).circuit)) {
						removed.add(entry.getKey());
					} else {
						updateCircuitDescription(processor, entry.getValue());
					}
				}
			});
			removed.forEach(circuit -> {
				this.viewItems.get(circuit).dispose();
				this.viewItems.remove(circuit);
			});
		}
		
	}

	protected void listSubProcesses(Collection<CircuitProcess> processes, CircuitProcess process) {
		
		if (!this.viewItems.containsKey(process.circuit)) {
			Optional<TreeItem> parent = this.viewItems.values().stream().filter(item ->  !item.isDisposed() ? ((CircuitProcess) item.getData()).circuit == process.parentCircuit : false).findAny();
			TreeItem item = parent.isPresent() ? new TreeItem(parent.get(), SWT.NONE) : new TreeItem(this.treeView, SWT.NONE);
			item.setData(process);
			item.setText("N/A");
			this.viewItems.put(process.circuit, item);
		}
		
		processes.stream().filter(process1 -> process1.parentCircuit == process.circuit).forEach(subProcess -> {
			listSubProcesses(processes, subProcess);
		});
		
	}
	
	protected void listUnknownProcesses(Collection<CircuitProcess> processes) {
		
		processes.forEach(process -> {
			if (!this.viewItems.containsKey(process.circuit)) {
				Optional<TreeItem> parent = this.viewItems.values().stream().filter(item ->  !item.isDisposed() ? ((CircuitProcess) item.getData()).circuit == process.parentCircuit : false).findAny();
				TreeItem item = parent.isPresent() ? new TreeItem(parent.get(), SWT.NONE) : new TreeItem(this.treeView, SWT.NONE);
				item.setData(process);
				item.setText("Unknown");
				this.viewItems.put(process.circuit, item);
			}
		});
		
	}
	
	protected void updateCircuitDescription(CircuitProcessor processor, TreeItem item) {
		CircuitProcess process = (CircuitProcess) item.getData();
		
		String name = process.circuit.getCircuitFile() != null ? process.circuit.getCircuitFile().getName() : "unknown";
		String executionTime = Long.toString(process.executionTime);
		
		boolean active = processor.holdsCircuit(process.circuit);
		boolean running = processor.isExecuting(process.circuit);
		
		String activity = Translator.translate("circuit_viewer.tree_view.circuit_description." + (active ? (running ? "active" : "inactive") : ".error"));
		
		item.setText(Translator.translate("circuit_viewer.tree_view.circuit_description", name, executionTime, activity));
		if (item.getImage() != null) item.getImage().dispose();
		item.setImage(decodeImage(active ? (running ? RUNNING_ICON_B64 : SUSPENDED_ICON_B64) : ERROR_ICON_B64));
	}
	
}
