package de.m_marvin.logicsim.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

public class MTGLCanvas extends GLCanvas {
	
	public MTGLCanvas(Composite parent, int style, GLData data) {
		super(parent, style, data);
	}
	
	@Override
	protected void checkWidget() {
		this.getDisplay();
		if (this.isDisposed()) SWT.error(SWT.ERROR_WIDGET_DISPOSED);
	}
	
	public Point getSizePixels() {
		return DPIUtil.autoScaleUp(this.getSize());
	}
	
}
