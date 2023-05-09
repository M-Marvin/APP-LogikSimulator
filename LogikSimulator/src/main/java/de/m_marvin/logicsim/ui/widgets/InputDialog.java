package de.m_marvin.logicsim.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import de.m_marvin.logicsim.ui.Translator;

public class InputDialog {
	
	public static final int WIDTH = 300;
	
	public abstract static class ConfigField {
		public String description;
		
		public ConfigField(String description) {
			this.description = description;
		}
		
		public void setupWidget(Composite composite) {
			RowLayout layout = new RowLayout(SWT.VERTICAL);
			layout.fill = true;
			composite.setLayout(layout);
			Label descLabel = new Label(composite, SWT.NONE);
			descLabel.setText(Translator.translate(description));
		}
		
		public abstract void applyValue();
	}
	
	public static class NumberConfigField extends ConfigField {
		public int defaultValue;
		public int max;
		public int min;
		public IntConsumer configReceiver;
		public Spinner numberField;
		
		public NumberConfigField(String description, int defaultValue, int min, int max, IntConsumer configReceiver) {
			super(description);
			this.defaultValue = defaultValue;
			this.max = max;
			this.min = min;
			this.configReceiver = configReceiver;
		}
		
		@Override
		public void setupWidget(Composite composite) {
			super.setupWidget(composite);
			this.numberField = new Spinner(composite, SWT.BORDER);
			this.numberField.setMaximum(max);
			this.numberField.setMinimum(min);
			this.numberField.setSelection(defaultValue);
		}

		@Override
		public void applyValue() {
			this.configReceiver.accept(this.numberField.getSelection());
		}
	}
	
	public static class StringConfigField extends ConfigField {
		public String defaultValue;
		public Consumer<String> configReceiver;
		public Text textField;
		
		public StringConfigField(String description, String defaultValue, Consumer<String> configReceiver) {
			super(description);
			this.defaultValue = defaultValue;
			this.configReceiver = configReceiver;
		}
		
		@Override
		public void setupWidget(Composite composite) {
			super.setupWidget(composite);
			this.textField = new Text(composite, SWT.BORDER);
			this.textField.setText(defaultValue);
		}

		@Override
		public void applyValue() {
			this.configReceiver.accept(this.textField.getText());
		}
	}
	
	protected Shell shell;
	
	protected Button confirmButton;
	protected Button abbortButton;
	protected List<ConfigField> configs = new ArrayList<>();
	
	public InputDialog(Shell shell, String description, String defaultString, Consumer<String> configReceiver) {
		this(shell);
		addConfig(new StringConfigField(description, defaultString, configReceiver));
	}

	public InputDialog(Shell shell, String description, int defaultValue, int min, int max, IntConsumer configReceiver) {
		this(shell);
		addConfig(new NumberConfigField(description, defaultValue, min, max, configReceiver));
	}
	
	public InputDialog(Shell shell) {
		this.shell = new Shell(shell);
		this.shell.setText(Translator.translate("config_dialog.title"));
	}
	
	public Shell getShell() {
		return shell;
	}

	public void setLocation(int x, int y) {
		this.shell.setLocation(x, y);
	}
	
	public void addConfig(ConfigField config) {
		this.configs.add(config);
	}
	
	public void open() {
		
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.fill = true;
		this.shell.setLayout(layout);
		
		this.configs.forEach(config -> {
			Composite composite = new Composite(shell, 0);
			composite.setLayoutData(new RowData());
			config.setupWidget(composite);						
		});
		
		Composite buttonComp = new Composite(shell, 0);
		buttonComp.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		this.confirmButton = new Button(buttonComp, SWT.PUSH);
		this.confirmButton.setLayoutData(new RowData(100, SWT.DEFAULT));
		this.confirmButton.setText(Translator.translate("config_dialog.button.confirm"));
		this.confirmButton.addListener(SWT.Selection, e -> confirm());
		
		this.abbortButton = new Button(buttonComp, SWT.PUSH);
		this.abbortButton.setLayoutData(new RowData(100, SWT.DEFAULT));
		this.abbortButton.setText(Translator.translate("config_dialog.button.abbort"));
		this.abbortButton.addListener(SWT.Selection, e -> this.shell.close());
		
		this.shell.pack();
		this.shell.open();
		
	}
	
	public void confirm() {
		this.configs.forEach(ConfigField::applyValue);
		this.shell.close();
	}
	
}
