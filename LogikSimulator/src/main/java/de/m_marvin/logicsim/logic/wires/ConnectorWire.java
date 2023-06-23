package de.m_marvin.logicsim.logic.wires;

import java.util.Map;

import com.google.gson.JsonObject;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.NetConnector;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.univec.impl.Vec2i;

public class ConnectorWire extends NetConnector {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABLSURBVFhH7ZYxCgAwCAPT/v/PLQUprtpClrslmx5mUeBmRB5WZJU8o8yMtGEX6J4v10UFTyCAAAIIIIAAAgjYBX78hB3uXioAM9IG5AoDMvJCY58AAAAASUVORK5CYII=";

	public static final float VISUAL_LINE_WIDTH = 2.0F;
	
	/* Factory methods */
	
	protected static ConnectorWire currentWireA;
	protected static ConnectorWire currentWireB;
	
	public static void placeClick(Circuit circuit, Vec2i coursorPosition) {
		if (currentWireA == null) {
			currentWireA = new ConnectorWire(circuit);
			currentWireA.setPosA(coursorPosition);
			currentWireA.setPosB(coursorPosition);
			circuit.add(currentWireA);
		} else {
			if (currentWireB == null) {
				circuit.reconnect(false, currentWireA);
				currentWireA = null;
			} else {
				circuit.reconnect(false, currentWireA);
				currentWireA = currentWireB;
				currentWireB = null;
			}
		}
	}
	
	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		if (currentWireA != null) {
			if (currentWireA.getPosA().x == coursorPosition.x || currentWireA.getPosA().y == coursorPosition.y) {
				if (currentWireB != null) {
					circuit.remove(currentWireB);
					currentWireB = null;
				}
				currentWireA.setPosB(coursorPosition);
			} else {
				if (currentWireB == null) {
					currentWireB = new ConnectorWire(circuit);
					circuit.add(currentWireB);
				}
				Vec2i dist = currentWireA.getPosA().sub(coursorPosition);
				if (Math.abs(dist.x) > Math.abs(dist.y)) {
					currentWireA.setPosB(new Vec2i(coursorPosition.x, currentWireA.getPosA().y));
					currentWireB.setPosA(new Vec2i(coursorPosition.x, currentWireA.getPosA().y));
					currentWireB.setPosB(coursorPosition);
				} else {
					currentWireA.setPosB(new Vec2i(currentWireA.getPosA().x, coursorPosition.y));
					currentWireB.setPosA(new Vec2i(currentWireA.getPosA().x, coursorPosition.y));
					currentWireB.setPosB(coursorPosition);
				}
			}
			return true;
		}
		return false;
	}
	
	public static void abbortPlacement(Circuit circuit) {
		if (currentWireA != null) {
			circuit.remove(currentWireA);
			currentWireA = null;
		}
		if (currentWireB != null) {
			circuit.remove(currentWireB);
			currentWireB = null;
		}
	}
	
	/* End of factory methods */
	
	protected Vec2i posA = new Vec2i();
	protected Vec2i posB = new Vec2i();
	
	public ConnectorWire(Circuit circuit) {
		super(circuit);
		
		this.label = "wire";
		this.passives.add(new PassivNode(this, 0, "wire", posA));
		this.passives.add(new PassivNode(this, 0, "wire", posB));
	}
	
	public Vec2i getPosA() {
		return posA.add(visualPosition);
	}
	
	public void setPosA(Vec2i posA) {
		Vec2i otherNode = posB.add(visualPosition);
		this.visualPosition = new Vec2i(Math.min(posA.x, otherNode.x), Math.min(posA.y, otherNode.y)).sub(3, 3);
		this.posB.setI(otherNode.sub(visualPosition));
		this.posA.setI(posA.sub(visualPosition));
	}
	
	public Vec2i getPosB() {
		return posB.add(visualPosition);
	}
	
	public void setPosB(Vec2i posB) {
		Vec2i otherNode = posA.add(visualPosition);
		this.visualPosition = new Vec2i(Math.min(posB.x, otherNode.x), Math.min(posB.y, otherNode.y)).sub(3, 3);
		this.posA.setI(otherNode.sub(visualPosition));
		this.posB.setI(posB.sub(visualPosition));
	}
	
	@Override
	public void render()  {
		
		Vec2i position1 = this.posA.add(visualPosition);
		Vec2i position2 = this.posB.add(visualPosition);
		
		NetState logicalState = this.circuit.getNetState(this.passives.get(0));
		
		if (logicalState == NetState.FLOATING) {
			EditorArea.swapColor(1, 1, 1, 1);
		} else if (logicalState.getLogicState()) {
			EditorArea.swapColor(0, 0, 1, 1);
		} else {
			EditorArea.swapColor(0, 1, 1, 1);
		}
		
		EditorArea.drawLine(VISUAL_LINE_WIDTH, position1.x, position1.y, position2.x, position2.y);
		
	}

	@Override
	public int getVisualWidth() {
		return Math.abs(this.posA.x - this.posB.x) + 6;
	}

	@Override
	public int getVisualHeight() {
		return Math.abs(this.posA.y - this.posB.y) + 6;
	}
	
	@Override
	public void serialize(JsonObject json) {
		super.serialize(json);
		json.addProperty("x1", this.posA.x);
		json.addProperty("y1", this.posA.y);
		json.addProperty("x2", this.posB.x);
		json.addProperty("y2", this.posB.y);
	}
	
	@Override
	public void deserialize(JsonObject json) {
		super.deserialize(json);
		this.posA.x = json.get("x1").getAsInt();
		this.posA.y = json.get("y1").getAsInt();
		this.posB.x = json.get("x2").getAsInt();
		this.posB.y = json.get("y2").getAsInt();
	}

	@Override
	public Map<String, NetState> getLaneData() {
		return this.passives.get(0).getLaneReference();
	}
	
}
