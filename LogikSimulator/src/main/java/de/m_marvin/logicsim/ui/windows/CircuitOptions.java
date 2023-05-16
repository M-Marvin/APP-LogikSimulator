package de.m_marvin.logicsim.ui.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.ShortCircuitType;
import de.m_marvin.logicsim.ui.Translator;

public class CircuitOptions {
	
	protected Shell shell;
	protected Circuit circuit;
	
	protected Button shortCircuitModeButton;
	protected Button highPrioModeButton;
	protected Button lowPrioModeButton;
	
	public CircuitOptions(Display display, Circuit circuit) {
		this.shell = new Shell(display, SWT.SHELL_TRIM & (~SWT.RESIZE));
		this.shell.setImage(Editor.decodeImage(LogicSim.LOGIC_SIM_ICON));
		this.shell.setText(Translator.translate("circuit_options.title"));
		this.shell.setLayout(new GridLayout(1, true));
		
		this.circuit = circuit;
		
		Group simulationMode = new Group(shell, SWT.NONE);
		simulationMode.setText(Translator.translate("circuit_options.circuit_mode.title"));
		for (int i = 0; i < ShortCircuitType.values().length; i++) {
			ShortCircuitType mode = ShortCircuitType.values()[i];
			Button optButton = new Button(simulationMode, SWT.RADIO);
			optButton.setBounds(10, 15 + i * 20, 140, 20);
			optButton.setText(Translator.translate("circuit_options.circuit_mode." + mode.toString().toLowerCase()));
			optButton.addListener(SWT.Selection, (e) -> circuit.setShortCircuitMode(mode));
			if (circuit.getShortCircuitMode() == mode) optButton.setSelection(true);
		}
		simulationMode.pack();
		
		Label informationLabel = new Label(shell, SWT.NONE);
		informationLabel.setText(Translator.translate("circuit_options.description"));
		
		this.shell.pack();
		this.shell.open();
	}
	
}
