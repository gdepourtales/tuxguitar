package org.herac.tuxguitar.editor.action.note;

import org.herac.tuxguitar.action.TGActionContext;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionBase;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.util.TGContext;

public class TGRemoveMixerChangeAction extends TGActionBase {

	public static final String NAME = "action.beat.general.remove-mixer-change";

	public TGRemoveMixerChangeAction(TGContext context) {
		super(context, NAME);
	}
	
	protected void processAction(TGActionContext context){
		TGBeat beat = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT);
		TGSongManager tgSongManager = getSongManager(context);
		tgSongManager.getMeasureManager().removeMixerChange(beat);
	}
}
