package de.m_marvin.logicsim.logic.parts;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.EditorArea;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.univec.impl.Vec2i;

public class ButtonComponent extends Component {
	
	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAF3SURBVFhHzZcNbsMgDIVLf6aeYfc/ZKVuo34kLzKGpEAI3SdZhVKbZ0NIcd77u3PucZq4ivmpGdBtsDV2klhf8nGeemVYAX9ibmpWUyr8InYT+0HHTvYUQxV+Qy8d1/1WoWARYIGApLQdYXwkGbDrtSerEhh/SdIKwPoMpWrHHsFoAW+XYDhWAM6BoZxxEM3tj2Ar8PHHsBUEXILuAXugNhAnL/FL4tuSc7B0KXSwEh/+nu+B2x4BtZMD7QNc6x5omTxLi4Buk4PcOcAJ8JkzsjZ5zgdG0MbrGP8JoiBoc5eiMtrJspX5mh98MMb4CfzBkcdxEl8rYVZrGfQiiq8FHJn5Ki1PQVf+pYBhe0COgMueCsD3ezYLNrT9PveHN5wFGqjLXhg6AAFRfByCugK8LLBEcMC4NmRGq4VPWbTEOhAE4ObSig4cTSLopFCBUPpwL0VjhgJ4LwQ2U91vqQKAmFD5ICDzMiJPuTVrMe/YEmfHwj7w3t9f/w54lcD7KeEAAAAASUVORK5CYII=";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new ButtonComponent(circuit));
	}
	
	/* End of factory methods */
	
	protected boolean toggle;
	
	public ButtonComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "button";
		this.outputs.add(new OutputNode(this, 2, "out", new Vec2i(50, 20)));
	}
	
	@Override
	public int getVisualWidth() {
		return 40;
	}

	@Override
	public int getVisualHeight() {
		return 40;
	}

	@Override
	public void updateIO() {
		this.outputs.get(0).setState(this.toggle);
	}
	
	@Override
	public void click(Vec2i clickPosition) {
		this.toggle = !this.toggle;
	}
	
	@Override
	public void render()  {
		
		if (toggle) {
			EditorArea.swapColor(0, 0, 1, 0.4F);
		} else {
			EditorArea.swapColor(0, 1, 1, 0.4F);
		}
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, this.toggle ? "ON" : "OFF");
		
	}
	
}
