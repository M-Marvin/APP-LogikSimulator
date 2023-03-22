package de.m_marvin.commandlineparser;

import java.util.HashMap;
import java.util.Map;

public class CommandLineParser {
	
	protected Map<String, Object> options = new HashMap<String, Object>();
	
	public void addOption(String name, Object defaultValue) {
		options.put(name, defaultValue);
	}
	
	public String getOption(String name) {
		return (String) options.get(name);
	}
	
	public boolean getFlag(String name) {
		return (boolean) options.get(name);
	}
	
	public void parseInput(String[] args) {
		
		String[] currentOption = null;
		StringBuilder value = null;
		for (int i = 0; i < args.length; i++) {
			
			String argument = args[i];
			String[] optionName = parseOptionName(argument);
			
			if (optionName != null) {
				if (currentOption != null) {
					if (value.isEmpty()) {
						options.put(currentOption[0], true);
					} else {
						options.put(currentOption[0], value.toString());
					}
				}
				currentOption = optionName;
				value = new StringBuilder();
				if (currentOption.length > 1) value.append(currentOption[1]);
				continue;
			} else {
				value.append((value.isEmpty() ? "" : " ") + argument);
			}
			
		}
		
		if (currentOption != null) {
			if (value.isEmpty()) {
				options.put(currentOption[0], true);
			} else {
				options.put(currentOption[0], value.toString());
			}
		}
		
	}
	
	public String[] parseOptionName(String s) {
		String option = null;
		if (s.startsWith("--")) {
			option = s.substring(2);
		} else if (s.startsWith("-")) {
			option = s.substring(1);
		}
		if (option == null) {
			return null;
		} else {
			return option.split("=");
		}
	}
	
}
