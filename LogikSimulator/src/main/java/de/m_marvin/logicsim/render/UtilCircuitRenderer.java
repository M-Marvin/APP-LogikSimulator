package de.m_marvin.logicsim.render;

import java.awt.Color;

import de.m_marvin.openui.core.UIRenderMode;
import de.m_marvin.renderengine.buffers.BufferBuilder;
import de.m_marvin.renderengine.buffers.IBufferSource;
import de.m_marvin.renderengine.resources.defimpl.ResourceLocation;
import de.m_marvin.renderengine.translation.PoseStack;

public class UtilCircuitRenderer {
	
	public static void renderLine(int x1, int y1, int x2, int y2, Color color, IBufferSource<UIRenderMode<ResourceLocation>> bufferSource, PoseStack matrixStack) {

		float r = color.getRed() / 255F;
		float g = color.getGreen() / 255F;
		float b = color.getBlue() / 255F;
		float a = color.getAlpha() / 255F;
		
		BufferBuilder buffer = bufferSource.startBuffer(CircuitRenderTypes.lines(20));
		
		buffer.vertex(matrixStack, x1, y1, 0).color(r, g, b, a).endVertex();
		buffer.vertex(matrixStack, x2, y2, 0).color(r, g, b, a).endVertex();
		
		buffer.end();
		
	}
	
}
