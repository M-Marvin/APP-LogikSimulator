package de.m_marvin.logicsim.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.util.Registries.ComponentEntry;
import de.m_marvin.univec.impl.Vec2i;

public class EditorArea extends Canvas implements MouseListener, MouseMoveListener, KeyListener {
	
	public static final int RASTER_SIZE = 10;
	
	public Circuit circuit;
	public Vec2i visualOffset = new Vec2i(0, 0);
	
	protected Component grabedComponent = null;
	protected Vec2i grabOffset = new Vec2i(0, 0);
	protected Vec2i mousePosition = new Vec2i(0, 0);
	protected ComponentEntry activePlacement;
	
	protected GLData glData;
	protected GLCanvas glCanvas;
	protected GLCapabilities glCapabilities;
	protected boolean resized;
	protected boolean initialized = false;
	
	public EditorArea(Composite parent) {
		super(parent, SWT.NONE);
		this.setLayout(new FillLayout());
		this.glData = new GLData();
		this.glData.doubleBuffer = true;
		this.glCanvas = new GLCanvas(this, SWT.None, glData);
		this.glCanvas.addListener(SWT.Resize, (event) -> this.resized = true);
		this.glCanvas.addMouseListener(this);
		this.glCanvas.addMouseMoveListener(this);
		this.glCanvas.addKeyListener(this);
		initOpenGL();
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
	    this.resized = true;
	    this.initialized = false;
	}
	
	public void setCircuit(Circuit circuit) {
		this.circuit = circuit;
	}
	
	public Circuit getCircuit() {
		return circuit;
	}
	
	public GLCanvas getGlCanvas() {
		return glCanvas;
	}
	
	public void setActivePlacement(ComponentEntry activePlacement) {
		if (this.activePlacement != null) removeActivePlacement();
		this.activePlacement = activePlacement;
		this.redraw();
	}
	
	public void removeActivePlacement() {
		if (this.activePlacement == null) return;
		this.activePlacement.placementAbbortMethod().accept(circuit);
		this.activePlacement = null;
		this.redraw();
	}
	
	public ComponentEntry getActivePlacement() {
		return activePlacement;
	}
	
	public void setGrabbedComponent(Component component) {
		if (this.grabedComponent != null) removeUnplacedComponent();
		this.grabedComponent = component;
		this.grabOffset = this.mousePosition.sub(component.getVisualPosition());
		this.circuit.reconnect(true, component);
		this.redraw();
	}
	
	public void releaseGrabbedComponent() {
		if (this.grabedComponent == null) return;
		this.grabedComponent.setVisualPosition(this.mousePosition.sub(grabOffset));
		this.circuit.reconnect(false, this.grabedComponent);
		this.grabedComponent = null;
		this.grabOffset = new Vec2i(0, 0);
		this.redraw();
	}
	
	public Component getGrabbedComponent() {
		return this.grabedComponent;
	}
	
	public void removeUnplacedComponent() {
		if (this.grabedComponent == null) return;
		this.circuit.remove(grabedComponent);
		this.grabOffset = new Vec2i(0, 0);
		this.grabedComponent = null;
		this.redraw();
	}
	
	@Override
	public void mouseUp(MouseEvent event) {

		if (this.circuit == null) return;
		
		if (event.button == 1) {
			if (this.grabedComponent != null) {
				this.releaseGrabbedComponent();
			}
		}
	}
	
	@Override
	public void mouseDown(MouseEvent event) {

		if (this.circuit == null) return;
		
		if (this.activePlacement == null) {
			
			if (this.grabedComponent == null) {
				for (Component component : this.circuit.getComponents()) {
					if (component.getVisualPosition().x <= this.mousePosition.x &&
						component.getVisualPosition().x + component.getVisualWidth() >= this.mousePosition.x &&
						component.getVisualPosition().y <= this.mousePosition.y &&
						component.getVisualPosition().y + component.getVisualHeight() >= this.mousePosition.y) {
						
						if (event.button == 1) {
							this.setGrabbedComponent(component);
						} else {
							component.click(mousePosition);
						}
						break;			
					}
				}
			}
		} else if (event.button == 1) {
			this.activePlacement.placementClickMethod().accept(circuit, this.mousePosition);
		}
		
	}

	@Override
	public void mouseMove(MouseEvent event) {
		
		if (this.circuit == null) return;
		
		this.mousePosition = new Vec2i(event.x, event.y).clamp(this.grabOffset, Vec2i.fromVec(this.getSize()));
		Vec2i rasterOffset = this.mousePosition.add(RASTER_SIZE / 2, RASTER_SIZE / 2).module(RASTER_SIZE).sub(RASTER_SIZE / 2, RASTER_SIZE / 2);
		this.mousePosition.subI(rasterOffset);
		
		if (this.activePlacement != null) {
			boolean redraw = this.activePlacement.placementMoveMethod().apply(circuit, mousePosition);
			if (redraw) this.redraw();
		}
		
		if (this.grabedComponent != null) {
			this.redraw();
		}
		
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent event) {}
	
	@Override
	public void keyPressed(KeyEvent event) {

		if (this.circuit == null) return;
		
		if (event.keyCode == SWT.DEL) {
			if (this.grabedComponent != null) {
				this.removeUnplacedComponent();
			} else {
				for (Component component : this.circuit.getComponents()) {
					if (component.getVisualPosition().x <= this.mousePosition.x &&
						component.getVisualPosition().x + component.getVisualWidth() >= this.mousePosition.x &&
						component.getVisualPosition().y <= this.mousePosition.y &&
						component.getVisualPosition().y + component.getVisualHeight() >= this.mousePosition.y) {
						
						this.circuit.remove(component);
						this.circuit.reconnect(true, component);
						this.redraw();
						break;			
					}
				}
			}
		} else if (event.keyCode == SWT.ESC) {
			if (this.grabedComponent != null) {
				this.removeUnplacedComponent();
			} else if (this.activePlacement != null) {
				this.removeActivePlacement();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {}

	public static final int VISUAL_BUNDING_BOX_OFFSET = 5;
	
	public void render() {
		
		if (this.isDisposed() || this.glCanvas == null || this.circuit == null) return;
		
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

		swapColor(1, 1, 1, 1);
		
		circuit.getComponents().forEach(component -> {
			
			component.render();
			
			swapColor(1, 1, 1, 1);
			
			component.getInputs().forEach(inputNode -> {
				Vec2i position = inputNode.getVisualOffset().add(component.getVisualPosition());
				drawNode(position, 1);
			});
			component.getOutputs().forEach(outputNode -> {
				Vec2i position = outputNode.getVisualOffset().add(component.getVisualPosition());
				drawNode(position, 2);
			});
			component.getPassives().forEach(passivNode -> {
				Vec2i position = passivNode.getVisualOffset().add(component.getVisualPosition());
				drawNode(position, 3);
			});
			
		});
		
		if (this.grabedComponent != null) {
			
			Vec2i topLeft = this.mousePosition.sub(grabOffset).sub(VISUAL_BUNDING_BOX_OFFSET, VISUAL_BUNDING_BOX_OFFSET);
			int width = this.grabedComponent.getVisualWidth() + VISUAL_BUNDING_BOX_OFFSET * 2;
			int height = this.grabedComponent.getVisualHeight() + VISUAL_BUNDING_BOX_OFFSET * 2;
			
			swapColor(0, 1, 0, 0.4F);
			drawRectangle(1, topLeft.x , topLeft.y, width, height);
			
		}
		
		this.glCanvas.swapBuffers();
		
	}
	
	public static void drawNode(Vec2i position, int type) {
		switch (type) {
		case 1:
			drawCircle(1, position.x, position.y, 5);
			drawPoint(4, position.x, position.y);
			break;
		case 2:
			drawCircle(1, position.x, position.y, 5);
			drawLine(1, position.x - 5, position.y - 5, position.x + 5, position.y + 5);
			drawLine(1, position.x - 5, position.y + 5, position.x + 5, position.y - 5);
			break;
		case 3:
			drawCircle(1, position.x, position.y, 5);
			break;
		}
	}
	
	public void drawRaster() {
		
		int rasterSize1 = 10;
		
		Vec2i rasterOffset = this.visualOffset.module(RASTER_SIZE);
		Vec2i renderOffset = this.visualOffset.sub(rasterOffset);
		
		swapColor(1.0F, 0.5F, 0.0F, 0.4F);
		
		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < this.getSize().x; i+= RASTER_SIZE * rasterSize1) {
			GL11.glVertex2f(i + renderOffset.x, renderOffset.y);
			GL11.glVertex2f(i + renderOffset.x, renderOffset.y + this.getSize().y);
		}
		for (int j = 0; j < this.getSize().x; j += RASTER_SIZE * rasterSize1) {
			GL11.glVertex2f(renderOffset.x, j + renderOffset.y);
			GL11.glVertex2f(renderOffset.x + this.getSize().x, j + renderOffset.y);
		}
		GL11.glEnd();
		
		for (int i = 0; i < this.getSize().x; i+= RASTER_SIZE * rasterSize1) {
			for (int j = 0; j < this.getSize().x; j += RASTER_SIZE * rasterSize1) {
				drawRectangle(1, i + renderOffset.x - 5, j + renderOffset.y - 5, 10, 10);
			}
		}
		
		swapColor(1, 1, 1, 0.3F);
		
		GL11.glPointSize(1);
		GL11.glBegin(GL11.GL_POINTS);
		for (int i = 0; i < this.getSize().x; i += RASTER_SIZE) {
			for (int j = 0; j < this.getSize().y; j += RASTER_SIZE) {
				if (i % (rasterSize1 * RASTER_SIZE) != 0 && j % (rasterSize1 * RASTER_SIZE) != 0) GL11.glVertex2f(i + renderOffset.x, j + renderOffset.y);
			}
		}
		GL11.glEnd();
	}
	
	public static void swapColor(float r, float g, float b, float a) {
		GL11.glColor4f(r, g, b, a);
	}
	
	public static void drawRectangle(float width, float x, float y, float w, float h) {
		GL11.glLineWidth(width);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		GL11.glVertex2f(x, y);
		GL11.glVertex2f(x + w, y);
		GL11.glVertex2f(x + w, y + h);
		GL11.glVertex2f(x, y + h);
		GL11.glVertex2f(x, y);
		GL11.glEnd();
	}

	public static void drawLine(float width, int x, int y, int x2, int y2) {
		GL11.glLineWidth(width);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		GL11.glVertex2f(x, y);
		GL11.glVertex2f(x2, y2);
		GL11.glEnd();
	}

	public static void drawPoint(float size, int x, int y) {
		GL11.glPointSize(size);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex2f(x, y);
		GL11.glEnd();
	}
	
	public static void drawCircle(float width, float x, float y, float r) {
		GL11.glLineWidth(width);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		int segmentCount = 10;
		for (int a = 0; a <= 360; a += 360 / segmentCount) {
			double cx = Math.sin(Math.toRadians(a)) * r;
			double cy = Math.cos(Math.toRadians(a)) * r;
			GL11.glVertex2d(x + cx, y + cy);
		}
		GL11.glEnd();
	}
	
	public static void drawComponentFrame(float x, float y, float w, float h) {
		
		int f = 2;
		int f1 = (int) (h / 3);
		
		GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
		GL11.glVertex2f(x + f, y + f + 1);
		GL11.glVertex2f(x + w - f - 1, y + f + 1);
		GL11.glVertex2f(x + f, y + h - f - 1);
		GL11.glVertex2f(x + w - f - 1, y + h - f - 1);
		GL11.glEnd();
		
		swapColor(1F, 0.4F, 0, 1);
		
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2f(x, y + (h - f1) / 2);
		GL11.glVertex2f(x, y);
		GL11.glVertex2f(x, y);
		GL11.glVertex2f(x + w, y);
		GL11.glVertex2f(x + w, y + (h - f1) / 2);
		GL11.glVertex2f(x + w, y);
		
		GL11.glVertex2f(x, y + h - (h - f1) / 2);
		GL11.glVertex2f(x, y + h);
		GL11.glVertex2f(x, y + h);
		GL11.glVertex2f(x + w, y + h);
		GL11.glVertex2f(x + w, y + h - (h - f1) / 2);
		GL11.glVertex2f(x + w, y + h);
		GL11.glEnd();
		
	}
	
}
