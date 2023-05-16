package de.m_marvin.logicsim.ui;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Translator {
	
	public static final Gson GSON = new Gson();
	
	private static HashMap<String, String> languages = new HashMap<>();
	private static HashMap<String, String> translations = new HashMap<>();
	private static List<String> langFolders = new ArrayList<>();
	
	public static void addLangFolder(String langFolder) {
		langFolders.add(langFolder);
	}
	
	public static List<String> getLangFolders() {
		return langFolders;
	}
	
	public static void addLanguage(String lang, String name) {
		languages.put(lang, name);
	}

	public static Set<String> getAvailableLanguages() {
		return languages.keySet();		
	}

	public static String resolveLangName(String lang) {
		return languages.getOrDefault(lang, "unknown");
	}
	
	public static void changeLanguage(String language) {
		translations.clear();
		getLangFolders().forEach(folder -> {
			String file = folder + "/" + language + ".json";
			InputStream stream = Translator.class.getResourceAsStream(file);
			if (stream == null) {
				System.err.println("Language file '" + file + "' not found!");
			} else {
				JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
				json.entrySet().forEach(entry -> translations.put(entry.getKey(), entry.getValue().getAsString()));
			}
		});
		System.out.println("Changed language to '" + language + "'");
	}
	
	public static String translate(String key, Object... args) {
		String translationPattern = translations.get(key);
		if (translationPattern == null) return key;
		for (int i = 0; i < args.length; i++) {
			translationPattern = translationPattern.replace("%" + i, args[i].toString());
		}
		return translationPattern;
	}
	
}
