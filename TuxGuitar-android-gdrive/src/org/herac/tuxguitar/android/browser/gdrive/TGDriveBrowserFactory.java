package org.herac.tuxguitar.android.browser.gdrive;

import org.herac.tuxguitar.android.browser.model.TGBrowserException;
import org.herac.tuxguitar.android.browser.model.TGBrowserFactory;
import org.herac.tuxguitar.android.browser.model.TGBrowserFactoryHandler;
import org.herac.tuxguitar.android.browser.model.TGBrowserFactorySettingsHandler;
import org.herac.tuxguitar.android.browser.model.TGBrowserSettings;
import org.herac.tuxguitar.util.TGContext;

public class TGDriveBrowserFactory implements TGBrowserFactory{
	
	public static final String BROWSER_TYPE = "google-drive";
	public static final String BROWSER_NAME = "Google Drive";
	
	private TGContext context;
	
	public TGDriveBrowserFactory(TGContext context) {
		this.context = context;
	}
	
	public String getType(){
		return BROWSER_TYPE;
	}
	
	public String getName(){
		return BROWSER_NAME;
	}
	
	public void createBrowser(TGBrowserFactoryHandler handler, TGBrowserSettings data) throws TGBrowserException {
		handler.onCreateBrowser(new TGDriveBrowser(this.context, TGDriveBrowserSettings.createInstance(data)));
	}

	public void createSettings(final TGBrowserFactorySettingsHandler handler) throws TGBrowserException {
		new TGDriveBrowserSettingsFactory(this.context, handler).createSettings();
	}
}
