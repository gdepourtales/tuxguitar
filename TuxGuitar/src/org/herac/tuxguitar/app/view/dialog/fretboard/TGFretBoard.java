package org.herac.tuxguitar.app.view.dialog.fretboard;

import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.action.TGActionProcessorListener;
import org.herac.tuxguitar.app.action.impl.caret.TGGoRightAction;
import org.herac.tuxguitar.app.action.impl.caret.TGMoveToAction;
import org.herac.tuxguitar.app.action.impl.tools.TGOpenScaleDialogAction;
import org.herac.tuxguitar.app.action.impl.tools.TGOpenScaleFinderDialogAction;
import org.herac.tuxguitar.app.system.config.TGConfigKeys;
import org.herac.tuxguitar.app.system.icons.TGIconManager;
import org.herac.tuxguitar.app.tools.scale.ScaleInfo;
import org.herac.tuxguitar.app.ui.TGApplication;
import org.herac.tuxguitar.app.util.TGMusicKeyUtils;
import org.herac.tuxguitar.app.view.component.docked.TGDockedPlayingComponent;
import org.herac.tuxguitar.app.view.main.TGWindow;
import org.herac.tuxguitar.app.view.util.TGBufferedPainterListenerLocked;
import org.herac.tuxguitar.app.view.util.TGBufferedPainterLocked.TGBufferedPainterHandle;
import org.herac.tuxguitar.app.view.widgets.TGNoteToolbar;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.TGEditorManager;
import org.herac.tuxguitar.editor.action.TGActionProcessor;
import org.herac.tuxguitar.editor.action.note.TGChangeNoteAction;
import org.herac.tuxguitar.editor.action.note.TGDeleteNoteAction;
import org.herac.tuxguitar.song.models.*;
import org.herac.tuxguitar.ui.UIFactory;
import org.herac.tuxguitar.ui.event.UIMouseEvent;
import org.herac.tuxguitar.ui.event.UIMouseUpListener;
import org.herac.tuxguitar.ui.event.UISelectionEvent;
import org.herac.tuxguitar.ui.event.UISelectionListener;
import org.herac.tuxguitar.ui.layout.UITableLayout;
import org.herac.tuxguitar.ui.resource.*;
import org.herac.tuxguitar.ui.widget.*;
import org.herac.tuxguitar.util.TGContext;

import java.util.Iterator;

public class TGFretBoard extends TGDockedPlayingComponent {
	
	public static final int TOP_SPACING = 10;
	public static final int BOTTOM_SPACING = 10;
	
	private static final int STRING_SPACING = TuxGuitar.getInstance().getConfig().getIntegerValue(TGConfigKeys.FRETBOARD_STRING_SPACING);
	private static final String[] NOTE_NAMES = TGMusicKeyUtils.getSharpKeyNames(TGMusicKeyUtils.PREFIX_FRETBOARD);
	
	private TGContext context;
	private TGFretBoardConfig config;
	private TGNoteToolbar toolbar;
	private UILabel scaleName;
	private UIButton scale;
	private UIButton scaleFinder;
	private UIImage image;
	
	private TGBeat beat;
	private TGBeat externalBeat;
	
	private int[] frets;
	private int[] strings;
	private float fretSpacing;
	private boolean changes;
	private UISize lastSize;
	protected UIDropDownSelect<Integer> handSelector;
	protected UICanvas canvas;
	private UIFont scaledFont;

	public TGFretBoard(TGContext context, UIContainer parent) {
		this.context = context;
		this.config = new TGFretBoardConfig(context);
		this.config.load();
		this.control = getUIFactory().createPanel(parent, false);
		
		this.initToolBar();
		this.initEditor();
		this.createControlLayout();
		this.loadIcons();
		this.loadProperties();
		
		TuxGuitar.getInstance().getKeyBindingManager().appendListenersTo(this.canvas);
	}
	
	public void createControlLayout() {
		UITableLayout uiLayout = new UITableLayout(0f);
		uiLayout.set(this.toolbar.getControl(), 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		uiLayout.set(this.canvas, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, null, 1f, 0f);

		this.control.setLayout(uiLayout);
	}

	private void initToolBar() {
		UIFactory factory = getUIFactory();
		this.toolbar =  new TGNoteToolbar(context, factory, this.control);

		// separator
		this.toolbar.createLeftSeparator(factory);

		// hand selector
		this.handSelector = factory.createDropDownSelect(this.toolbar.getLeftComposite());
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.right-mode"), TGFretBoardConfig.DIRECTION_RIGHT));
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.left-mode"), TGFretBoardConfig.DIRECTION_LEFT));
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, this.getDirection(this.config.getDirection())));
		this.handSelector.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				Integer direction = TGFretBoard.this.handSelector.getSelectedValue();
				if( direction != null ) {
					updateDirection(direction);
				}
			}
		});
		this.toolbar.setLeftControlLayout(this.handSelector);

		// separator
		this.toolbar.createLeftSeparator(factory);

		// scale
		this.scale = factory.createButton(this.toolbar.getLeftComposite());
		this.scale.addSelectionListener(new TGActionProcessorListener(this.context, TGOpenScaleDialogAction.NAME));
		this.toolbar.setLeftControlLayout(this.scale);

		// finder
		this.scaleFinder = factory.createButton(this.toolbar.getLeftComposite());
		this.scaleFinder.addSelectionListener(new TGActionProcessorListener(this.context, TGOpenScaleFinderDialogAction.NAME));
		this.toolbar.setLeftControlLayout(this.scaleFinder);

		// scale name
		this.scaleName = factory.createLabel(this.toolbar.getLeftComposite());
		this.toolbar.setLeftControlLayout(this.scaleName);

		this.toolbar.getSettings().addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				configure();
			}
		});
	}

	private void initEditor() {
		this.lastSize = new UISize();
		this.canvas = getUIFactory().createCanvas(this.control, false);
		this.canvas.setBgColor(this.config.getColorBackground());
		this.canvas.addMouseUpListener(new TGFretBoardMouseListener());
		this.canvas.addPaintListener(new TGBufferedPainterListenerLocked(this.context, new TGFretBoardPainterListener()));
	}

	private void loadScaleName() {
		int scaleKey = TuxGuitar.getInstance().getScaleManager().getSelectionKey();
		ScaleInfo info = TuxGuitar.getInstance().getScaleManager().getSelection();
		String key = TuxGuitar.getInstance().getScaleManager().getKeyName( scaleKey );
		this.scaleName.setText( ( key != null && info != null ) ? ( key + " - " + info.getName() ) : "" );
	}
	
	private void calculateFretSpacing(float width) {
		if (getTrack() == null) {
			return;
		}
		int maxFrets = getTrack().getFrets() + 1;
		this.fretSpacing = (width / maxFrets);
		int aux = 0;
		for (int i = 0; i < maxFrets; i++) {
			aux += (i * 2);
		}
		this.fretSpacing += (aux / maxFrets) + 2;
	}
	
	private void disposeFretBoardImage(){
		if( this.image != null && !this.image.isDisposed() ){
			this.image.dispose();
		}
	}
	
	protected void initFrets(int fromX, int fretCount) {
		this.frets = new int[fretCount + 1];
		int nextX = fromX;
		int direction = this.getDirection(this.config.getDirection());
		if (direction == TGFretBoardConfig.DIRECTION_RIGHT) {
			for (int i = 0; i < this.frets.length; i++) {
				this.frets[i] = nextX;
				nextX += (this.fretSpacing - ((i + 1) * 2));
			}
		} else if (direction == TGFretBoardConfig.DIRECTION_LEFT) {
			for (int i = this.frets.length - 1; i >= 0; i--) {
				this.frets[i] = nextX;
				nextX += (this.fretSpacing - (i * 2));
			}
		}
	}
	
	private int getDirection( int value ){
		int direction = value;
		if( direction != TGFretBoardConfig.DIRECTION_RIGHT && direction != TGFretBoardConfig.DIRECTION_LEFT ){
			direction = TGFretBoardConfig.DIRECTION_RIGHT;
		}
		return direction;
	}
	
	private void initStrings(int count) {
		int fromY = TOP_SPACING;
		this.strings = new int[count];
		
		for (int i = 0; i < this.strings.length; i++) {
			this.strings[i] = fromY + (STRING_SPACING * i);
		}
	}
	
	private void updateEditor(){
		if( isVisible() ){
			if(TuxGuitar.getInstance().getPlayer().isRunning()){
				this.beat = TuxGuitar.getInstance().getEditorCache().getPlayBeat();
			}else if(this.externalBeat != null){
				this.beat = this.externalBeat;
			}else{
				this.beat = TuxGuitar.getInstance().getEditorCache().getEditBeat();
			}

			int fretCount = getFretCount();
			int stringCount = getStringCount();
			if (this.frets.length != fretCount || this.strings.length != stringCount) {
				disposeFretBoardImage();

				if (this.frets.length != fretCount) {
					this.initFrets(10, fretCount);
					this.lastSize.setWidth(0);
				}
				if (this.strings.length != stringCount) {
					initStrings(stringCount);
					this.lastSize.setHeight(0);
                }
				//Fuerzo a cambiar el ancho
			}

			UIRectangle childArea = this.control.getChildArea();
			float clientWidth = childArea.getWidth();
			float clientHeight = childArea.getHeight();
			
			if( this.lastSize.getWidth() != clientWidth || hasChanges() ){
				this.layout(clientWidth);
			}
			
			if( this.lastSize.getHeight() != clientHeight ) {
				TuxGuitar.getInstance().getFretBoardEditor().showComponent();
			}
			this.lastSize.setWidth(clientWidth);
			this.lastSize.setHeight(clientHeight);
		}
	}

	private void paintFretBoard(UIPainter painter, float zoom){
		if(this.image == null || this.image.isDisposed()){
			UIFactory factory = getUIFactory();
			UIRectangle area = this.control.getChildArea();

			this.image = factory.createImage(area.getWidth() * zoom, (((STRING_SPACING) * (this.strings.length - 1)) + TOP_SPACING + BOTTOM_SPACING) * zoom);
			
			UIPainter painterBuffer = this.image.createPainter();
			
			//fondo
			painterBuffer.setBackground(this.config.getColorBackground());
			painterBuffer.initPath(UIPainter.PATH_FILL);
			painterBuffer.addRectangle(area.getX() * zoom, area.getY() * zoom, area.getWidth() * zoom, area.getHeight() * zoom);
			painterBuffer.closePath();
			
			
			// pinto las cegillas
			TGIconManager iconManager = TGIconManager.getInstance(this.context);
			UIImage fretImage = iconManager.getFretboardFret();
			UIImage firstFretImage = iconManager.getFretboardFirstFret();
			
			painterBuffer.drawImage(firstFretImage, 0, 0, firstFretImage.getWidth(), firstFretImage.getHeight(), (this.frets[0] - 5) * zoom, (this.strings[0] - 5) * zoom, firstFretImage.getWidth() * zoom,this.strings[this.strings.length - 1] * zoom);
			
			paintFretPoints(painterBuffer,0, zoom);
			for (int i = 1; i < this.frets.length; i++) {
				painterBuffer.drawImage(fretImage, 0, 0, fretImage.getWidth(), fretImage.getHeight(), this.frets[i] * zoom, (this.strings[0] - 5) * zoom,fretImage.getWidth() * zoom, this.strings[this.strings.length - 1] * zoom);
				paintFretPoints(painterBuffer, i, zoom);
			}
			
			// pinto las cuerdas
			for (int i = 0; i < this.strings.length; i++) {
				painterBuffer.setForeground(this.config.getColorString());
				if(i > 2){
					painterBuffer.setLineWidth(2);
				}
				painterBuffer.initPath();
				painterBuffer.setAntialias(false);
				painterBuffer.moveTo(this.frets[0] * zoom, this.strings[i] * zoom);
				painterBuffer.lineTo(this.frets[this.frets.length - 1] * zoom, this.strings[i] * zoom);
				painterBuffer.closePath();
			}
			
			// pinto la escala
			paintScale(painterBuffer, zoom);
			
			painterBuffer.dispose();
		}
		painter.drawImage(this.image,0,0);
	}
	
	private void paintFretPoints(UIPainter painter, int fretIndex, float zoom) {
		painter.setBackground(this.config.getColorFretPoint());
		if ((fretIndex + 1) < this.frets.length) {
			int fret = ((fretIndex + 1) % 12);
			painter.setLineWidth(10 * zoom);
			if (fret == 0) {
				int size = (int) (getOvalSize() * zoom);
				int x = (int) ((this.frets[fretIndex] + ((this.frets[fretIndex + 1] - this.frets[fretIndex]) / 2)) * zoom);
				int y1 = (int) ((this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2) - STRING_SPACING) * zoom);
				int y2 = (int) ((this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2) + STRING_SPACING) * zoom);
				painter.initPath(UIPainter.PATH_FILL);
				painter.addCircle(x, y1, size);
				painter.addCircle(x, y2, size);
				painter.closePath();
			} else if (fret == 3 || fret == 5 || fret == 7 || fret == 9) {
				int size = (int) (getOvalSize() * zoom);
				int x = (int) ((this.frets[fretIndex] + ((this.frets[fretIndex + 1] - this.frets[fretIndex]) / 2)) * zoom);
				int y = (int) ((this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2)) * zoom);
				painter.initPath(UIPainter.PATH_FILL);
				painter.addCircle(x, y, size);
				painter.closePath();
			}
			painter.setLineWidth(1);
		}
	}
	
	private void paintScale(UIPainter painter, float zoom) {
		TGTrack track = getTrack();
		
		for (int i = 0; i < this.strings.length; i++) {
			TGString string = track.getString(i + 1);
			for (int j = 0; j < this.frets.length; j++) {
				
				int noteIndex = ((string.getValue() + j) %  12 );
				if(TuxGuitar.getInstance().getScaleManager().getScale().getNote(noteIndex)){
					int x = this.frets[j];
					if(j > 0){
						x -= ((x - this.frets[j - 1]) / 2);
					}
					int y = this.strings[i];

					if( (this.config.getStyle() & TGFretBoardConfig.DISPLAY_TEXT_SCALE) != 0 ){
						paintKeyText(painter,this.config.getColorScale(),x,y,NOTE_NAMES[noteIndex], zoom);
					}
					else{
						paintKeyOval(painter,this.config.getColorScale(),x,y, zoom);
					}
				}
			}
		}
		
		painter.setForeground(this.config.getColorBackground());
	}
	
	private void paintNotes(UIPainter painter, float zoom) {
		if(this.beat != null){
			TGTrack track = getTrack();
			
			for(int v = 0; v < this.beat.countVoices(); v ++){
				TGVoice voice = this.beat.getVoice( v );
				Iterator<TGNote> it = voice.getNotes().iterator();
				while (it.hasNext()) {
					TGNote note = (TGNote) it.next();
					int fretIndex = note.getValue();
					int stringIndex = note.getString() - 1;
					if (fretIndex >= 0 && fretIndex < this.frets.length && stringIndex >= 0 && stringIndex < this.strings.length) {
						int x = this.frets[fretIndex];
						if (fretIndex > 0) {
							x -= ((this.frets[fretIndex] - this.frets[fretIndex - 1]) / 2);
						}
						int y = this.strings[stringIndex];

						if( (this.config.getStyle() & TGFretBoardConfig.DISPLAY_TEXT_NOTE) != 0 ){
							int realValue = track.getString(note.getString()).getValue() + note.getValue();
							paintKeyText(painter,this.config.getColorNote(), x, y, NOTE_NAMES[ (realValue % 12) ], zoom);
						}
						else{
							paintKeyOval(painter,this.config.getColorNote(), x, y, zoom);
						}
					}
				}
			}
			painter.setLineWidth(1);
		}
	}
	
	private void paintKeyOval(UIPainter painter, UIColor background,int x, int y, float zoom) {
		x *= zoom;
		y *= zoom;
		int size = (int) (getOvalSize() * zoom);
		painter.setBackground(background);
		painter.initPath(UIPainter.PATH_FILL);
		painter.moveTo(x, y);
		painter.addCircle(x, y, size);
		painter.closePath();
	}
	
	private void paintKeyText(UIPainter painter, UIColor foreground, int x, int y, String text, float zoom) {
		painter.setBackground(this.config.getColorKeyTextBackground());
		painter.setForeground(foreground);
		painter.setFont(this.scaledFont);
		
		float fmWidth = painter.getFMWidth(text) / zoom;
		float fmHeight = painter.getFMHeight() / zoom;
		
		painter.initPath(UIPainter.PATH_FILL);
		painter.addRectangle((x - (fmWidth / 2f)) * zoom, (y - (fmHeight / 2f)) * zoom, fmWidth * zoom, fmHeight * zoom);
		painter.closePath();
		painter.drawString(text, (x - (fmWidth / 2f)) * zoom,(y + painter.getFMMiddleLine() / zoom) * zoom);
	}
	
	protected void paintEditor(UIPainter painter) {
		this.updateEditor();

		float zoom = this.control.getDeviceZoom() / 100f;
		UIFont font = this.config.getFont();

		if (this.frets.length > 0 && this.strings.length > 0) {
			this.scaledFont = this.getUIFactory().createFont(font.getName(), font.getHeight() * zoom, font.isBold(), font.isItalic());
			paintFretBoard(painter, zoom);
			paintNotes(painter, zoom);
			this.scaledFont.dispose();
		} else {
			UIFont percFont = this.getUIFactory().createFont(font.getName(), 14f * zoom, true, false);
			UIRectangle area = this.canvas.getBounds();
			painter.setBackground(this.config.getColorBackground());
			painter.setForeground(this.config.getColorKeyTextBackground());
			painter.setFont(percFont);

			painter.initPath(UIPainter.PATH_FILL);
			painter.addRectangle(0, 0, area.getWidth(), area.getHeight());
			painter.closePath();

			String percussionString = TuxGuitar.getProperty("instrument.percussion-channel");
			float tx = (area.getWidth() - painter.getFMWidth(percussionString) / zoom) / 2f;
			float ty = area.getHeight() / 2f + painter.getFMMiddleLine() / zoom;
			painter.drawString(percussionString, tx * zoom, ty * zoom);

			percFont.dispose();
		}
	}
	
	protected void hit(float x, float y) {
		int fretIndex = getFretIndex(x);
		if (fretIndex == -1) {
			return;
		}
		int stringIndex = getStringIndex(y);
		int stringNumber = (stringIndex + 1);
		
		this.selectString(stringNumber);
		if(!this.removeNote(fretIndex, stringNumber)) {
			this.addNote(fretIndex, stringNumber);
		}
	}
	
	private void selectString(int number) {
		TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGMoveToAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, getTrack().getString(number));
		tgActionProcessor.process();
	}
	
	private int getStringIndex(float y) {
		int index = -1;
		for (int i = 0; i < this.strings.length; i++) {
			if (index < 0) {
				index = i;
			} else {
				float distanceY = Math.abs(y - this.strings[index]);
				float currDistanceY = Math.abs(y - this.strings[i]);
				if( currDistanceY < distanceY) {
					index = i;
				}
			}
		}
		return index;
	}
	
	private int getFretIndex(float x) {
		int length = this.frets.length;
		if (length == 0) {
			return -1;
		}
		if ((x - 10) <= this.frets[0] && this.frets[0] < this.frets[length - 1]) {
			return 0;
		}
		if ((x + 10) >= this.frets[0] && this.frets[0] > this.frets[length - 1]) {
			return 0;
		}
		
		for (int i = 0; i < length; i++) {
			if ((i + 1) < length) {
				if (x > this.frets[i] && x <= this.frets[i + 1] || x > this.frets[i + 1] && x <= this.frets[i]) {
					return i + 1;
				}
			}
		}
		return length - 1;
	}
	
	private boolean removeNote(int fret, int string) {
		if(this.beat != null){
			for(int v = 0; v < this.beat.countVoices(); v ++){
				TGVoice voice = this.beat.getVoice( v );
				Iterator<TGNote> it = voice.getNotes().iterator();
				while (it.hasNext()) {
					TGNote note = (TGNote) it.next();
					if( note.getValue() == fret && note.getString() == string ) {
						TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGDeleteNoteAction.NAME);
						tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_NOTE, note);
						tgActionProcessor.process();
						
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private TGTrack getTrack() {
		if( this.beat != null ){
			TGMeasure measure = this.beat.getMeasure();
			if( measure != null ){
				TGTrack track = measure.getTrack();
				if( track != null ){
					return track;
				}
			}
		}
		return TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getTrack();
	}

	private int getFretCount() {
		TGTrack track = getTrack();
		if( track != null ){
			if (!TuxGuitar.getInstance().getSongManager().isPercussionChannel(track.getSong(), track.getChannelId())) {
				return track.getFrets();
			}
		}
		return -1;
	}

	private int getStringCount() {
		TGTrack track = getTrack();
		if( track != null ){
			return track.stringCount();
		}
		return 0;
	}
	
	private int getOvalSize(){
		return ((STRING_SPACING / 2) + (STRING_SPACING / 10));
	}
	
	private void addNote(int fret, int string) {
		TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGChangeNoteAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_FRET, fret);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, getTrack().getString(string));
		tgActionProcessor.process();
	}
	
	protected void updateDirection( int direction ){
		this.config.saveDirection( this.getDirection(direction) );
		this.initFrets(10, getFretCount());
		this.setChanges(true);
		this.canvas.redraw();
	}
	
	public boolean hasChanges(){
		return this.changes;
	}
	
	public void setChanges(boolean changes){
		this.changes = changes;
	}
	
	public void setExternalBeat(TGBeat externalBeat){
		this.externalBeat = externalBeat;
	}
	
	public TGBeat getExternalBeat(){
		return this.externalBeat;
	}
	
	public void redraw() {
		if(!this.isDisposed()){
			this.control.redraw();
			this.canvas.redraw();
			this.toolbar.update();
		}
	}
	
	public void redrawPlayingMode(){
		if(!this.isDisposed()){
			this.canvas.redraw();
		}
	 }
	
	public void dispose(){
		this.control.dispose();
		this.disposeFretBoardImage();
		this.config.dispose();
	}
	
	public void loadProperties(){
		int selection = this.handSelector.getSelectedItem().getValue();
		this.handSelector.removeItems();
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.right-mode"), TGFretBoardConfig.DIRECTION_RIGHT));
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.left-mode"), TGFretBoardConfig.DIRECTION_LEFT));
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, selection));
		this.toolbar.loadProperties();
		this.scale.setText(TuxGuitar.getProperty("scale"));
		this.scaleFinder.setToolTipText(TuxGuitar.getProperty("scale.finder"));
		this.loadScaleName();
		this.setChanges(true);
		this.control.layout();
	}
	
	public void loadIcons(){
	    this.toolbar.loadIcons();
		this.scaleFinder.setImage(TGIconManager.getInstance(this.context).getSearch());
		this.control.layout();
		this.layout(this.control.getChildArea().getWidth());
	}
	
	public void loadScale(){
		this.loadScaleName();
		this.setChanges(true);
		this.control.layout();
	}
	
	public int getWidth(){
		if (this.frets.length == 0) {
			return 0;
		}
		return this.frets[this.frets.length - 1];
	}
	
	public void computePackedSize() {
		this.initStrings(getStringCount());
		this.control.getLayout().set(this.canvas, UITableLayout.PACKED_HEIGHT, Float.valueOf(((STRING_SPACING) * (this.strings.length - 1)) + TOP_SPACING + BOTTOM_SPACING));
		this.control.computePackedSize(null, null);
	}
	
	public void layout(float width){
		this.disposeFretBoardImage();
		this.calculateFretSpacing(width);
		this.initFrets(10, getFretCount());
		this.initStrings(getStringCount());
		this.setChanges(false);
	}
	
	public void configure(){
		this.config.configure(TGWindow.getInstance(this.context).getWindow());
	}
	
	public void reloadFromConfig(){
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, this.getDirection(this.config.getDirection())));
		this.setChanges(true);
		this.redraw();
	}
	
	public UICanvas getCanvas(){
		return this.canvas;
	}
	
	public UIFactory getUIFactory() {
		return TGApplication.getInstance(this.context).getFactory();
	}
	
	private class TGFretBoardMouseListener implements UIMouseUpListener {
		
		public TGFretBoardMouseListener(){
			super();
		}
		
		public void onMouseUp(UIMouseEvent event) {
			getCanvas().setFocus();
			if( event.getButton() == 1 ){
				if(!TuxGuitar.getInstance().getPlayer().isRunning() && !TGEditorManager.getInstance(TGFretBoard.this.context).isLocked()){
					if( getExternalBeat() == null ){
						hit(event.getPosition().getX(), event.getPosition().getY());
					}else{
						setExternalBeat( null );
						TuxGuitar.getInstance().updateCache(true);
					}
				}
			}else{
				new TGActionProcessor(TGFretBoard.this.context, TGGoRightAction.NAME).process();
			}
		}
	}
	
	private class TGFretBoardPainterListener implements TGBufferedPainterHandle {
		
		public TGFretBoardPainterListener(){
			super();
		}

		public void paintControl(UIPainter painter) {
			TGFretBoard.this.paintEditor(painter);
		}

		public UICanvas getPaintableControl() {
			return TGFretBoard.this.canvas;
		}
	}
}
