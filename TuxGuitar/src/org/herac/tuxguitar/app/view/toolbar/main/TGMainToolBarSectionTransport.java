package org.herac.tuxguitar.app.view.toolbar.main;

import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.action.impl.caret.TGMoveToAction;
import org.herac.tuxguitar.app.action.impl.transport.*;
import org.herac.tuxguitar.app.system.icons.TGColorManager;
import org.herac.tuxguitar.app.system.icons.TGColorManager.TGSkinnableColor;
import org.herac.tuxguitar.app.transport.TGTransport;
import org.herac.tuxguitar.app.view.component.tab.Caret;
import org.herac.tuxguitar.app.view.component.tab.Tablature;
import org.herac.tuxguitar.app.view.component.tab.TablatureEditor;
import org.herac.tuxguitar.app.view.util.TGProcess;
import org.herac.tuxguitar.app.view.util.TGSyncProcessLocked;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.document.TGDocumentManager;
import org.herac.tuxguitar.editor.action.TGActionProcessor;
import org.herac.tuxguitar.editor.event.TGRedrawEvent;
import org.herac.tuxguitar.event.TGEvent;
import org.herac.tuxguitar.event.TGEventException;
import org.herac.tuxguitar.event.TGEventListener;
import org.herac.tuxguitar.player.base.MidiPlayer;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGMeasureHeader;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.ui.event.*;
import org.herac.tuxguitar.ui.layout.UITableLayout;
import org.herac.tuxguitar.ui.resource.*;
import org.herac.tuxguitar.ui.widget.UIButton;
import org.herac.tuxguitar.ui.widget.UICanvas;
import org.herac.tuxguitar.ui.widget.UIToggleButton;

public class TGMainToolBarSectionTransport extends TGMainToolBarSection implements TGEventListener {
	
	private static final int STATUS_STOPPED = 1;
	private static final int STATUS_PAUSED = 2;
	private static final int STATUS_RUNNING = 3;

	private static final int PLAY_MODE_DELAY = 250;

	private static final String COLOR_BACKGROUND = "widget.transport.backgroundColor";
	private static final String COLOR_FOREGROUND = "widget.transport.foregroundColor";

	private static final TGSkinnableColor[] SKINNABLE_COLORS = new TGSkinnableColor[] {
			new TGSkinnableColor(COLOR_BACKGROUND, new UIColorModel(0x10, 0x10, 0x10)),
			new TGSkinnableColor(COLOR_FOREGROUND, new UIColorModel(0xf0, 0xf0, 0xf0)),
	};

	final float FONT_SIZE = 14f;
	final float DISPLAY_MARGIN = 2f;

	private TGProcess redrawProcess;

	private UIButton first;
	private UIButton previous;
	private UIButton play;
	private UIButton next;
	private UIButton last;
	private UICanvas display;
	private UIToggleButton countDown;
	private UIToggleButton metronome;
	private UIToggleButton loop;
	private UIButton playMode;
	private int status;

	private UIFont displayFont;
	private UIColor backgroundColor;
	private UIColor foregroundColor;

	public TGMainToolBarSectionTransport(TGMainToolBar toolBar) {
		super(toolBar);
		this.redrawProcess = new TGSyncProcessLocked(getContext(), () -> display.redraw());
	}
	
	public void createSection() {
		this.first = this.createButton();
		this.first.addSelectionListener(event -> TGTransport.getInstance(getToolBar().getContext()).gotoFirst());

		this.previous = this.createButton();
		this.previous.addSelectionListener(event -> TGTransport.getInstance(getToolBar().getContext()).gotoPrevious());

		this.play = this.createButton();
		this.play.addSelectionListener(this.createActionProcessor(TGTransportPlayAction.NAME));

		this.next = this.createButton();
		this.next.addSelectionListener(event -> TGTransport.getInstance(getToolBar().getContext()).gotoNext());

		this.last = this.createButton();
		this.last.addSelectionListener(event -> TGTransport.getInstance(getToolBar().getContext()).gotoLast());


		this.displayFont = getFactory().createFont("Monospace", FONT_SIZE, false, false);
		TGColorManager tgColorManager = TGColorManager.getInstance(getToolBar().getContext());
		tgColorManager.appendSkinnableColors(SKINNABLE_COLORS);
		backgroundColor = tgColorManager.getColor(COLOR_BACKGROUND);
		foregroundColor = tgColorManager.getColor(COLOR_FOREGROUND);

		this.display = getFactory().createCanvas(getControl(), false);
		addItem(this.display);
		getLayout().set(this.display, 1, getItems().size(), UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, false, true, 1, 1, 100f, 18f, 0f);
		this.display.addMouseDownListener(new UIMouseDownListener() {
			public void onMouseDown(UIMouseEvent event) {
				if (event.getButton() == 1) {
					moveTransport(event.getPosition().getX());
				}
			}
		});
		this.display.addMouseMoveListener(new UIMouseMoveListener() {
			public void onMouseMove(UIMouseEvent event) {
				if ((event.getState() & UIMouseEvent.BUTTON1) != 0) {
					moveTransport(event.getPosition().getX());
				}
			}
		});
		this.display.addPaintListener(new UIPaintListener() {
			public void onPaint(UIPaintEvent event) {
				TGMainToolBarSectionTransport.this.paintDisplay(event.getPainter());
			}
		});
		this.display.addDisposeListener(new UIDisposeListener() {
			public void onDispose(UIDisposeEvent event) {
                displayFont.dispose();
                backgroundColor.dispose();
                foregroundColor.dispose();
			}
		});

		this.metronome = this.createToggleButton();
		this.metronome.addSelectionListener(this.createActionProcessor(TGTransportMetronomeAction.NAME));

		this.countDown = this.createToggleButton();
		this.countDown.addSelectionListener(this.createActionProcessor(TGTransportCountDownAction.NAME));

		this.loop = this.createToggleButton();
		this.loop.addSelectionListener(this.createActionProcessor(TGTransportSetLoopAction.NAME));

		this.playMode = this.createButton();
		this.playMode.addSelectionListener(this.createActionProcessor(TGOpenTransportModeDialogAction.NAME));

		this.status = STATUS_STOPPED;
		this.loadIcons();
		this.loadProperties();

		final TuxGuitar tg = TuxGuitar.getInstance();
		tg.getEditorManager().addRedrawListener(this);

		getControl().addDisposeListener(new UIDisposeListener() {
			public void onDispose(UIDisposeEvent event) {
				tg.getEditorManager().removeRedrawListener(TGMainToolBarSectionTransport.this);
				tg.updateCache(true);
			}
		});
	}

	private void moveTransport(float x) {
	    Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		final TGSongManager songManager = tablature.getSongManager();
		final TGDocumentManager documentManager = TGDocumentManager.getInstance(getContext());

		TGMeasureHeader first = songManager.getFirstMeasureHeader(documentManager.getSong());
		TGMeasureHeader last = songManager.getLastMeasureHeader(documentManager.getSong());
		long minimum = first.getStart();
		long maximum = last.getStart() + last.getLength() - 1;

		long position = Math.round(x / display.getBounds().getWidth() * ((double) (maximum - minimum)) + minimum);

		Caret caret = tablature.getCaret();
		TGTrack track = caret.getTrack();
		TGMeasure measure = tablature.getSongManager().getTrackManager().getMeasureAt(track, position);
		if (measure != null) {

			TGBeat beat = tablature.getSongManager().getMeasureManager().getBeatIn(measure, position);

			if (beat != null) {
				TGActionProcessor action = new TGActionProcessor(getContext(), TGMoveToAction.NAME);
				action.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, track);
				action.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, measure);
				action.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT, beat);
				action.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, caret.getSelectedString());
				action.process();
			}
		}
	}

	private void paintDisplay(UIPainter painter) {

		final MidiPlayer player = MidiPlayer.getInstance(getContext());
		final Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		final TGDocumentManager documentManager = TGDocumentManager.getInstance(getContext());
		final UISize size = this.display.getBounds().getSize();

		long position;
		if (player.isRunning()) {
			position = player.getTickPosition();
		} else {
			position = tablature.getCaret().getSelectedBeat().getStart();
		}

		TGMeasureHeader first = tablature.getSongManager().getFirstMeasureHeader(documentManager.getSong());
		TGMeasureHeader last = tablature.getSongManager().getLastMeasureHeader(documentManager.getSong());
		long minimum = first.getStart();
		long maximum = last.getStart() + last.getLength() - 1;
		float positionPercent = (position - minimum) / (float) (maximum - minimum);

		painter.setBackground(backgroundColor);

		painter.initPath(UIPainter.PATH_FILL);
		painter.addRectangle(0f, 0f, size.getWidth(), size.getHeight());
		painter.closePath();

		painter.setBackground(foregroundColor);
		painter.setAlpha(64);
		painter.initPath(UIPainter.PATH_FILL);
		painter.addRectangle(0, 0, size.getWidth() * positionPercent, size.getHeight());
		painter.closePath();
		painter.setAlpha(255);

		String time = Long.toString(position);
		painter.setFont(this.displayFont);
		painter.setForeground(foregroundColor);
		float realTextHeight = painter.getFMTopLine() - painter.getFMBaseLine();
		painter.drawString(time, DISPLAY_MARGIN, painter.getFMTopLine() + (size.getHeight() - realTextHeight) / 2f);

		float textWidth = painter.getFMWidth(time);
		float currentPackedWidth = getLayout().get(this.display, UITableLayout.PACKED_WIDTH, size.getWidth());
		float newPackedWidth = Math.max(currentPackedWidth, textWidth + DISPLAY_MARGIN * 2f);
		if (newPackedWidth != currentPackedWidth) {
			getLayout().set(this.display, UITableLayout.PACKED_WIDTH, newPackedWidth);
			getControl().layout();
		}
	}
	
	public void updateItems(){
		MidiPlayer player = MidiPlayer.getInstance(this.getToolBar().getContext());
		this.loop.setSelected(player.getMode().isLoop());
	    this.metronome.setSelected(player.isMetronomeEnabled());
		this.countDown.setSelected(player.getCountDown().isEnabled());
		this.loadIcons(false);
		this.display.redraw();
	}
	
	public void loadProperties(){
		this.first.setToolTipText(this.getText("transport.first"));
		this.previous.setToolTipText(this.getText("transport.previous"));
		this.play.setToolTipText(this.getText("transport.start"));
		this.next.setToolTipText(this.getText("transport.next"));
		this.last.setToolTipText(this.getText("transport.last"));
		this.metronome.setToolTipText(this.getText("transport.metronome"));
		this.countDown.setToolTipText(this.getText("transport.count-down"));
		this.loop.setToolTipText(this.getText("transport.simple.play-looped"));
		this.playMode.setToolTipText(this.getText("transport.mode"));
	}
	
	public void loadIcons(){
		this.loop.setImage(this.getIconManager().getTransportMode());
		this.metronome.setImage(this.getIconManager().getTransportMetronome());
		this.countDown.setImage(this.getIconManager().getTransportCountDown());
		this.playMode.setImage(this.getIconManager().getPlayMode());
		this.loadIcons(true);
	}
	
	public void loadIcons(boolean force){
		int lastStatus = this.status;
		
		MidiPlayer player = MidiPlayer.getInstance(this.getToolBar().getContext());
		if (player.isRunning()) {
			this.status = STATUS_RUNNING;
		} else if (player.isPaused()) {
			this.status = STATUS_PAUSED;
		} else {
			this.status = STATUS_STOPPED;
		}

		if (force || lastStatus != this.status) {
			if (this.status == STATUS_RUNNING) {
				this.first.setImage(this.getIconManager().getTransportIconFirst2());
				this.previous.setImage(this.getIconManager().getTransportIconPrevious2());
				this.next.setImage(this.getIconManager().getTransportIconNext2());
				this.last.setImage(this.getIconManager().getTransportIconLast2());
				this.play.setImage(this.getIconManager().getTransportIconPause());
				this.play.setToolTipText(this.getText("transport.pause"));
			} else if (this.status == STATUS_PAUSED) {
				this.first.setImage(this.getIconManager().getTransportIconFirst2());
				this.previous.setImage(this.getIconManager().getTransportIconPrevious2());
				this.next.setImage(this.getIconManager().getTransportIconNext2());
				this.last.setImage(this.getIconManager().getTransportIconLast2());
				this.play.setImage(this.getIconManager().getTransportIconPlay2());
				this.play.setToolTipText(this.getText("transport.start"));
			} else {
				this.first.setImage(this.getIconManager().getTransportIconFirst1());
				this.previous.setImage(this.getIconManager().getTransportIconPrevious1());
				this.next.setImage(this.getIconManager().getTransportIconNext1());
				this.last.setImage(this.getIconManager().getTransportIconLast1());
				this.play.setImage(this.getIconManager().getTransportIconPlay1());
				this.play.setToolTipText(this.getText("transport.start"));
			}
		}
	}

	@Override
	public void processEvent(TGEvent event) throws TGEventException {
		if (TGRedrawEvent.EVENT_TYPE.equals(event.getEventType())) {
			int type = event.getAttribute(TGRedrawEvent.PROPERTY_REDRAW_MODE);
			if( type == TGRedrawEvent.PLAYING_THREAD || type == TGRedrawEvent.PLAYING_NEW_BEAT ) {
				this.redrawProcess.process();
			}
		}
	}
}
