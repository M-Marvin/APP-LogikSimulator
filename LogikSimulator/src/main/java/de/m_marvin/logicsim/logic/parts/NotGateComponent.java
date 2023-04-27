package de.m_marvin.logicsim.logic.parts;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.ui.widgets.EditorArea;
import de.m_marvin.univec.impl.Vec2i;

public class NotGateComponent extends Component {

	public static final String ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIYSURBVFhHvZc9TsQwEIUTlkUrARKCDi5ACbego0RCVJyAQyAaWlpKOg7BMSipKGgQNCAtEN6beKKJ4/w52XzSU+x1djwztse7aZZlizRNv5OcdSjLm4Jtk6axBLY28FjLe93wHfiD0rzZm66Oz6A59MOOP9kSYhZ+pVcdt/1YR0nhgA8dqKR2RNQ+gxT89RoSVRfUfhGk7wDXZ1J67dhVMLUDrUswOb4DrAN1nEI30K70xoKFyDUJz3/dMbyCOHYovTjUfhG4n4GmY6jFSZ8xDNoDWrmCFSwWdYCVqa0IaeRjOFBkQC+jL9dXQs5cQvfQAfTKD8Cje4Z4gu7yZoFOrEHMNQNd1jWUgbcGfUB1MOP5feBOAY3TO1WIC4hje9KLw84h8+gS0FtmQ2+p0BKcQw/QDqTRHblniGdIf2coMqkh9esAX6grRmcQxzellyTbkEYS0jXkw89pn5eeBGsjZZuDfKnr8Tx2zxAv0HveLFDngvbpgHoY4hb6hE6kF0fFvvVEs8GXQnCpmPahvxnq7AscbDqSW+4ZS5v99hcGUrHfdbOtjJADjWs0AoV9lIDZkAzwu/tOPtzQ/uehzcv/ByXo3ajXrYEOlOyzCNoMaBnWFPELHLdiZKq+6PkvLbE1RAf4zyUWa7g0CbBBMQOSermH2HCoA/aY+JHafkwWCJ2RzIsD3mVkWeKW7FMTmpzzx2QfZFm2+AfiQqWI9JK4DAAAAABJRU5ErkJggg==";

	/* Factory methods */

	public static boolean coursorMove(Circuit circuit, Vec2i coursorPosition) {
		return Component.coursorMove(circuit, coursorPosition, () -> new NotGateComponent(circuit));
	}
	
	/* End of factory methods */
	
	public NotGateComponent(Circuit circuit) {
		super(circuit);
		
		this.label = "not_gate";
		this.inputs.add(new InputNode(this, 0, "in", new Vec2i(-10, 20)));
		this.outputs.add(new OutputNode(this, 1, "out", new Vec2i(50, 20)));
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
		this.outputs.get(0).setState(!this.inputs.get(0).getState());
	}
	
	@Override
	public void render()  {
		
		EditorArea.swapColor(0, 1, 0, 0.4F);
		EditorArea.drawComponentFrame(visualPosition.x, visualPosition.y, getVisualWidth(), getVisualHeight());
		EditorArea.swapColor(1, 1, 1, 1);
		TextRenderer.drawText(visualPosition.x + getVisualWidth() / 2, visualPosition.y + getVisualHeight() / 2, 12, "NOT");
		
	}
	
}
