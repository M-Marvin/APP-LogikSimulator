package de.m_marvin.logicsim.render;

import java.util.function.Function;

import org.lwjgl.opengl.GL33;

import de.m_marvin.openui.core.UIRenderMode;
import de.m_marvin.openui.flatmono.UIRenderModes;
import de.m_marvin.renderengine.GLStateManager;
import de.m_marvin.renderengine.resources.defimpl.ResourceLocation;
import de.m_marvin.renderengine.utility.NumberFormat;
import de.m_marvin.renderengine.utility.Utility;
import de.m_marvin.renderengine.vertices.RenderPrimitive;
import de.m_marvin.renderengine.vertices.VertexFormat;

public class CircuitRenderTypes {
	
	public static UIRenderMode<ResourceLocation> lines(int width) {
		return UIRenderModes.lines(width);
	}
	
	public static UIRenderMode<ResourceLocation> triangles() {
		return UIRenderModes.plainSolid();
	}
	
	public static UIRenderMode<ResourceLocation> triangleStrip() {
		return trianglesStrip;
	}
	public static final UIRenderMode<ResourceLocation> trianglesStrip = new UIRenderMode<ResourceLocation>(
			RenderPrimitive.TRIANGLES_STRIP, 
			new VertexFormat()
				.appand("position", NumberFormat.FLOAT, 3, false)
				.appand("color", NumberFormat.FLOAT, 4, false),
			UIRenderModes.SHADER_PLAIN_SOLID, 
			(shader, container) -> {
				shader.getUniform("ProjMat").setMatrix4f(container.getProjectionMatrix());
				GLStateManager.enable(GL33.GL_DEPTH_TEST);
				GLStateManager.enable(GL33.GL_BLEND);
				GLStateManager.blendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
			}
	);
	
	public static UIRenderMode<ResourceLocation> lineStrip(int size) {
		return lineStrip.apply(size);
	}
	private static final Function<Integer, UIRenderMode<ResourceLocation>> lineStrip = Utility.memorize((width) -> {
		return new UIRenderMode<ResourceLocation>(
			RenderPrimitive.LINES_STRIP, 
			new VertexFormat()
				.appand("position", NumberFormat.FLOAT, 3, false)
				.appand("color", NumberFormat.FLOAT, 4, false),
			UIRenderModes.SHADER_PLAIN_SOLID, 
			(shader, container) -> {
				shader.getUniform("ProjMat").setMatrix4f(container.getProjectionMatrix());
				GLStateManager.enable(GL33.GL_DEPTH_TEST);
				GLStateManager.enable(GL33.GL_BLEND);
				GLStateManager.blendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
				GLStateManager.lineWidth(width);
			}
		);
	});
	
	public static UIRenderMode<ResourceLocation> points(int size) {
		return points.apply(size);
	}
	private static final Function<Integer, UIRenderMode<ResourceLocation>> points = Utility.memorize((width) -> {
		return new UIRenderMode<ResourceLocation>(
			RenderPrimitive.POINTS, 
			new VertexFormat()
				.appand("position", NumberFormat.FLOAT, 3, false)
				.appand("color", NumberFormat.FLOAT, 4, false),
			UIRenderModes.SHADER_PLAIN_SOLID, 
			(shader, container) -> {
				shader.getUniform("ProjMat").setMatrix4f(container.getProjectionMatrix());
				GLStateManager.enable(GL33.GL_DEPTH_TEST);
				GLStateManager.enable(GL33.GL_BLEND);
				GLStateManager.blendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
				GLStateManager.pointSize(width);
			}
		);
	});
	
}
