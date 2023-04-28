package de.m_marvin.logicsim.ui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

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
import de.m_marvin.logicsim.logic.Circuit.NetState;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.NetConnector;
import de.m_marvin.logicsim.logic.nodes.InputNode;
import de.m_marvin.logicsim.logic.nodes.Node;
import de.m_marvin.logicsim.logic.nodes.OutputNode;
import de.m_marvin.logicsim.logic.nodes.PassivNode;
import de.m_marvin.logicsim.ui.TextRenderer;
import de.m_marvin.logicsim.util.Registries.ComponentEntry;
import de.m_marvin.univec.impl.Vec2i;
import de.m_marvin.univec.impl.Vec3i;

public class EditorArea extends Composite implements MouseListener, MouseMoveListener, KeyListener, MouseWheelListener {
	
	public static final String ERROR_ICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAAEEAAABACAYAAABFqxrgAAABZElEQVR4nO3ay27DMAxE0asi///LzqYEDLSBqQdJCZnZZSVldCw7jhqFua7run9urbWKefxUDLpbSpqHvwosFRokgSIJnxRYsjVIAgUSnhRYMjVIAskSvAosWRokgUQJvQosGRokgSQJowos0RokgQQJ3qfDyqdISSBYQu/qVmmQBAIljK5qhQZJIEjC7Gpma5AEAiSsWsVMDZLAYgk9q+d525ylQRJYWELEqs3eRbyRBBaVEHntZmiQBBaUkLGDR2uQBCZLyHyqi9QgCUyUUPG7P0qDJDBYQuWb4QgNksBACTucMlmtQRLoLGEHBd6xejRIAh0l7KTAO6ZXgyTgLGFHBd6xPRokAcfb5p0V3DMzT0ngQcIpCiyj833FTOc5Rxz1P00BjN8ptCfwoYQTFVhGNEgC/5RwsgJLr4ayu8NOZW5xjjAq3u+jPYGbhFX/9Z+W1lqTBH4lfKsCiySgEgCVAMAbcBDcc4s/x/EAAAAASUVORK5CYII=";
	
	public static final int RASTER_SIZE = 10;
	public static final int VISUAL_BUNDING_BOX_OFFSET = 5;
	public static final int SHOW_HIDEN_TAG_RANGE = 100;
	public static final int MIN_WARNING_DISTANCE = 100;
			
	public Circuit circuit;
	public Vec2i visualOffset = new Vec2i(0, 0);
	public Vec2i areaSize = new Vec2i(0, 0);
	
	protected Component hoveredComponent = null;
	protected Component grabedComponent = null;
	protected Vec2i grabOffset = new Vec2i(0, 0);
	protected Vec2i mousePosition = new Vec2i(0, 0);
	protected ComponentEntry activePlacement;
	protected boolean grabbedBackground = false;
	protected boolean allowEditing = true;
	protected long animationTimer;
	protected Supplier<Collection<SimulationWarning>> warningSupplier;
	
	protected GLData glData;
	protected GLCanvas glCanvas;
	protected GLCapabilities glCapabilities;
	protected boolean resized;
	protected boolean initialized = false;
	
	protected Slider sliderHorizontal;
	protected Slider sliderVertical;

	public static class SimulationWarning {
		public Component component;
		public String message;
		public Supplier<Boolean> stillValid; 
		public long decayTime;
		
		public SimulationWarning(Component component, String message, Supplier<Boolean> stillValid, long decayTime) {
			this.component = component;
			this.message = message;
			this.stillValid = stillValid;
			this.decayTime = decayTime;
		}
	}

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
	
	public void setWarningSupplier(Supplier<Collection<SimulationWarning>> warningSupplier) {
		this.warningSupplier = warningSupplier;
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
				if (this.hoveredComponent != null) {
					if (event.button == 1) {
						if (event.count == 1) {
							this.setGrabbedComponent(this.hoveredComponent);
						} else {
							this.hoveredComponent.click(mousePosition, true);
						}
					} else {
						this.hoveredComponent.click(mousePosition, false);
					}
					return;
				}
			}

			this.grabOffset = this.mousePosition;
			this.grabbedBackground = true;
			
		} else if (event.button == 1) {
			this.activePlacement.placementClickMethod().accept(circuit, this.mousePosition.sub(this.visualOffset));
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
			boolean redraw = this.activePlacement.placementMoveMethod().apply(circuit, mousePosition.sub(this.visualOffset));
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

		this.hoveredComponent = null;
		for (Component component : this.circuit.getComponents()) {
			Vec2i pos = this.mousePosition.sub(this.visualOffset);
			if (component.isInBounds(pos)) {
				this.hoveredComponent = component;
				break;
			}
		}
		
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent event) {}

	@Override
	public void mouseScrolled(MouseEvent e) {
		
		int scroll = e.count;
		boolean shiftDown = (e.stateMask & SWT.SHIFT) > 0;
		Vec2i scrollVec = shiftDown ? new Vec2i(scroll, 0) : new Vec2i(0, scroll);
		scrollView(scrollVec.mul(10));
		
	}
	
	@Override
	public void keyPressed(KeyEvent event) {

		if (!this.isAllowedEditing()) return;
		if (this.circuit == null) return;
		
		if (event.keyCode == SWT.DEL) {
			if (this.grabedComponent != null) {
				this.removeUnplacedComponent();
			} else {
				if (this.hoveredComponent != null) {
					this.circuit.remove(this.hoveredComponent);
					this.circuit.reconnect(true, this.hoveredComponent);
					this.hoveredComponent = null;
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

				for (InputNode inputNode : component.getInputs()) {
					boolean mouseNearBy = this.mousePosition.dist(inputNode.getVisualPosition().add(visualOffset)) < SHOW_HIDEN_TAG_RANGE;
					boolean connected = circuit.isNodeConnected(inputNode);
					Vec2i position = inputNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 1, inputNode.getLaneTag(), inputNode.getLabel(), connected, mouseNearBy);
				}
				for (OutputNode outputNode : component.getOutputs()) {
					boolean mouseNearBy = this.mousePosition.dist(outputNode.getVisualPosition().add(visualOffset)) < SHOW_HIDEN_TAG_RANGE;
					boolean connected = circuit.isNodeConnected(outputNode);
					Vec2i position = outputNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 2, outputNode.getLaneTag(), outputNode.getLabel(), connected, mouseNearBy);
				}
				for (PassivNode passivNode : component.getPassives()) {
					boolean mouseNearBy = this.mousePosition.dist(passivNode.getVisualPosition().add(visualOffset)) < SHOW_HIDEN_TAG_RANGE;
					boolean connected = circuit.isNodeConnected(passivNode);
					Vec2i position = passivNode.getVisualOffset().add(component.getVisualPosition());
					drawNode(position, 3, passivNode.getLaneTag(), "", connected, mouseNearBy);
				}
				
			}
			
		});
		
		if (warningSupplier != null) {
			
			if (System.currentTimeMillis() - animationTimer > 1000) {
				animationTimer = System.currentTimeMillis();
			}
			
			if (System.currentTimeMillis() - animationTimer > 500) {
				
				List<Vec2i> drawnWarnings = new ArrayList<>();
				warningSupplier.get().forEach(warning -> {
					
					Vec2i position = warning.component.getVisualPosition().add(new Vec2i(warning.component.getVisualWidth() / 2, warning.component.getVisualHeight() / 2));
					
					for (Vec2i dw : drawnWarnings) {
						if (dw.dist(position) < MIN_WARNING_DISTANCE) return;
					}
					drawnWarnings.add(position);
					
					drawWarning(position, warning);
					
				});
				
			}
			
		}
		
		if (this.grabedComponent != null) {
			
			Vec2i topLeft = this.mousePosition.sub(grabOffset).sub(VISUAL_BUNDING_BOX_OFFSET, VISUAL_BUNDING_BOX_OFFSET);
			int width = this.grabedComponent.getVisualWidth() + VISUAL_BUNDING_BOX_OFFSET * 2;
			int height = this.grabedComponent.getVisualHeight() + VISUAL_BUNDING_BOX_OFFSET * 2;
			swapColor(0, 1, 0, 0.4F);
			drawRectangle(1, topLeft.x , topLeft.y, width, height);
			
		}
		
		GL11.glPopMatrix();

		if (this.hoveredComponent instanceof NetConnector connector) {
			
			Map<String, NetState> laneData = connector.getLaneData();
			if (laneData != null) drawLaneInfo(this.mousePosition, laneData);
			
		}
		
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
	
	public static void drawLaneInfo(Vec2i position, Map<String, NetState> laneData) {
		
		int x = 0;
		int y = 0;
		
		Map<String, Long> busData = Circuit.getLaneData(laneData);
		SortedSet<String> buses = new TreeSet<>(busData.keySet());
		SortedSet<String> lanes = new TreeSet<>(laneData.keySet());

		swapColor(0, 0, 0, 1);
		drawFilledRectangle(position.x + 50, position.y - 50, 320, laneData.size() * 20);
		swapColor(1, 1, 1, 1);
		drawLine(2, position.x, position.y, position.x + 50, position.y - 50);
		drawRectangle(1, position.x + 50, position.y - 50, 320, laneData.size() * 20);
		
		boolean b = false;
		for (String lane : lanes) {
			NetState state = laneData.get(lane);
			if (state.getLogicState()) {
				swapColor(0, 0, 1, 1);
			} else {
				swapColor(0, 1, 1, 1);
			}
			TextRenderer.drawText(position.x + 55 + x, position.y - 40 + y, 14, lane, TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
			//TextRenderer.drawText(position.x + 50 + x + 60, position.y + 50 + y, 14, state.toString(), TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
			y += 20;
		}
		
		x = 60;
		int y1 = 0;
		
		for (String bus : buses) {
			long value = busData.get(bus);
			
			swapColor(1F, 0.4F, 0, 1);
			TextRenderer.drawText(position.x + 55 + x, position.y - 40 + y1, 14, bus, TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
			if (Long.toUnsignedString(value).length() > 4) {
				TextRenderer.drawText(position.x + 75 + x + 70, position.y - 40 + y1, 14, Long.toUnsignedString(value, 16), TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
			} else {
				TextRenderer.drawText(position.x + 75 + x + 40, position.y - 40 + y1, 14, Long.toUnsignedString(value, 16), TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
				TextRenderer.drawText(position.x + 75 + x + 130, position.y - 40 + y1, 14, Long.toUnsignedString(value), TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
			}
			b = !b;
			swapColor(1F, 0.4F, 0, 0.6F);
			drawLine(1, position.x + 45 + x, position.y - 35 + y1, position.x + 275 + x, position.y - 35 + y1);
			
			y1 += 20;
		}
		
	}
	
	public static void drawWarning(Vec2i position, SimulationWarning warning) {
		
		swapColor(1, 0, 0, 1);
		drawWarningSign(position, 0.5F);
		
		TextRenderer.drawText(position.x, position.y + 35, 12, warning.message);
		
	}
	
	public static void drawWarningSign(Vec2i position, float size) {

		GL11.glPushMatrix();
		GL11.glTranslated(position.x, position.y, 0);
		GL11.glScalef(size, size, 1);
		GL11.glBegin(GL11.GL_QUADS);
		
		Vec2i cu = new Vec2i(0, -40);
		Vec2i cl = new Vec2i(-40, 40);
		Vec2i cr = new Vec2i(40, 40);
		Vec3i w = new Vec3i(15, 9, 20);
		
		GL11.glVertex2d(cu.x, cu.y);
		GL11.glVertex2d(cr.x, cr.y);
		GL11.glVertex2d(cr.x - w.x, cr.y - w.y);
		GL11.glVertex2d(cu.x, cu.y + w.z);
		
		GL11.glVertex2d(cu.x, cu.y);
		GL11.glVertex2d(cl.x, cl.y);
		GL11.glVertex2d(cl.x + w.x, cl.y - w.y);
		GL11.glVertex2d(cu.x, cu.y + w.z);
		
		GL11.glVertex2d(cl.x + w.x, cl.y - w.y);
		GL11.glVertex2d(cr.x - w.x, cr.y - w.y);
		GL11.glVertex2d(cr.x, cr.y);
		GL11.glVertex2d(cl.x, cl.y);
		
		int yo = -6;
		int tw = 4;
		int th1 = 17;
		
		GL11.glVertex2d(-tw, yo);
		GL11.glVertex2d(tw, yo);
		GL11.glVertex2d(tw, yo + th1);
		GL11.glVertex2d(-tw, yo + th1);
		
		GL11.glVertex2d(-tw, yo + th1 + tw * 2);
		GL11.glVertex2d(tw, yo + th1 + tw * 2);
		GL11.glVertex2d(tw, yo + th1 + tw * 4);
		GL11.glVertex2d(-tw, yo + th1 + tw * 4);
		
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
	}
	
	public static void drawNode(Vec2i position, int type, String laneTag, String label, boolean connected, boolean mouseNearBy) {
		swapColor(1, 1, 1, 1);
		switch (type) {
		case 1:
			drawCircle(1, position.x, position.y, 5);
			drawPoint(4, position.x, position.y);
			if (!label.isEmpty() && (!connected || mouseNearBy)) TextRenderer.drawText(position.x - 10, position.y, 12, label, TextRenderer.ORIGIN_RIGHT | TextRenderer.RESIZED);
			break;
		case 2:
			drawCircle(1, position.x, position.y, 5);
			drawLine(1, position.x - 5, position.y - 5, position.x + 5, position.y + 5);
			drawLine(1, position.x - 5, position.y + 5, position.x + 5, position.y - 5);
			if (!label.isEmpty() && (!connected || mouseNearBy)) TextRenderer.drawText(position.x + 10, position.y, 12, label, TextRenderer.ORIGIN_LEFT | TextRenderer.RESIZED);
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

	public static void drawFilledRectangle(float x, float y, float w, float h) {
		GL11.glBegin(GL11.GL_QUADS);
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
