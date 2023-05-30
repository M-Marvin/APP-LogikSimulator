package de.m_marvin.logicsim.logic.parts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.m_marvin.logicsim.LogicSim;
import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.logicsim.ui.widgets.InputDialog;
import de.m_marvin.logicsim.ui.windows.Editor;
import de.m_marvin.univec.impl.Vec2i;

public class MemoryComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAGkSURBVFhHxZeBcsIgEESl1tb//9rO2BZ5xM1cLkiAZPTN7EDAwHIXIgkxxmsI4ec08ZkUp2rG1qHWd0pjfaXiY7pqwxv4TwpTtZtW4+ekS9IvF97tnykRhqwYSPJgXGJcKyaVlhCBRxVuSaXBj0LmSXVGEaBB7j26qUWtzL+VAVZOiNch6qPHREYGlPsaNsdeli4TMuAH2cuWiVUK2BJ5W+zELqQpEjLwncTe5DnYS5cJGTgKJvSTVk14A0c/C8+YTWHgqLCX1I1et5ZSWFvx967GelcKePGh1YRyZ9tLba34exerT4TRXWAH8oP66yojBvzkrE5tXZODNfAsTJ5aKlrSxPj8+/LiG4rAEfDnVzwR9aLwt6y8yEgKwE7oJ98ys+iXAXIyH5NeDSZYteQPJ2ofwd+7GutdD+EMBnzORlfbTTqRnxUBJt06ESl8PdriggFyTplfDK/GPgPaBXLOEZ1+lRIp6xWoXERajYCB/Bc5iA25D79dFAZytPN3KZUHMmC3oe0He+37WsFMjnw24L4NLbf01dzywSJq5nxfTkOM8XoHoJyURAmJ8Q8AAAAASUVORK5CYII=";
	
	/* Factory methods */
	
	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new MemoryComponent(circuit));
	}
	
	/* End of factory methods */
	
	public static final int ADDRESS_BUS_WIDTH = 64;
	
	protected File lastDataFile;
	protected int dataRowCount;
	
	protected boolean isVolatile;
	protected int minDataRowCount;
	protected int dataRowWidth;
	protected byte[] data;
	
	public MemoryComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "memory";
		this.inputs.add(new InputNode(this, 0, "adrress", new Vec2i(-10, 20)));
		this.inputs.add(new InputNode(this, 1, "data_in", new Vec2i(-10, 40)));
		this.inputs.add(new InputNode(this, 2, "write", new Vec2i(-10, 80)));
		this.inputs.add(new InputNode(this, 3, "read", new Vec2i(-10, 100)));
		this.outputs.add(new OutputNode(this, 4, "data_out", new Vec2i(90, 40)));
		
		this.data = new byte[32];
		this.dataRowWidth = 1;
		this.dataRowCount = this.data.length / this.dataRowWidth;
	}
	
	@Override
	public int getVisualWidth() {
		return 80;
	}

	@Override
	public int getVisualHeight() {
		return 120;
	}
	
	public int getDataRowWidth() {
		return this.dataRowWidth;
	}
	
	public int getDataRowCount() {
		return this.dataRowCount;
	}
	
	public void writeData(int address, int data) {
		if (address >= this.dataRowCount) return;
		int dataIndex = address * this.dataRowWidth;
		for (int i = 0; i < this.dataRowWidth; i++) {
			this.data[dataIndex + i] = (byte) (data >> i * 8);
		}
	}
	
	public int readData(int address) {
		if (address >= this.dataRowCount) return 0;
		int dataIndex = address * this.dataRowWidth;
		int value = 0;
		for (int i = 0; i < this.dataRowWidth; i++) {
			value |= this.data[dataIndex + i] << i * 8;
		}
		return value;
	}
	
	public void loadDataFromFile(boolean changeFile) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (changeFile || this.lastDataFile == null || !this.lastDataFile.isFile()) {
			FileDialog fileDialog = new FileDialog(editor.getShell(), SWT.OPEN);
			fileDialog.setFilterExtensions(new String[] {"*.*"});
			String path = fileDialog.open();
			if (path != null) {
				File file = new File(path);
				if (file.isFile()) {
					this.lastDataFile = file;
				} else {
					return;
				}
			} else {
				return;
			}
		}
		try {
			InputStream is = new FileInputStream(lastDataFile);
			int i;
			int p = 0;
			while ((i = is.read()) != -1) {
				this.data[p++] = (byte) i ;
			}
			is.close();
		} catch (IOException e) {
			System.err.println("Failed to load data from file!");
			e.printStackTrace();
			Editor.showErrorInfo(editor.getShell(), "editor.window.error.load_memory", e);
		}
	}
	
	public void saveDataToFile(boolean changeFile) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (changeFile || this.lastDataFile == null) {
			FileDialog fileDialog = new FileDialog(editor.getShell(), SWT.SAVE);
			fileDialog.setFilterExtensions(new String[] {"*.*"});
			String path = fileDialog.open();
			if (path != null) {
				File file = new File(path);
				this.lastDataFile = file;
			} else {
				return;
			}
		}
		try {
			OutputStream os = new FileOutputStream(lastDataFile);
			for (int i = 0; i < this.data.length; i++) {
				os.write(this.data[i]);
			}
			os.close();
		} catch (IOException e) {
			System.err.println("Failed to save data from file!");
			e.printStackTrace();
			Editor.showErrorInfo(editor.getShell(), "editor.window.error.save_memory", e);
		}
	}
	
	public void verifyMemory() {
		this.dataRowCount = this.data.length / this.dataRowWidth;
		if (this.dataRowCount < this.minDataRowCount) {
			byte[] newDataArr = new byte[this.minDataRowCount * this.dataRowWidth];
			for (int i = 0; i < this.data.length; i++) newDataArr[i] = this.data[i];
			this.data = newDataArr;
			this.dataRowCount = this.minDataRowCount;
		}
	}
	
	public void clearMemory() {
		for (int i = 0; i < this.data.length; i++) this.data[i] = 0;
	}
	
	@Override
	public void reset() {
		super.reset();
		if (this.isVolatile) clearMemory();
	}
	
	@Override
	public void serialize(JsonObject json) {
		super.serialize(json);
		json.addProperty("isVolatile", this.isVolatile);
		json.addProperty("minDataRows", this.minDataRowCount);
		json.addProperty("dataRowWidth", this.dataRowWidth);
		JsonArray arr = new JsonArray(this.data.length);
		for (int i = 0; i < this.data.length; i++) arr.add(this.data[i]);
		json.add("data", arr);
	}
	
	@Override
	public void deserialize(JsonObject json) {
		super.deserialize(json);
		this.isVolatile = json.get("isVolatile").getAsBoolean();
		this.minDataRowCount = json.get("minDataRowCount").getAsInt();
		this.dataRowWidth = json.get("dataRowWidth").getAsInt();
		JsonArray arr = json.get("data").getAsJsonArray();
		this.dataRowCount = arr.size() / this.dataRowWidth;
		this.data = new byte[arr.size()];
		for (int i = 0; i < arr.size(); i++) this.data[i] = arr.get(i).getAsByte();
		verifyMemory();
	}
	
	@Override
	public void click(Vec2i clickPosition, boolean leftClick) {
		Editor editor = LogicSim.getInstance().getLastInteractedEditor();
		if (leftClick) {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.StringConfigField("editor.config.change_component_name", getLabel(), this::setLabel));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.memory.row_width", this.dataRowWidth, 1, 64, i -> { this.dataRowWidth = Math.max(Math.min(i, 8), 1); verifyMemory(); }));
			configDialog.addConfig(new InputDialog.NumberConfigField("editor.config.memory.min_rows", this.minDataRowCount, 1, Integer.MAX_VALUE, i -> { this.minDataRowCount = i; verifyMemory(); }));
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		} else {
			InputDialog configDialog = new InputDialog(editor.getShell());
			configDialog.addConfig(new InputDialog.ButtonConfigField("editor.config.memory.load_data.description").addButton("editor.config.memory.load_data.last_file", () -> loadDataFromFile(false)).addButton("editor.config.memory.load_data.new_file", () -> loadDataFromFile(true)));
			configDialog.addConfig(new InputDialog.ButtonConfigField("editor.config.memory.save_data.description").addButton("editor.config.memory.save_data.last_file", () -> saveDataToFile(false)).addButton("editor.config.memory.save_data.new_file", () -> saveDataToFile(true)));
			configDialog.addConfig(new InputDialog.ButtonConfigField("editor.config.memory.clear.description", "editor.config.memory.clear", () -> clearMemory()));
			configDialog.open();
			configDialog.setLocation(clickPosition.x, clickPosition.y);
		}
	}
	
	@Override
	public void render() {
		
		EditorArea.swapColor(1, 0, 1, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(0, 0, 0, 0.4F);
		EditorArea.drawFilledRectangle(visualPosition.x + 10, visualPosition.y + 20, 60, 20);
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + 12, 12, "Data Rows");
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + 30, 12, Integer.toString(getDataRowCount()));
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, this.label);
		
	}

	@Override
	public void updateIO() {
		
		int address = this.inputs.get(0).readBusValue(ADDRESS_BUS_WIDTH);
		
		if (this.inputs.get(2).getState()) {
			int dataIn = this.inputs.get(1).readBusValue(getDataRowWidth() * 8);
			writeData(address, dataIn);
		}
		if (this.inputs.get(3).getState()) {
			int dataOut = readData(address);
			this.outputs.get(0).writeBusValue(dataOut, getDataRowWidth() * 8);
		} else {
			this.outputs.get(0).writeBusValue(0, getDataRowWidth() * 8);
		}
		
	}

}
