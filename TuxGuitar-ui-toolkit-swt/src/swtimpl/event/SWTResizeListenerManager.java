package swtimpl.event;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.herac.tuxguitar.ui.event.UIResizeEvent;
import org.herac.tuxguitar.ui.event.UIResizeListenerManager;

import swtimpl.SWTComponent;

public class SWTResizeListenerManager extends UIResizeListenerManager implements Listener {
	
	private SWTComponent<?> control;
	
	public SWTResizeListenerManager(SWTComponent<?> control) {
		this.control = control;
	}
	
	public void handleEvent(Event e) {
		this.onResize(new UIResizeEvent(this.control));
	}
}
