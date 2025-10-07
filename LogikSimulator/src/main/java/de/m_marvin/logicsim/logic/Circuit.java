package de.m_marvin.logicsim.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.simulator.AsyncArrayList;
import de.m_marvin.logicsim.logic.simulator.FastAsyncMap;
import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.univec.impl.Vec4i;

/**
 * The class containing all information about an circuit and its current simulation state.
 * The methods of this class are thread safe when calling them but the methods that return Lists, Maps or Sets do return the internal object.
 * Modifications or calls to that object are <b>NOT guaranteed to be threads safe</b> and should be surrounded by an synchronized block with the circuit as lock-object!
 * The returned objects are mainly intended to be used to directly read data from the simulation without querying the objects over and over.
 * 
 * @author Marvin K.
 *
 */
public class Circuit {
	
	public static final String DEFAULT_BUS_LANE = "bus0";
	
	public static Random floatingValue = new Random();
	public static boolean shortCircuitValue = false;
	
	public static boolean getFloatingValue() {
		return floatingValue.nextBoolean();
	}
	
	public static boolean getShortCircuitValue() {
		shortCircuitValue = !shortCircuitValue;
		return floatingValue.nextBoolean();
	}
	
	public static NetState combineStates(NetState stateA, NetState stateB, ShortCircuitType type) {
		if (stateA == NetState.FLOATING) return stateB;
		if (stateB == NetState.FLOATING) return stateA;
		switch (type) {
		default:
		case HIGH_LOW_SHORT:
			if (stateA == NetState.SHORT_CIRCUIT) return NetState.SHORT_CIRCUIT;
			if (stateB == NetState.SHORT_CIRCUIT) return NetState.SHORT_CIRCUIT;
			if (stateA != stateB) return NetState.SHORT_CIRCUIT;
			return stateA;
		case PREFER_HIGH:
			if (stateA == NetState.SHORT_CIRCUIT) return NetState.HIGH;
			if (stateB == NetState.SHORT_CIRCUIT) return NetState.HIGH;
			return stateA == NetState.HIGH || stateB == NetState.HIGH ? NetState.HIGH : NetState.LOW;
		case PREFER_LOW:
			if (stateA == NetState.SHORT_CIRCUIT) return NetState.LOW;
			if (stateB == NetState.SHORT_CIRCUIT) return NetState.LOW;
			return stateA == NetState.LOW || stateB == NetState.LOW ? NetState.LOW : NetState.HIGH;
		}
	}
	
	public static NetState safeLaneRead(Map<String, NetState> laneData, String lane) {
		NetState state = laneData.get(lane);
		if (state == null) return NetState.FLOATING;
		return state;
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
		
		public boolean isLogicalState() {
			return this == HIGH || this == LOW;
		}
		
		public boolean isErrorState() {
			return !isLogicalState();
		}
	}
	
	protected static Number castToBits(Long value, int bitCount) {
		if (bitCount > 32) return value.longValue();
		if (bitCount > 16) return value.intValue();
		if (bitCount > 8) return value.shortValue();
		return value.byteValue();
	}
	
	public static Map<String, Long> getLaneData(Map<String, NetState> laneData) {
		
		Map<String, Long> busData = new HashMap<>();
		Map<String, Integer> bitCounts = new HashMap<>();
		for (String lane : laneData.keySet()) {
			String[] laneParts = lane.split("(?<=\\D)(?=\\d)");
			try {
				String bus = laneParts[0];
				int bit = Integer.parseInt(laneParts[1]);
				boolean value = safeLaneRead(laneData, lane).getLogicState();
				int bitCount = bitCounts.getOrDefault(bus, 0);
				if (bit + 1 > bitCount) {
					bitCount = bit + 1;
					bitCounts.put(bus, bitCount);
				}
				
				Long dataValue = busData.getOrDefault(bus, 0L);
				if (value) dataValue += (1L << bit);
				busData.put(bus, dataValue);
			} catch (NumberFormatException e) {}
		}
		return busData;
		
	}
	
	protected final List<List<Node>> networks = new AsyncArrayList<>();
	protected List<Map<String, NetState>> valuesSec = new AsyncArrayList<>();
	protected List<Map<String, NetState>> valuesPri = new AsyncArrayList<>();
	protected final List<Component> components = new AsyncArrayList<>();
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
	
	
	
	protected synchronized List<Node> removeNet(int netId) {
		if (this.valuesPri.size() > netId) this.valuesPri.remove(netId);
		if (this.valuesSec.size() > netId) this.valuesSec.remove(netId);
		return this.networks.remove(netId);
	}
	
	protected OptionalInt findNet(Node node) {
		for (int i = 0; i < this.networks.size(); i++) {
			for (Node n : this.networks.get(i)) {
				if (n.equals(node)) return OptionalInt.of(i);
			}
		}
		return OptionalInt.empty();
	}
	
	public void reconnect(boolean disconnect, Component... components) {
		
		if (!disconnect) {
			
			for (Component c : components) {
				
				for (Node n : c.getAllNodes()) {
					
					List<Node> newNet = null;
					
					Vec2i pos = n.getVisualPosition();
					
					for (int net = 0; net < this.networks.size(); net++) {
						List<Node> network = this.networks.get(net);
						for (Node n2 : network) {
							
							if (n2.getVisualPosition().equals(pos) || n2.equals(n)) {
								
								if (newNet == null) {
									newNet = network;
								} else {
									removeNet(net);
									newNet.addAll(network);
									

								}
								
								break;
								
							}
							
						}
					}

					if (newNet == null) {
						newNet = new AsyncArrayList<>();
						newNet.add(n);
						this.networks.add(newNet);
						this.valuesPri.add(new FastAsyncMap<>());
						this.valuesSec.add(new FastAsyncMap<>());
					} else {
						newNet.add(n);
					}
					
				}
				
			}
			
		} else {
			
			List<Component> disconnectedComponents = new ArrayList<>();
			
			for (Component c : components) {
				for (Node n : c.getAllNodes()) {
					
					
					for (int net = 0; net < this.networks.size(); net++) {
						List<Node> network = this.networks.get(net);
						
						if (network.contains(n)) {
							
							removeNet(net);
							for (Node n2 : network) {
								if (!disconnectedComponents.contains(n2.getComponent())) disconnectedComponents.add(n2.getComponent());
							}
							
						}
						
					}
					
					
				}
			}
			
			for (Component c : components) {
				disconnectedComponents.remove(c);
			}
			
			if (!disconnectedComponents.isEmpty()) {
				reconnect(false, disconnectedComponents.toArray(i -> new Component[i]));
			}
			
		}
		
	}
	
	protected NetState getNetValue(int netId, String lane) {
		return this.valuesSec.size() > netId ? safeLaneRead(valuesSec.get(netId), lane) : NetState.FLOATING;
	}

	protected Map<String, NetState> getNetLanes(int netId) {
		return this.valuesSec.size() > netId ? this.valuesSec.get(netId) : null;
	}

	protected Map<String, NetState> getNetLanesPri(int netId) {
		return this.valuesSec.size() > netId ? this.valuesSec.get(netId) : null;
	}

	protected void applyNetValue(int netId, NetState state, String lane) {
		if (netId >= this.valuesPri.size()) return;
		NetState resultingState = combineStates(safeLaneRead(this.valuesPri.get(netId), lane), state, this.shortCircuitType);
		this.valuesPri.get(netId).put(lane, resultingState);
		//this.valuesSec.get(netId).put(lane, resultingState);
	}
	
	protected void applyNetLanes(int netId, Map<String, NetState> laneStates) {
		if (netId >= this.valuesPri.size()) return;
		Map<String, NetState> laneStatesPri = this.valuesPri.get(netId);
		//Map<String, NetState> laneStatesSec = this.valuesSec.get(netId);
		for (String lane : laneStates.keySet()) {
			NetState resultingState = combineStates(safeLaneRead(laneStatesPri, lane), laneStates.get(lane), this.shortCircuitType);
			if (lane != null && resultingState != null) {
				laneStatesPri.put(lane, resultingState);
				//laneStatesSec.put(lane, resultingState);
			}
		}
	}
	
	public NetState getNetState(Node node) {
		return getNetState(node, DEFAULT_BUS_LANE);
	}
	
	public NetState getNetState(Node node, String lane) {
		OptionalInt netId = findNet(node);
		if (netId.isEmpty()) return NetState.FLOATING;
		return getNetValue(netId.getAsInt(), lane);
	}
	
	public Map<String, NetState> getLaneMapReference(Node node) {
		OptionalInt netId = findNet(node);
		if (netId.isEmpty()) return null;
		return getNetLanes(netId.getAsInt());
	}

	public Map<String, NetState> getLaneMapReferencePri(Node node) {
		OptionalInt netId = findNet(node);
		if (netId.isEmpty()) return null;
		return getNetLanesPri(netId.getAsInt());
	}
	
	public void setNetState(Node node, NetState state) {
		setNetState(node, state, DEFAULT_BUS_LANE);
	}
	
	public void writeLanes(Node node, Map<String, NetState> laneStates) {
		if (laneStates == null || laneStates.isEmpty()) return;
		OptionalInt netId = findNet(node);
		if (netId.isPresent()) applyNetLanes(netId.getAsInt(), laneStates);
	}
	
	public void setNetState(Node node, NetState state, String lane) {
		OptionalInt netId = findNet(node);
		if (netId.isPresent()) applyNetValue(netId.getAsInt(), state, lane);
	}

	public void resetNetworks() {
		for (int i = 0; i < this.valuesPri.size(); i++) this.valuesPri.get(i).clear();
		for (int i = 0; i < this.valuesSec.size(); i++) {
			for (String lane : this.valuesSec.get(i).keySet()) {
				this.valuesSec.get(i).put(lane, NetState.LOW);
			}
		}
		this.components.forEach(Component::reset);
	}
	
	public void updateCircuit() {
		assert !this.virtual : "Can't simulate virtual circuit!";
		this.valuesPri.forEach(holder -> holder.clear());
		this.components.forEach(Component::updateIO);
		
		
		
//		List<Map<String, NetState>> temp = this.valuesSec;
//		this.valuesSec = this.valuesPri;
//		this.valuesPri = temp;
//		this.valuesPri.forEach(m -> m.keySet().forEach(k -> m.put(k, NetState.FLOATING)));
		
		for (int i = 0; i < this.valuesPri.size(); i++) {
			Map<String, NetState> laneDataSec = this.valuesSec.get(i);
			Map<String, NetState> laneDataPri = this.valuesPri.get(i);
			laneDataSec.putAll(laneDataPri);
			laneDataSec.keySet().stream().filter(lane -> !laneDataPri.containsKey(lane)).toList().forEach(laneDataSec::remove);
		}
		
//		for (int i = 0; i < this.valuesPri.size(); i++) {
//			Map<String, NetState> laneDataSec = this.valuesSec.get(i);
//			Map<String, NetState> laneDataPri = this.valuesPri.get(i);
//			laneDataSec.putAll(laneDataPri);
//			laneDataSec.keySet().stream().filter(lane -> !laneDataPri.containsKey(lane)).toList().forEach(laneDataSec::remove);
//		}
	}
	
	
	
	
	public synchronized boolean isNodeConnected(Node node) {
		for (int i = 0; i < this.networks.size(); i++) {
			for (Node n : this.networks.get(i)) {
				if (n.equals(node)) {
					return networks.size() > i ?  networks.get(i).size() > 1 : false;
				}
			}
		}
		return false;
	}
	
	public synchronized void add(Component component) {
		if (!this.virtual) component.created();
		this.components.add(component);
	}
	
	public synchronized void remove(Component component) {
		if (!this.virtual) component.dispose();
		this.components.remove(component);
	}
	
	
	
	public List<Component> getComponents() {
		return this.components;
	}

	public List<Component> getComponents(Predicate<Component> componentPredicate) {
		return this.components.stream().filter(componentPredicate).toList();
	}
	
	public synchronized void clear() {
		this.components.clear();
		this.networks.clear();
		this.valuesPri.clear();
		this.valuesSec.clear();
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
