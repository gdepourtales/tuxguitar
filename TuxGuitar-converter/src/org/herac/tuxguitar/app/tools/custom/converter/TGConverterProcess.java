package org.herac.tuxguitar.app.tools.custom.converter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.system.icons.TGIconEvent;
import org.herac.tuxguitar.app.system.language.TGLanguageEvent;
import org.herac.tuxguitar.app.util.DialogUtils;
import org.herac.tuxguitar.event.TGEvent;
import org.herac.tuxguitar.event.TGEventListener;
import org.herac.tuxguitar.util.TGContext;
import org.herac.tuxguitar.util.TGException;
import org.herac.tuxguitar.util.TGSynchronizer;

public class TGConverterProcess implements TGConverterListener, TGEventListener{
	
	private static final int SHELL_WIDTH = 650;
	private static final int SHELL_HEIGHT = 350;
	
	protected static final String EOL = ("\n");
	
	protected static final Color COLOR_INFO  = TuxGuitar.getInstance().getDisplay().getSystemColor(SWT.COLOR_BLUE);
	protected static final Color COLOR_ERROR = TuxGuitar.getInstance().getDisplay().getSystemColor(SWT.COLOR_RED );
	
	private TGContext context;
	protected Shell dialog;
	protected StyledText output;
	protected Button buttonCancel;
	protected Button buttonClose;
	protected TGConverter converter;
	protected boolean finished;
	
	public TGConverterProcess(TGContext context) {
		this.context = context;
	}
	
	public void start(String initFolder, String destFolder, TGConverterFormat format ){
		this.converter = new TGConverter(this.context, initFolder, destFolder);
		this.converter.setFormat(format);
		this.converter.setListener(this);
		
		this.showProcess();
		
		new Thread(new Runnable() {
			public void run() throws TGException {
				TGConverterProcess.this.converter.process();
			}
		}).start();
	}
	
	protected void showProcess() {
		this.finished = false;
		
		this.dialog = DialogUtils.newDialog(TuxGuitar.getInstance().getShell(),SWT.SHELL_TRIM);
		this.dialog.setLayout(new GridLayout());
		this.dialog.setSize( SHELL_WIDTH , SHELL_HEIGHT );
		this.dialog.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TuxGuitar.getInstance().getIconManager().removeLoader( TGConverterProcess.this );
				TuxGuitar.getInstance().getLanguageManager().removeLoader( TGConverterProcess.this );
			}
		});
		this.dialog.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				e.doit = TGConverterProcess.this.finished;
			}
		});
		
		Composite composite = new Composite(this.dialog,SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		
		this.output = new StyledText(composite,SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		this.output.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.output.setEditable(false);
		
		//------------------BUTTONS--------------------------
		Composite buttons = new Composite(this.dialog, SWT.NONE);
		buttons.setLayout(new GridLayout(2,false));
		buttons.setLayoutData(new GridData(SWT.RIGHT,SWT.BOTTOM,true,false));
		
		this.buttonCancel = new Button(buttons, SWT.PUSH);
		this.buttonCancel.setEnabled( false );
		this.buttonCancel.setLayoutData(getButtonsData());
		this.buttonCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				TGConverterProcess.this.converter.setCancelled( true );
			}
		});
		
		this.buttonClose = new Button(buttons, SWT.PUSH);
		this.buttonClose.setEnabled( false );
		this.buttonClose.setLayoutData(getButtonsData());
		this.buttonClose.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				TGConverterProcess.this.dialog.dispose();
			}
		});
		
		this.loadIcons(false);
		this.loadProperties(false);
		
		TuxGuitar.getInstance().getIconManager().addLoader( this );
		TuxGuitar.getInstance().getLanguageManager().addLoader( this );
		
		DialogUtils.openDialog(this.dialog, DialogUtils.OPEN_STYLE_CENTER);
	}
	
	private GridData getButtonsData(){
		GridData data = new GridData(SWT.FILL,SWT.FILL,true,true);
		data.minimumWidth = 80;
		data.minimumHeight = 25;
		return data;
	}
	
	public boolean isDisposed(){
		return (this.dialog == null || this.dialog.isDisposed() );
	}
	
	public void loadProperties(){
		this.loadProperties(true);
	}
	
	public void loadProperties(boolean layout){
		if(!isDisposed()){
			this.dialog.setText(TuxGuitar.getProperty("batch.converter"));
			this.buttonCancel.setText(TuxGuitar.getProperty("cancel"));
			this.buttonClose.setText(TuxGuitar.getProperty("close"));
			if(layout){
				this.dialog.layout(true, true);
			}
		}
	}
	
	public void loadIcons() {
		this.loadIcons(true);
	}
	
	public void loadIcons(boolean layout){
		if(!isDisposed()){
			this.dialog.setImage(TuxGuitar.getInstance().getIconManager().getAppIcon());
			if(layout){
				this.dialog.layout(true, true);
			}
		}
	}
	
	//------------------------------------------------------------------------------------------------//
	//---TGConverterListener Implementation ----------------------------------------------------------//
	//------------------------------------------------------------------------------------------------//
	
	public void notifyFileProcess(final String filename) {
		if(!isDisposed() ){
			try {
				TGSynchronizer.instance().execute(new TGSynchronizer.TGRunnable() {
					public void run() throws TGException {
						if(!isDisposed() ){
							TGConverterProcess.this.output.append(TuxGuitar.getProperty("batch.converter.messages.converting", new String[] {filename}));
							TGConverterProcess.this.output.setSelection( TGConverterProcess.this.output.getCharCount() );
						}
					}
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public void notifyFileResult(final String filename, final int result) {
		if(!isDisposed() ){
			try {
				TGSynchronizer.instance().execute(new TGSynchronizer.TGRunnable() {
					public void run() throws TGException {
						if(!isDisposed() ){
							TGConverterProcess.this.appendLogMessage(result, filename);
							TGConverterProcess.this.output.setSelection( TGConverterProcess.this.output.getCharCount() );
						}
					}
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public void notifyStart() {
		if(!isDisposed() ){
			try {
				TGSynchronizer.instance().execute(new TGSynchronizer.TGRunnable() {
					public void run() throws TGException {
						if(!isDisposed() ){
							TGConverterProcess.this.finished = false;
							TGConverterProcess.this.buttonClose.setEnabled( TGConverterProcess.this.finished );
							TGConverterProcess.this.buttonCancel.setEnabled( !TGConverterProcess.this.finished );
							TGConverterProcess.this.output.setCursor(TGConverterProcess.this.dialog.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
						}
					}
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public void notifyFinish() {
		if(!isDisposed() ){
			try {
				TGSynchronizer.instance().execute(new TGSynchronizer.TGRunnable() {
					public void run() throws TGException {
						if(!isDisposed() ){
							TGConverterProcess.this.finished = true;
							TGConverterProcess.this.buttonClose.setEnabled( TGConverterProcess.this.finished );
							TGConverterProcess.this.buttonCancel.setEnabled( !TGConverterProcess.this.finished );
							TGConverterProcess.this.output.setCursor(TGConverterProcess.this.dialog.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
						}
					}
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void appendLogMessage(int result, String fileName) {
		String message = (TuxGuitar.getProperty( "batch.converter.messages." + (result == TGConverter.FILE_OK ? "ok" : "failed") ) + EOL);
		
		switch (result) {
			case TGConverter.FILE_COULDNT_WRITE :
				message += ( TuxGuitar.getProperty("batch.converter.messages.couldnt-write", new String[] {fileName}) + EOL );
				break;
			case TGConverter.FILE_BAD :
				message += ( TuxGuitar.getProperty("batch.converter.messages.bad-file", new String[] {fileName}) + EOL );
				break;
			case TGConverter.FILE_NOT_FOUND :
				message += ( TuxGuitar.getProperty("batch.converter.messages.file-not-found", new String[] {fileName}) + EOL );
				break;
			case TGConverter.OUT_OF_MEMORY :
				message += ( TuxGuitar.getProperty("batch.converter.messages.out-of-memory", new String[] {fileName}) + EOL );
				break;
			case TGConverter.EXPORTER_NOT_FOUND :
				message += ( TuxGuitar.getProperty("batch.converter.messages.exporter-not-found", new String[] {fileName}) + EOL );
				break;
			case TGConverter.UNKNOWN_ERROR :
				message += ( TuxGuitar.getProperty("batch.converter.messages.unknown-error", new String[] {fileName}) + EOL );
				break;
		}
		
		StyleRange range = new StyleRange();
		range.foreground = ( result == TGConverter.FILE_OK ? TGConverterProcess.COLOR_INFO : TGConverterProcess.COLOR_ERROR );
		range.start = TGConverterProcess.this.output.getCharCount();
		range.length = message.length();
		
		TGConverterProcess.this.output.append( message );
		TGConverterProcess.this.output.setStyleRange(range);
	}

	public void processEvent(TGEvent event) {
		if( TGIconEvent.EVENT_TYPE.equals(event.getEventType()) ) {
			this.loadIcons();
		}
		else if( TGLanguageEvent.EVENT_TYPE.equals(event.getEventType()) ) {
			this.loadProperties();
		}
	}
}
