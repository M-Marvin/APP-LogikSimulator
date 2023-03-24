package de.m_marvin.logicsim.ui;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.m_marvin.logicsim.CircuitProcessor;
import de.m_marvin.logicsim.CircuitProcessor.CircuitProcess;
import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;

public class CircuitViewer {
	
	public static final String RUNNING_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABESURBVChTY2D4z/EfjHEARgxJxh+MUBYYYCqAAahC3AqgAKcC3vM/wDSGApjEZyOgHBDAFaBLwAHvOYb/IAzlogEGBgAn2R2n/6vpZQAAAABJRU5ErkJggg==";
	public static final String SUSPENDED_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAsSURBVChTYzxyjuE/AxBcucTAkJHAwAhiI4sxgRj4wNBQAPIKCCMDhBgDAwCAMw/f3Kx0cwAAAABJRU5ErkJggg==";
	public static final String ERROR_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAA2SURBVChTY0AG/4HoBphCACYoTRiAdMMwsinEmYCsG4ZhphA0gRGkGsrGADeBmBHdW6iAgQEAcBIcKuG5HsgAAAAASUVORK5CYII=";
	
	protected Shell shell;
	
	protected Map<Circuit, TreeItem> viewItems = new HashMap<>(); 
	protected Tree treeView;

	public static Image decodeImage(String imageString) {
		return new Image(LogicSim.getInstance().getDisplay(), new ImageData(new ByteArrayInputStream(Base64.getDecoder().decode(imageString))));
	}
	
	public CircuitViewer(Display display) {
		this.shell = new Shell(display);
		this.shell.setLayout(new BorderLayout());
		
		this.treeView = new Tree(shell, SWT.SINGLE);
		this.treeView.setLayoutData(new BorderData(SWT.CENTER));
		
		updateView();
		this.shell.open();
	}
	
	public Shell getShell() {
		return shell;
	}
	
	public void updateView() {
		
		CircuitProcessor processor = LogicSim.getInstance().getCircuitProcessor();
		
		synchronized (processor) {
			
			Optional<CircuitProcess> mainProcess = processor.getProcesses().stream().filter(process -> process.parentCircuit == null).findAny();
			
			if (mainProcess.isPresent()) listSubProcesses(processor.getProcesses(), mainProcess.get());
			
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
	
	protected void listSubProcesses(Collection<CircuitProcess> processes, CircuitProcess process) {
		
		if (!this.viewItems.containsKey(process.circuit)) {
			Optional<TreeItem> parent = this.viewItems.values().stream().filter(item ->  !item.isDisposed() ? ((CircuitProcess) item.getData()).circuit == process.parentCircuit : false).findAny();
			TreeItem item = parent.isPresent() ? new TreeItem(parent.get(), SWT.NONE) : new TreeItem(this.treeView, SWT.NONE);
			item.setData(process);
			item.setText("Unknown");
			this.viewItems.put(process.circuit, item);
		}
		
		processes.stream().filter(process1 -> process1.parentCircuit == process.circuit).forEach(subProcess -> {
			listSubProcesses(processes, subProcess);
		});
		
	}
	
}
