package de.m_marvin.logicsim.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
	
	public static void saveCircuit(Circuit circuit, File file) throws IOException {
		if (!file.equals(circuit.getCircuitFile())) circuit.setCircuitFile(file);
		serializeCircuit(circuit, new FileOutputStream(file));
	}
	
	public static Circuit loadCircuit(File file) throws IOException {
		Circuit circuit = new Circuit();
		circuit.setCircuitFile(file);
		deserializeCircuit(circuit, new FileInputStream(file));
		return circuit;
	}
	
	public static void serializeCircuit(Circuit circuit, OutputStream stream) throws IOException {
		JsonObject json = serializeCircuit(circuit);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
		writer.write(GSON.toJson(json));
		writer.close();
	}
	
	public static void deserializeCircuit(Circuit circuit, InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = reader.readLine()) != null) sb.append(line);
		reader.close();
		
		JsonObject json = GSON.fromJson(sb.toString(), JsonObject.class);
		deserializeCircuit(circuit, json);
	}
	
	public static void deserializeCircuit(Circuit circuit, JsonObject json) {
		
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
				e.printStackTrace();
			}
		});
		
		circuit.getComponents().forEach(component -> circuit.reconnect(false, component));
		
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
