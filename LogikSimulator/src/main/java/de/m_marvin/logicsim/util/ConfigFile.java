package de.m_marvin.logicsim.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class ConfigFile {
	
	public static void setValue(File configFile, String field, String value) {
		if (configFile == null || !configFile.isFile()) {
			System.err.println("Config file '" + configFile + "' not found!");
			return;
		}
		try {
			List<String> configLines = new ArrayList<>();
			BufferedReader configReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
			String line;
			while ((line = configReader.readLine()) != null) configLines.add(line);
			configReader.close();
			BufferedWriter configWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile)));
			boolean configWritten = false;
			for (String nline : configLines) {
				String[] s = nline.split("=");
				if (s.length == 2) {
					if (s[0].equals(field)) {
						nline = field + "=" + value;
						configWritten = true;
					}
					configWriter.write(nline + "\r\n");
				}
			}
			if (!configWritten) configWriter.write(field + "=" + value + "\r\n");
			configWriter.close();
		} catch (IOException e) {
			System.err.println("Could not access config file!");
			e.printStackTrace();
		}
	}
	
	public static String getValue(File configFile, String field, String defaultValue) {
		if (configFile == null || !configFile.isFile()) {
			System.err.println("Config file '" + configFile + "' not found!");
			return defaultValue;
		}
		try {
			BufferedReader configReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
			String line;
			String value = defaultValue;
			while ((line = configReader.readLine()) != null) {
				String[] s = line.split("=");
				if (s.length == 2 && s[0].equals(field) && s.length == 2) value = s[1];
			}
			configReader.close();
			return value;
		} catch (IOException e) {
			System.err.println("Could not access config file!");
			e.printStackTrace();
			return defaultValue;
		}
	}
	
}
