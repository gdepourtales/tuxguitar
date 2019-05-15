package org.herac.tuxguitar.app.view.dialog.channel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.herac.tuxguitar.song.models.TGChannel;
import org.herac.tuxguitar.ui.UIFactory;
import org.herac.tuxguitar.ui.event.UISelectionEvent;
import org.herac.tuxguitar.ui.event.UISelectionListener;
import org.herac.tuxguitar.ui.layout.UIScrollBarPanelLayout;
import org.herac.tuxguitar.ui.layout.UITableLayout;
import org.herac.tuxguitar.ui.widget.UILayoutContainer;
import org.herac.tuxguitar.ui.widget.UIPanel;
import org.herac.tuxguitar.ui.widget.UIScrollBarPanel;
import org.herac.tuxguitar.ui.widget.UISeparator;

public class TGChannelList {
	
	private static final int SCROLL_INCREMENT = 10;
	
	private List<TGChannelItem> channelItems;
	private List<UISeparator> separators;
	private TGChannelManagerDialog dialog;
	
	protected UIScrollBarPanel channelItemAreaSC;
	protected UIPanel channelItemArea;
	
	public TGChannelList(TGChannelManagerDialog dialog){
		this.dialog = dialog;
		this.channelItems = new ArrayList<TGChannelItem>();
		this.separators = new ArrayList<>();
	}
	
	public void show(UILayoutContainer parent){
		UIFactory uiFactory = this.dialog.getUIFactory();
		
		this.channelItemAreaSC = uiFactory.createScrollBarPanel(parent, true, false, false);
		this.channelItemAreaSC.setLayout(new UIScrollBarPanelLayout(false, true, true, true, false, true));
		
		this.channelItemAreaSC.getVScroll().setIncrement(SCROLL_INCREMENT);
		this.channelItemAreaSC.getVScroll().addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGChannelList.this.channelItemAreaSC.layout();
			}
		});
		
		this.channelItemArea = uiFactory.createPanel(this.channelItemAreaSC, false);
	}
	
	public void removeChannelsAfter( int count ){
		while(!this.channelItems.isEmpty() && this.channelItems.size() > count ){
			TGChannelItem tgChannelItem = this.channelItems.remove(0);
			tgChannelItem.dispose();
            UISeparator separator = this.separators.remove(0);
            separator.dispose();
		}
	}
	
	public TGChannelItem getOrCreateChannelItemAt( int index){
		while( this.channelItems.size() <= index ){
			TGChannelItem tgChannelItem = new TGChannelItem(this.dialog);
			tgChannelItem.show(this.channelItemArea);
			this.channelItems.add(tgChannelItem);
            UISeparator separator = this.dialog.getUIFactory().createHorizontalSeparator(this.channelItemArea);
            this.separators.add(separator);
		}
		return (TGChannelItem)this.channelItems.get(index);
	}
	
	public void loadProperties(){
		Iterator<TGChannelItem> it = this.channelItems.iterator();
		while( it.hasNext() ){
			TGChannelItem tgChannelItem = (TGChannelItem)it.next();
			tgChannelItem.loadProperties();
		}
	}
	
	public void loadIcons(){
		Iterator<TGChannelItem> it = this.channelItems.iterator();
		while( it.hasNext() ){
			TGChannelItem tgChannelItem = (TGChannelItem)it.next();
			tgChannelItem.loadIcons();
		}
	}
	
	public void updateItems(){
		List<TGChannel> channels = this.dialog.getHandle().getChannels();
		
		boolean countChanged = (channels.size() != this.channelItems.size());
		if( countChanged ) {
			this.removeChannelsAfter(channels.size());
		}
		
		for(int i = 0 ; i < channels.size() ; i ++) {
			TGChannel channel = (TGChannel)channels.get(i);
			TGChannelItem tgChannelItem = getOrCreateChannelItemAt(i);
			tgChannelItem.loadChannel(channel);
		}
		
		if( countChanged ) {
			this.layoutItems();
		}
	}
	
	public void layoutItems() {
		UITableLayout uiLayout = new UITableLayout();
		uiLayout.set(UITableLayout.MARGIN, 0f);
		for(int i = 0 ; i < this.channelItems.size() ; i ++) {
			uiLayout.set(this.channelItems.get(i).getComposite(), ((i * 2) + 1), 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 1, null, null, 4f);
            uiLayout.set(this.separators.get(i), ((i * 2) + 2), 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 1, null, null, 0f);
		}
		this.channelItemArea.setLayout(uiLayout);
		this.channelItemAreaSC.layout();
	}
	
	public UIScrollBarPanel getControl() {
		return this.channelItemAreaSC;
	}
}
