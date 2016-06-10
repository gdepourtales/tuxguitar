package swtimpl.event;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.herac.tuxguitar.ui.event.UIPaintEvent;
import org.herac.tuxguitar.ui.event.UIPaintListenerManager;

import swtimpl.resource.SWTPainter;
import swtimpl.widget.SWTControl;

public class SWTPaintListenerManager extends UIPaintListenerManager implements PaintListener {
	
	private SWTControl<?> control;
	
	public SWTPaintListenerManager(SWTControl<?> control) {
		this.control = control;
	}
	
	public void paintControl(PaintEvent e) {
		this.onPaint(new UIPaintEvent(this.control, new SWTPainter(e.gc)));
	}
}
