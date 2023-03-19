package de.m_marvin.logicsim.ui;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.m_marvin.logicsim.Registries;

public class Translator {
	
	public static final Gson GSON = new Gson();
	
	public static HashMap<String, String> translations = new HashMap<>();
	
	public static void changeLanguage(String language) {
		translations.clear();
		Registries.getLangFolders().forEach(folder -> {
			String file = folder + "/" + language + ".json";
			InputStream stream = Translator.class.getResourceAsStream(file);
			if (stream == null) {
				System.err.println("Language file '" + file + "' not found!");
			}
			JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
			json.entrySet().forEach(entry -> translations.put(entry.getKey(), entry.getValue().getAsString()));
		});
	}
	
	public static String translate(String key) {
		String translationPattern = translations.get(key);
		if (translationPattern == null) return key;
		return translationPattern;
	}
	
}
