package de.m_marvin.logicsim.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.logicsim.logic.Circuit;

public class Registries {
	
	/* Folder Registry */
	
	public static record ComponentFolder(String name, String icon) {}
	protected static List<ComponentFolder> partFolders = new ArrayList<>();

	public static ComponentFolder registerFolder(String name, String icon) {
		ComponentFolder folder = new ComponentFolder(name, icon);
		partFolders.add(folder);
		return folder;
	}
	
	public static ComponentFolder getFolderEntry(String name) {
		for (ComponentFolder entry : partFolders) {
			if (entry.name == name) return entry;
		}
		throw new IllegalArgumentException("There is no folder registered with name '" + name + "'");
	}
	
	public static List<ComponentFolder> getRegisteredFolderList() {
		return partFolders;
	}
	
	/* Component Registry */
	
	public static record ComponentEntry(ComponentFolder folder, Class<?> componentClass, BiConsumer<Circuit, Vec2i> placementClickMethod, BiFunction<Circuit, Vec2i, Boolean> placementMoveMethod, Consumer<Circuit> placementAbbortMethod, String name, String icon) {}
	protected static List<ComponentEntry> logicParts = new ArrayList<>();
	
	public static void registerPart(ComponentFolder folder, Class<?> component, BiConsumer<Circuit, Vec2i> placementClickMethod, BiFunction<Circuit, Vec2i, Boolean> placementMoveMethod, Consumer<Circuit> placementAbbortMethod, String name, String icon) {
		logicParts.add(new ComponentEntry(folder, component, placementClickMethod, placementMoveMethod, placementAbbortMethod, name, icon));
	}
	
	public static ComponentEntry getPartEntry(Class<?> connectorClass) {
		for (ComponentEntry entry : logicParts) {
			if (entry.componentClass == connectorClass) return entry;
		}
		throw new IllegalArgumentException("There is no net-connector registered with class '" + connectorClass.getName() + "'");
	}
	
	public static List<ComponentEntry> getRegisteredPartsList() {
		return logicParts;
	}
	
	/* Language File Registry */
	
	protected static List<String> langFolders = new ArrayList<>();
	
	public static void registerLangFolder(String langFolder) {
		langFolders.add(langFolder);
	}
	
	public static List<String> getLangFolders() {
		return langFolders;
	}
	
}
