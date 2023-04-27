package de.m_marvin.logicsim.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Slider;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.util.Registries.ComponentEntry;
import de.m_marvin.univec.impl.Vec2i;

public class EditorArea extends Composite implements MouseListener, MouseMoveListener, KeyListener, MouseWheelListener {
	
	public static final int RASTER_SIZE = 10;
	public static final int VISUAL_BUNDING_BOX_OFFSET = 5;
		
	public Circuit circuit;
	public Vec2i visualOffset = new Vec2i(0, 0);
	public Vec2i areaSize = new Vec2i(0, 0);
	
	protected Component grabedComponent = null;
	protected Vec2i grabOffset = new Vec2i(0, 0);
	protected Vec2i mousePosition = new Vec2i(0, 0);
	protected ComponentEntry activePlacement;
	protected boolean grabbedBackground = false;
	protected boolean allowEditing = true;
	
	protected GLData glData;
	protected GLCanvas glCanvas;
	protected GLCapabilities glCapabilities;
	protected boolean resized;
	protected boolean initialized = false;
	
	protected Slider sliderHorizontal;
	protected Slider sliderVertical;
	
	public EditorArea(Composite parent) {
		this(parent, true);
	}
	
	public EditorArea(Composite parent, boolean sliders) {
		super(parent, SWT.NONE);
		this.setLayout(new BorderLayout());
		this.glData = new GLData();
		this.glData.doubleBuffer = true;
		this.glCanvas = new MTGLCanvas(this, SWT.None, glData);
		this.glCanvas.setLayoutData(new BorderData(SWT.CENTER));
		this.glCanvas.addListener(SWT.Resize, (event) -> this.resized = true);
		this.glCanvas.addMouseListener(this);
		this.glCanvas.addMouseMoveListener(this);
		this.glCanvas.addMouseWheelListener(this);
		this.glCanvas.addKeyListener(this);
		if (sliders) {
			this.sliderHorizontal = new Slider(this, SWT.NONE);
			this.sliderHorizontal.setLayoutData(new BorderData(SWT.BOTTOM));
			this.sliderHorizontal.setMaximum(1);
			this.sliderHorizontal.addListener(SWT.Selection, (e) -> {
				this.visualOffset.setX(-this.sliderHorizontal.getSelection());
			});
			this.sliderVertical = new Slider(this, SWT.VERTICAL);
			this.sliderVertical.setLayoutData(new BorderData(SWT.RIGHT));
			this.sliderVertical.setMaximum(1);
			this.sliderVertical.addListener(SWT.Selection, (e) -> {
				this.visualOffset.setY(-this.sliderVertical.getSelection());
			});
			this.glCanvas.addListener(SWT.Resize, (event) -> Display.getDefault().asyncExec(() -> resizeArea()));
		}
	}
	
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		resizeArea();
	}
	
	public void setAreaSize(Vec2i size) {
		this.areaSize = size;
		resizeArea();
	}
	
	public Vec2i getVisibleArea() {
		return this.glCanvas.isDisposed() ? new Vec2i() : Vec2i.fromVec(this.glCanvas.getSize());
	}
	
	public void setAllowEditing(boolean allowEditing) {
		this.allowEditing = allowEditing;
	}
	
	public boolean isAllowedEditing() {
		return allowEditing;
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

	protected void resizeArea() {
		Vec2i screenSize = getVisibleArea();
		if (screenSize.x == 0 || screenSize.y == 0) return;
		Vec2i scrollableArea = this.areaSize.sub(this.getVisibleArea()).max(1);
		this.sliderVertical.setMaximum(scrollableArea.y);
		this.sliderHorizontal.setMaximum(scrollableArea.x);
		this.visualOffset.clampI(scrollableArea.mul(-1).add(1, 1), new Vec2i(0, 0));
		if (scrollableArea.x > 1) this.sliderHorizontal.setSelection(-this.visualOffset.x);
		if (scrollableArea.y > 1) this.sliderVertical.setSelection(-this.visualOffset.y);
	}
	
	public void scrollView(Vec2i scrollVec) {
		Vec2i scrollableArea = this.areaSize.sub(this.getVisibleArea()).max(1);
		this.visualOffset = this.visualOffset.add(scrollVec).clamp(scrollableArea.mul(-1).add(1, 1), new Vec2i(0, 0));
		if (scrollableArea.x > 1 && this.sliderHorizontal != null) this.sliderHorizontal.setSelection(-this.visualOffset.x);
		if (scrollableArea.y > 1 && this.sliderVertical != null) this.sliderVertical.setSelection(-this.visualOffset.y);
	}
	
	@Override
	public void mouseUp(MouseEvent event) {

		if (!this.isAllowedEditing()) return;
		if (this.circuit == null) return;
		
		if (event.button == 1) {
			if (this.grabedComponent != null) {
				this.releaseGrabbedComponent();
			}
		}
		
		this.grabOffset = new Vec2i();
		this.grabbedBackground = false;
		
	}
	
	@Override
	public void mouseDown(MouseEvent event) {

		if (!this.isAllowedEditing()) return;
		if (this.circuit == null) return;
		
		if (this.activePlacement == null) {
			
			if (this.grabedComponent == null) {
				for (Component component : this.circuit.getComponents()) {
					for (Node node : component.getAllNodes()) {
						if (node.getVisualPosition().add(this.visualOffset).equals(mousePosition)) {
							Vec2i location = Vec2i.fromVec(event.display.getCursorLocation());
							if (node.click(location)) return;
						}
					}
				}
				for (Component component : this.circuit.getComponents()) {
					Vec2i visualPos = component.getVisualPosition().add(this.visualOffset);
					if (visualPos.x <= this.mousePosition.x &&
						visualPos.x + component.getVisualWidth() >= this.mousePosition.x &&
						visualPos.y <= this.mousePosition.y &&
						visualPos.y + component.getVisualHeight() >= this.mousePosition.y) {
						
						if (event.button == 1) {
							this.setGrabbedComponent(component);
						} else {
							component.click(mousePosition);
						}
						return;			
					}
				}
			}

			this.grabOffset = this.mousePosition;
			this.grabbedBackground = true;
			
		} else if (event.button == 1) {
			this.activePlacement.placementClickMethod().accept(circuit, this.mousePosition);
		}
		
	}

	@Override
	public void mouseMove(MouseEvent event) {

		if (!this.isAllowedEditing()) return;
		if (this.circuit == null) return;

		Vec2i screenSize = getVisibleArea();
		
		this.mousePosition = new Vec2i(event.x, event.y).clamp(new Vec2i(0, 0), Vec2i.fromVec(screenSize));
		Vec2i rasterOffset = this.mousePosition.add(RASTER_SIZE / 2, RASTER_SIZE / 2).module(RASTER_SIZE).sub(RASTER_SIZE / 2, RASTER_SIZE / 2);
		this.mousePosition.subI(rasterOffset);
		
		if (this.activePlacement != null) {
			boolean redraw = this.activePlacement.placementMoveMethod().apply(circuit, mousePosition);
			if (redraw) this.redraw();
		}
		
		if (this.grabedComponent != null) {
			this.redraw();
		}
		
		if (this.grabbedBackground) {
			
			Vec2i scrollVec = this.mousePosition.sub(this.grabOffset);
			this.grabOffset = this.mousePosition;
			scrollView(scrollVec);
			
		}
		
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent event) {}

	@Override
	public void mouseScrolled(MouseEvent e) {
		
		int scroll = e.count;
		boolean shiftDown = (e.stateMask & SWT.SHIFT) > 0;
		Vec2i scrollVec = shiftDown ? new Vec2i(scroll, 0) : new Vec2i(0, scroll);
		scrollView(scrollVec);
		
	}
	
	@Override
	public void keyPressed(KeyEvent event) {

		if (!this.isAllowedEditing()) return;
		if (this.circuit == null) return;
		
		if (event.keyCode == SWT.DEL) {
			if (this.grabedComponent != null) {
				this.removeUnplacedComponent();
			} else {
				for (Component component : this.circuit.getComponents()) {
					Vec2i visualPos = component.getVisualPosition().add(this.visualOffset);
					if (visualPos.x <= this.mousePosition.x &&
						visualPos.x + component.getVisualWidth() >= this.mousePosition.x &&
						visualPos.y <= this.mousePosition.y &&
						visualPos.y + component.getVisualHeight() >= this.mousePosition.y) {
						
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
	
	protected void initOpenGL() {
		if (this.glCanvas.isDisposed()) return;
		if (!this.glCanvas.isDisposed()) this.glCanvas.setCurrent();
		this.glCapabilities = GL.createCapabilities();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        this.glCanvas.addDisposeListener((e) -> TextRenderer.cleanUpOpenGL());
	    this.resized = true;
	    this.initialized = true;
	}
	
	public void render() {
		
		if (this.isDisposed() || this.glCanvas == null || this.circuit == null) return;
		
		if (!initialized) {
			initOpenGL();
		}
		
		if (!this.glCanvas.isDisposed()) this.glCanvas.setCurrent();
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		
		if (resized) {
			GL11.glViewport(0, 0, getVisibleArea().x, getVisibleArea().y);
			GL11.glLoadIdentity();
			GL11.glOrtho(0.0, getVisibleArea().x, getVisibleArea().y, 0.0, 0.0, 1.0);
			GL11.glClearColor(0, 0, 0, 1);
			this.resized = false;
		}
		
		drawRaster();

		swapColor(1, 1, 1, 1);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(this.visualOffset.x, this.visualOffset.y, 0);
		
		circuit.getComponents().forEach(component -> {
			
			if (isComponentVisible(component)) {
				
				component.render();
				
				swapColor(1, 1, 1, 1);
				
				component.getInputs().forEach(inputNode -> {
					Vec2i position = inputNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 1, inputNode.getLaneTag());
				});
				component.getOutputs().forEach(outputNode -> {
					Vec2i position = outputNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 2, outputNode.getLaneTag());
				});
				component.getPassives().forEach(passivNode -> {
					Vec2i position = passivNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 3, passivNode.getLaneTag());
				});
				
			}
			
		});
		
		if (this.grabedComponent != null) {
			
			Vec2i topLeft = this.mousePosition.sub(grabOffset).sub(VISUAL_BUNDING_BOX_OFFSET, VISUAL_BUNDING_BOX_OFFSET);
			int width = this.grabedComponent.getVisualWidth() + VISUAL_BUNDING_BOX_OFFSET * 2;
			int height = this.grabedComponent.getVisualHeight() + VISUAL_BUNDING_BOX_OFFSET * 2;
			
			swapColor(0, 1, 0, 0.4F);
			drawRectangle(1, topLeft.x , topLeft.y, width, height);
			
		}

		GL11.glPopMatrix();
		
		if (!this.glCanvas.isDisposed()) this.glCanvas.swapBuffers();
		
	}
	
	public boolean isComponentVisible(Component component) {
		
		Vec2i position = component.getVisualPosition();		
		Vec2i size = new Vec2i(component.getVisualWidth(), component.getVisualHeight());
		Vec2i position2 = position.add(size).add(VISUAL_BUNDING_BOX_OFFSET, VISUAL_BUNDING_BOX_OFFSET);
		Vec2i position1 = position.sub(VISUAL_BUNDING_BOX_OFFSET, VISUAL_BUNDING_BOX_OFFSET);
		
		Vec2i area1 = this.visualOffset.mul(-1);
		Vec2i area2 = area1.add(this.getVisibleArea());
		
		return
				(
					(position1.x >= area1.x && position1.x <= area2.x)
					||
					(position2.x >= area1.x && position2.x <= area2.x)
					||
					(position1.x < area1.x && position2.x > area2.x)
				)
				&&
				(
					(position1.y >= area1.y && position1.y <= area2.y)
					||
					(position2.y >= area1.y && position2.y <= area2.y)
					||
					(position1.y < area1.y && position2.y > area2.y)
				);
		
	}
	
	public static void drawNode(Vec2i position, int type, String laneTag) {
		swapColor(1, 1, 1, 1);
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
		if (!laneTag.equals(Circuit.DEFAULT_BUS_LANE)) {
			
			int i1 = type == 1 ? -1 : 1;
			int i2 = type == 1 ? TextRenderer.ORIGIN_RIGHT : TextRenderer.ORIGIN_LEFT;
			int tagLength = TextRenderer.drawText(position.x + 10 * i1, position.y - 10, 13, laneTag, i2 | TextRenderer.ORIGIN_BOTTOM | TextRenderer.RESIZED);
			
			swapColor(1F, 0.4F, 0, 1);
			drawLine(1, position.x + 10 * i1, position.y - 9, (int) (position.x + (10 + tagLength / 1.5) * i1), position.y - 9);
			drawLine(1, position.x, position.y, position.x + 10 * i1, position.y - 9);
		}
	}
	
	public void drawRaster() {
		
		int rasterSize1 = 10;
		int rasterSizePixels = RASTER_SIZE * rasterSize1;
		Vec2i rasterOffset = this.visualOffset.module(rasterSizePixels).add(rasterSizePixels, rasterSizePixels);
		Vec2i screenSize = getVisibleArea();
		
		swapColor(1.0F, 0.5F, 0.0F, 0.4F);
		
		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < screenSize.x; i+= rasterSizePixels) {
			GL11.glVertex2f(i + rasterOffset.x, 0);
			GL11.glVertex2f(i + rasterOffset.x, screenSize.y);
		}
		for (int j = 0; j < screenSize.y; j += rasterSizePixels) {
			GL11.glVertex2f(0, j + rasterOffset.y);
			GL11.glVertex2f(screenSize.x, j + rasterOffset.y);
		}
		GL11.glEnd();
		
		for (int i = 0; i < screenSize.x; i+= rasterSizePixels) {
			for (int j = 0; j < screenSize.y; j += rasterSizePixels) {
				drawRectangle(1, i + rasterOffset.x - 5, j + rasterOffset.y - 5, 10, 10);
			}
		}
		
		swapColor(1, 1, 1, 0.5F);
		
		GL11.glPointSize(1);
		GL11.glBegin(GL11.GL_POINTS);
		for (int i = -rasterSizePixels; i < screenSize.x; i += RASTER_SIZE) {
			for (int j = -rasterSizePixels; j < screenSize.y; j += RASTER_SIZE) {
				if (i % rasterSizePixels != 0 && j % rasterSizePixels != 0) GL11.glVertex2f(i + rasterOffset.x, j + rasterOffset.y);
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
