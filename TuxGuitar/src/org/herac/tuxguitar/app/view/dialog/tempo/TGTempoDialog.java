package org.herac.tuxguitar.app.view.dialog.tempo;

import org.herac.tuxguitar.app.TuxGuitar;
import org.herac.tuxguitar.app.ui.TGApplication;
import org.herac.tuxguitar.app.view.controller.TGViewContext;
import org.herac.tuxguitar.app.view.util.TGDialogUtil;
import org.herac.tuxguitar.app.view.widgets.TGDialogButtons;
import org.herac.tuxguitar.document.TGDocumentContextAttributes;
import org.herac.tuxguitar.editor.action.TGActionProcessor;
import org.herac.tuxguitar.editor.action.composition.TGChangeTempoRangeAction;
import org.herac.tuxguitar.song.models.TGMeasureHeader;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGTempo;
import org.herac.tuxguitar.ui.UIFactory;
import org.herac.tuxguitar.ui.layout.UITableLayout;
import org.herac.tuxguitar.ui.widget.*;
import org.herac.tuxguitar.util.TGContext;

public class TGTempoDialog {
	
	private static final int MIN_TEMPO = 30;
	private static final int MAX_TEMPO = 320;
	
	protected static final int[] DEFAULT_PERCENTS = new int[]{25,50,75,100,125,150,175,200};
	
	public void show(final TGViewContext context) {
		final TGSong song = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		final TGMeasureHeader header = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER);
		
		final UIFactory uiFactory = TGApplication.getInstance(context.getContext()).getFactory();
		final UIWindow uiParent = context.getAttribute(TGViewContext.ATTRIBUTE_PARENT);
		final UITableLayout dialogLayout = new UITableLayout();
		final UIWindow dialog = uiFactory.createWindow(uiParent, true, false);
		
		dialog.setLayout(dialogLayout);
		dialog.setText(TuxGuitar.getProperty("composition.tempo"));
		
		//-----------------TEMPO------------------------
		UITableLayout groupLayout = new UITableLayout();
		UIPanel group = uiFactory.createPanel(dialog, false);
		group.setLayout(groupLayout);
		dialogLayout.set(group, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);
		
		TGTempo currentTempo = header.getTempo();
		UILabel tempoLabel = uiFactory.createLabel(group);
		tempoLabel.setText(TuxGuitar.getProperty("composition.tempo"));
		groupLayout.set(tempoLabel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, true);
		
		
		final UISpinner tempo = uiFactory.createSpinner(group);
		tempo.setMinimum(MIN_TEMPO);
		tempo.setMaximum(MAX_TEMPO);
		tempo.setValue(currentTempo.getValue());
		groupLayout.set(tempo, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 150f, null, null);
		
		//------------------OPTIONS--------------------------
		UITableLayout optionsLayout = new UITableLayout();
		UIPanel options = uiFactory.createPanel(dialog, false);
		options.setLayout(optionsLayout);
		dialogLayout.set(options, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		final UIRadioButton applyToNext = uiFactory.createRadioButton(options);
		applyToNext.setText(TuxGuitar.getProperty("composition.tempo.position-to-next"));
		applyToNext.setSelected(true);
		optionsLayout.set(applyToNext, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		final UIRadioButton applyToEnd = uiFactory.createRadioButton(options);
		applyToEnd.setText(TuxGuitar.getProperty("composition.tempo.position-to-end"));
		optionsLayout.set(applyToEnd, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		final UIRadioButton applyToAllMeasures = uiFactory.createRadioButton(options);
		applyToAllMeasures.setText(TuxGuitar.getProperty("composition.tempo.start-to-end"));
		optionsLayout.set(applyToAllMeasures, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		//------------------BUTTONS--------------------------
		TGDialogButtons buttons = new TGDialogButtons(uiFactory, dialog,
				TGDialogButtons.ok(() -> {
					Integer value = tempo.getValue();
					Integer applyTo = parseApplyTo(applyToAllMeasures, applyToEnd, applyToNext);

					changeTempo(context.getContext(), song, header, value, applyTo);
					dialog.dispose();

				}), TGDialogButtons.cancel(dialog::dispose));
		dialogLayout.set(buttons.getControl(), 3, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, false);

		TGDialogUtil.openDialog(dialog,TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}
	
	private Integer parseApplyTo(UIRadioButton applyToAll, UIRadioButton applyToEnd, UIRadioButton applyToNext) {
		if( applyToAll.isSelected() ) {
			return TGChangeTempoRangeAction.APPLY_TO_ALL;
		}
		if( applyToEnd.isSelected() ) {
			return TGChangeTempoRangeAction.APPLY_TO_END;
		}
		if( applyToNext.isSelected() ) {
			return TGChangeTempoRangeAction.APPLY_TO_NEXT;
		}
		return 0;
	}
	
	public void changeTempo(TGContext context, TGSong song, TGMeasureHeader header, Integer value, Integer applyTo) {
		TGActionProcessor tgActionProcessor = new TGActionProcessor(context, TGChangeTempoRangeAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, song);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, header);
		tgActionProcessor.setAttribute(TGChangeTempoRangeAction.ATTRIBUTE_TEMPO, value);
		tgActionProcessor.setAttribute(TGChangeTempoRangeAction.ATTRIBUTE_APPLY_TO, applyTo);
		tgActionProcessor.processOnNewThread();
	}
}
