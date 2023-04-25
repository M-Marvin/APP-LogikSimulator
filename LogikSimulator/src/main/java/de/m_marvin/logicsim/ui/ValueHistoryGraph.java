package de.m_marvin.logicsim.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

public class ValueHistoryGraph extends Canvas {
	
	public static final int RASTER_SIZE = 1;
	
	protected final int historyDepth;
	protected final float maxValue;
	protected final float minValue;
	protected Map<String, float[]> graphData = new HashMap<>();
	protected Map<String, Color> dataColor = new HashMap<>();
	
	protected GLData glData;
	protected GLCanvas glCanvas;
	protected GLCapabilities glCapabilities;
	protected boolean resized;
	protected boolean initialized = false;
	
	public ValueHistoryGraph(Composite parent, float minValue, float maxValue, int historyDepth) {
		super(parent, SWT.NONE);
		this.setLayout(new FillLayout());
		this.glData = new GLData();
		this.glData.doubleBuffer = true;
		this.glCanvas = new GLCanvas(this, SWT.None, glData);
		this.glCanvas.addListener(SWT.Resize, (event) -> this.resized = true);
		initOpenGL();
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.historyDepth = historyDepth;
	}
	
	protected void initOpenGL() {
		this.glCanvas.setCurrent();
		this.glCapabilities = GL.createCapabilities();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        this.glCanvas.addDisposeListener((e) -> TextRenderer.cleanUpOpenGL());
	    this.resized = true;
	    this.initialized = false;
	}
	
	public GLCanvas getGlCanvas() {
		return glCanvas;
	}
	
	public void putData(String variable, float value) {
		if (this.graphData.containsKey(variable)) {
			float[] data = this.graphData.get(variable);
			data[data.length - 1] = value;
		}
	}
	
	public void nextColumn() {
		this.graphData.forEach((variable, data) -> {
			for (int i = 1; i < data.length; i++) data[i - 1] = data[i];
		});
	}
	
	public void addVariable(String name, Color color) {
		this.graphData.put(name, new float[historyDepth]);
		this.dataColor.put(name, color);
	}
	
	public static final int VISUAL_BUNDING_BOX_OFFSET = 5;
	
	public void render() {
		
		if (this.isDisposed() || this.glCanvas == null) return;
		
		this.glCanvas.setCurrent();
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		
		if (!initialized) {
			initOpenGL();
		}
		
		if (resized) {
			GL11.glViewport(0, 0, this.getSize().x, this.getSize().y);
	        GL11.glLoadIdentity();
		    GL11.glOrtho(0.0, this.getSize().x, this.getSize().y, 0.0, 0.0, 1.0);
			GL11.glClearColor(0, 0, 0, 1);
		}
		
		drawRaster();
		
		EditorArea.swapColor(1, 1, 1, 1);
		
		for (String name : this.graphData.keySet()) {
			Color c = this.dataColor.get(name);
			EditorArea.swapColor(c.getRed(), c.getGreen(), c.getBlue(), 1F);
			float y = 0;
			
			GL11.glBegin(GL11.GL_LINE_STRIP);
			for (int i = 0; i < historyDepth; i++) {
				float x = i * (getSize().x / historyDepth);
				y = (1 - ((this.graphData.get(name)[i] - this.minValue) / (this.maxValue - this.minValue))) * getSize().y;
				GL11.glVertex2d(x, y);
			}
			GL11.glEnd();

			GL11.glBegin(GL11.GL_QUAD_STRIP);
			for (int i = 0; i < historyDepth; i++) {
				float x = i * (getSize().x / historyDepth);
				y = (1 - ((this.graphData.get(name)[i] - this.minValue) / (this.maxValue - this.minValue))) * getSize().y;
				EditorArea.swapColor(c.getRed(), c.getGreen(), c.getBlue(), 0.8F);
				GL11.glVertex2d(x, y);
				EditorArea.swapColor(c.getRed(), c.getGreen(), c.getBlue(), 0F);
				GL11.glVertex2d(x, y + getSize().y / 2);
				
			}
			GL11.glEnd();
			
			float ty = Math.max(Math.min(y, getSize().y - 5), 5);
			EditorArea.swapColor(1, 1, 1, 1F);
			TextRenderer.drawText(getSize().x, ty, 10, name, TextRenderer.ORIGIN_RIGHT);
			
		}
		
		this.glCanvas.swapBuffers();
		
	}
	
	public void drawRaster() {
		
		int rasterColumns = (int) (this.maxValue - this.minValue);
		
		int rasterSize1 = Math.max(this.getSize().y / rasterColumns, 10);
		int rasterSize2 = Math.max(this.getSize().x / historyDepth, 10);
		
		EditorArea.swapColor(1.0F, 0.5F, 0.0F, 0.4F);
		
		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < this.getSize().x; i+= RASTER_SIZE * rasterSize2) {
			GL11.glVertex2f(i, 0);
			GL11.glVertex2f(i, this.getSize().y);
		}
		for (int j = 0; j < this.getSize().y; j += RASTER_SIZE * rasterSize1) {
			GL11.glVertex2f(0, j);
			GL11.glVertex2f(this.getSize().x, j);
		}
		GL11.glEnd();
		
	}
	
}
