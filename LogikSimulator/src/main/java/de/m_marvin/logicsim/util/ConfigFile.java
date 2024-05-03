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

import de.m_marvin.simplelogging.printing.LogType;
import de.m_marvin.simplelogging.printing.Logger;

public class ConfigFile {

	protected static final String LOG_LEVEL = "config";
	
	public static void setValue(File configFile, String field, String value) {
		if (configFile == null || !configFile.isFile()) {
			Logger.defaultLogger().logError(LOG_LEVEL,"Config file '" + configFile + "' not found!");
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
			Logger.defaultLogger().logError(LOG_LEVEL,"Could not access config file!");
			Logger.defaultLogger().printException(LogType.ERROR, e);
		}
	}
	
	public static String getValue(File configFile, String field, String defaultValue) {
		if (configFile == null || !configFile.isFile()) {
			Logger.defaultLogger().logError(LOG_LEVEL,"Config file '" + configFile + "' not found!");
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
			Logger.defaultLogger().logError(LOG_LEVEL,"Could not access config file!");
			Logger.defaultLogger().printException(LogType.ERROR, e);
			return defaultValue;
		}
	}
	
}
