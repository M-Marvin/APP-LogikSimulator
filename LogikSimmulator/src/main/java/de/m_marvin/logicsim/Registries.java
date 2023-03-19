package de.m_marvin.logicsim;

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
	protected List<ComponentFolder> partFolders = new ArrayList<>();

	public ComponentFolder registerFolder(String name, String icon) {
		ComponentFolder folder = new ComponentFolder(name, icon);
		this.partFolders.add(folder);
		return folder;
	}
	
	public ComponentFolder getFolderEntry(String name) {
		for (ComponentFolder entry : this.partFolders) {
			if (entry.name == name) return entry;
		}
		throw new IllegalArgumentException("There is no folder registered with name '" + name + "'");
	}
	
	public List<ComponentFolder> getRegisteredFolderList() {
		return this.partFolders;
	}
	
	/* Component Registry */
	
	public static record ComponentEntry(ComponentFolder folder, Class<?> componentClass, BiConsumer<Circuit, Vec2i> placementClickMethod, BiFunction<Circuit, Vec2i, Boolean> placementMoveMethod, Consumer<Circuit> placementAbbortMethod, String name, String icon) {}
	protected List<ComponentEntry> logicParts = new ArrayList<>();
	
	public void registerPart(ComponentFolder folder, Class<?> component, BiConsumer<Circuit, Vec2i> placementClickMethod, BiFunction<Circuit, Vec2i, Boolean> placementMoveMethod, Consumer<Circuit> placementAbbortMethod, String name, String icon) {
		this.logicParts.add(new ComponentEntry(folder, component, placementClickMethod, placementMoveMethod, placementAbbortMethod, name, icon));
	}
	
	public ComponentEntry getPartEntry(Class<?> connectorClass) {
		for (ComponentEntry entry : this.logicParts) {
			if (entry.componentClass == connectorClass) return entry;
		}
		throw new IllegalArgumentException("There is no net-connector registered with class '" + connectorClass.getName() + "'");
	}
	
	public List<ComponentEntry> getRegisteredPartsList() {
		return this.logicParts;
	}
	
}
