package de.m_marvin.logicsim.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.univec.impl.Vec4i;

public class Circuit {
	
	public static Random floatingValue = new Random();
	public static boolean shortCircuitValue = false;
	
	public synchronized static boolean getFloatingValue() {
		return floatingValue.nextBoolean();
	}
	
	public synchronized static boolean getShortCircuitValue() {
		shortCircuitValue = !shortCircuitValue;
		return shortCircuitValue;
	}
	
	public static NetState combineStates(NetState stateA, NetState stateB, ShortCircuitType type) {
		if (stateA == NetState.FLOATING) return stateB;
		if (stateB == NetState.FLOATING) return stateA;
		switch (type) {
		default:
		case HIGH_LOW_SHORT:
			if (stateA == NetState.SHORT_CIRCUIT) {
				return NetState.SHORT_CIRCUIT;
			}
			if (stateB == NetState.SHORT_CIRCUIT) {
				return NetState.SHORT_CIRCUIT;
			}
			if (stateA != stateB) {
				return NetState.SHORT_CIRCUIT;
			}
			return stateA;
		case PREFER_HIGH:
			if (stateA == NetState.SHORT_CIRCUIT) return NetState.HIGH;
			if (stateB == NetState.SHORT_CIRCUIT) return NetState.HIGH;
			if (stateA != stateB) {
				return NetState.SHORT_CIRCUIT;
			}
			return stateA == NetState.HIGH || stateB == NetState.HIGH ? NetState.HIGH : NetState.LOW;
		case PREFER_LOW:
			if (stateA == NetState.SHORT_CIRCUIT) return NetState.LOW;
			if (stateB == NetState.SHORT_CIRCUIT) return NetState.LOW;
			if (stateA != stateB) {
				return NetState.SHORT_CIRCUIT;
			}
			return stateA == NetState.LOW || stateB == NetState.LOW ? NetState.LOW : NetState.HIGH;
		}
	}
	
	public static enum ShortCircuitType {
		HIGH_LOW_SHORT,PREFER_HIGH,PREFER_LOW;
	}
	
	public static enum NetState {
		LOW(() -> false),HIGH(() -> true),FLOATING(Circuit::getFloatingValue),SHORT_CIRCUIT(Circuit::getShortCircuitValue);
		
		private final Supplier<Boolean> logicValue;
		
		NetState(Supplier<Boolean> logicValueSupplier) {
			this.logicValue = logicValueSupplier;
		}
		
		public boolean getLogicState() {
			return this.logicValue.get();
		}
	}
	
	protected final List<Set<Node>> networks = new ArrayList<>();
	protected final List<NetState> valueBuffer = new ArrayList<>();
	protected final List<NetState> values = new ArrayList<>();
	protected final List<Component> components = new ArrayList<>();
	protected File circuitFile;
	protected final boolean virtual;
	protected ShortCircuitType shortCircuitType = ShortCircuitType.HIGH_LOW_SHORT;
	
	public Circuit() {
		this(false);
	}

	public Circuit(boolean virtual) {
		this.virtual = virtual;
	}
	
	public File getCircuitFile() {
		return this.circuitFile;
	}
	
	public synchronized void setCircuitFile(File file) {
		this.circuitFile = file;
	}
	
	public ShortCircuitType getShortCircuitMode() {
		return this.shortCircuitType;
	}
	
	public synchronized void setShortCircuitMode(ShortCircuitType shortCircuitType) {
		this.shortCircuitType = shortCircuitType;
	}

	public boolean isVirtual() {
		return this.virtual;
	}
	
	
	
	
	protected synchronized void reconnectNet(List<Node> nodes, boolean excludeNodes) {
		List<Node> nodesToReconnect = new ArrayList<>();
		nodes.forEach(node -> {
			OptionalInt netId = findNet(node);
			if (netId.isPresent()) {
				nodesToReconnect.addAll(removeNet(netId.getAsInt()));
			} else {
				nodesToReconnect.add(node);
			}
		});
		List<Node> excluded = excludeNodes ? nodes : null;
		Stream.of(nodesToReconnect.toArray(l -> new Node[l])).mapToInt(node -> groupNodeToNet(node, excluded)).forEach(this::combineNets);
	}
	
	protected synchronized int groupNodeToNet(Node node, List<Node> excluded) {
		List<Node> nodeCache = new ArrayList<Node>();
		Set<Node> network = new HashSet<>();
		this.components.forEach(component -> {
			component.getAllNodes().forEach(node2 -> {
				if (excluded != null) for (Node n : excluded) if (n == node2) return;
				if (node2.equals(node)) {
					nodeCache.add(node2);
				}
			});
		});
		this.components.forEach(component -> {
			component.getAllNodes().forEach(node2 -> {
				nodeCache.forEach(node1 -> {
					if (excluded != null) for (Node n : excluded) if (n == node2) return;
					if (node2.getVisualPosition().equals(node1.getVisualPosition())) {
						network.add(node2);
					}
				});
			});
		});
		network.addAll(nodeCache);
		if (network.size() > 0) {
			this.networks.add(network);
			this.values.add(NetState.FLOATING);
			this.valueBuffer.add(NetState.FLOATING);
		}
		return this.networks.size() - 1;
	}
	
	protected synchronized void combineNets(int netId) {
		if (netId == -1) return;
		Set<Node> network = new HashSet<>();
		removeNet(netId).forEach(node -> {
			OptionalInt existingNet = findNet(node);
			if (existingNet.isPresent()) {
				network.addAll(removeNet(existingNet.getAsInt()));
			} else {
				network.add(node);
			}
		});
		if (network.size() > 0) {
			this.networks.add(network);
			this.values.add(NetState.FLOATING);
		}
	}

	protected synchronized Set<Node> removeNet(int netId) {
		this.values.remove(netId);
		return this.networks.remove(netId);
	}
	
	protected synchronized OptionalInt findNet(Node node) {
		for (int i = 0; i < this.networks.size(); i++) {
			for (Node n : this.networks.get(i)) {
				if (n.equals(node)) return OptionalInt.of(i);
			}
		}
		return OptionalInt.empty();
	}
	
	public void reconnect(boolean excludeComponents, Component... components) {
		List<Node> nodes = Stream.of(components).flatMap(component -> component.getAllNodes().stream()).toList();
		reconnectNet(nodes, excludeComponents);
	}
	
	protected synchronized NetState getNetValue(int netId) {
		return this.valueBuffer.size() > netId ? this.valueBuffer.get(netId) : NetState.FLOATING;
	}

	protected synchronized void applyNetValue(int netId, NetState state) {
		if (netId >= this.values.size()) return;
		NetState resultingState = combineStates(this.values.get(netId), state, this.shortCircuitType);
		this.values.set(netId, resultingState);
		this.valueBuffer.set(netId, resultingState);
	}
	
	public NetState getNetState(Node node) {
		OptionalInt netId = findNet(node);
		if (netId.isEmpty()) return NetState.FLOATING;
		return getNetValue(netId.getAsInt());
	}
	
	public void setNetState(Node node, NetState state) {
		OptionalInt netId = findNet(node);
		if (netId.isPresent()) applyNetValue(netId.getAsInt(), state);
	}

	public synchronized void resetNetworks() {
		for (int i = 0; i < this.values.size(); i++) this.values.set(i, NetState.LOW);
		for (int i = 0; i < this.valueBuffer.size(); i++) this.valueBuffer.set(i, NetState.LOW);
		this.components.forEach(Component::reset);
	}
	
	protected synchronized void cloneNetBuffer() {
//		this.valueBuffer.clear();
//		this.valueBuffer.addAll(this.values);
		this.values.clear();
		this.valueBuffer.forEach((v) -> this.values.add(NetState.FLOATING));
	}

	public synchronized void updateCircuit() {
		assert !this.virtual : "Can't simulate virtual circuit!";
		this.components.forEach(Component::updateIO);
		cloneNetBuffer();
	}
	
	
	
	
	public synchronized void add(Component component) {
		component.created();
		this.components.add(component);
	}
	
	public synchronized void remove(Component component) {
		component.dispose();
		this.components.remove(component);
	}
	
	public List<Component> getComponents() {
		return this.components;
	}
	
	public synchronized void clear() {
		this.components.clear();
		this.networks.clear();
		this.values.clear();
		this.valueBuffer.clear();
		this.networks.forEach((net) -> this.values.add(NetState.FLOATING));
		this.networks.forEach((net) -> this.valueBuffer.add(NetState.FLOATING));
	}
	
	public int nextFreeId() {
		OptionalInt lastId = this.components.stream().mapToInt(Component::getComponentNr).max();
		if (lastId.isEmpty()) return 0;
		return lastId.getAsInt() + 1;
	}
	
	public Vec4i getCircuitBounds(Predicate<Component> componentPredicate) {
		return getCircuitBounds(componentPredicate, componentPredicate);
	}
	
	public Vec4i getCircuitBounds(Predicate<Component> componentPredicateX, Predicate<Component> componentPredicateY) {
		if (this.components.stream().anyMatch(componentPredicateX) && this.components.stream().anyMatch(componentPredicateY)) {
			int minX = this.components.stream().filter(componentPredicateX).mapToInt(component -> component.getVisualPosition().x).min().getAsInt();
			int minY = this.components.stream().filter(componentPredicateY).mapToInt(component -> component.getVisualPosition().y).min().getAsInt();
			int maxX = this.components.stream().filter(componentPredicateX).mapToInt(component -> component.getVisualPosition().x).max().getAsInt();
			int maxY = this.components.stream().filter(componentPredicateY).mapToInt(component -> component.getVisualPosition().y).max().getAsInt();
			return new Vec4i(minX, minY, maxX, maxY);
		}
		return new Vec4i(0, 0, 0, 0);
	}
	
}
