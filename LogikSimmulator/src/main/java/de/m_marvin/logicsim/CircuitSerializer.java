package de.m_marvin.logicsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.ShortCircuitType;
import de.m_marvin.logicsim.logic.Component;

public class CircuitSerializer {
	
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static void saveCircuit(Circuit circuit, File file) throws FileNotFoundException {
		serializeCircuit(circuit, new FileOutputStream(file));
	}
	
	public static Circuit loadCircuit(File file) throws FileNotFoundException {
		return deserializeCircuit(new FileInputStream(file));
	}
	
	public static void serializeCircuit(Circuit circuit, OutputStream stream) {
		JsonObject json = serializeCircuit(circuit);
		GSON.toJson(json, new OutputStreamWriter(stream));
	}
	
	public static Circuit deserializeCircuit(InputStream stream) {
		JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
		return deserializeCircuit(json);
	}
	
	public static Circuit deserializeCircuit(JsonObject json) {
		
		Circuit circuit = new Circuit();
		
		circuit.setShortCircuitMode(ShortCircuitType.valueOf(json.get("shortCircuitType").getAsString()));
		
		JsonArray components = json.get("components").getAsJsonArray();
		
		components.forEach(componentJson -> {
			try {
				String componentClassName = componentJson.getAsJsonObject().get("id").getAsString();
				@SuppressWarnings("unchecked")
				Class<? extends Component> componentClass = (Class<? extends Component>) Class.forName(componentClassName);
				Component component = componentClass.getConstructor(Circuit.class).newInstance(circuit);
				component.deserialize(componentJson.getAsJsonObject());
				circuit.add(component);
			} catch (Exception e) {
				System.err.println("Failed to load component '" + componentJson.toString() + "'");
			}
		});
		
		return circuit;
		
	}
	
	public static JsonObject serializeCircuit(Circuit circuit) {
		
		JsonObject json = new JsonObject();
		
		json.addProperty("shortCircuitType", circuit.getShortCircuitMode().toString());
		
		JsonArray components = new JsonArray();
		
		circuit.getComponents().forEach(component -> {
			JsonObject componentJson = new JsonObject();
			componentJson.addProperty("id", component.getClass().getName());
			component.serialize(componentJson);
			components.add(componentJson);
		});
		
		json.add("components", components);
		
		return json;
		
	}
	
}
