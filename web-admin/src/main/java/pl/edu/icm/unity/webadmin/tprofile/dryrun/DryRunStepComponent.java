/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.dryrun;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webadmin.tprofile.MappingResultComponent;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileViewer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;

/**
 * UI Component used by {@link DryRunStep}.
 * 
 * @author Roman Krysinski
 */
public class DryRunStepComponent extends CustomComponent 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, DryRunStepComponent.class);
	
	private VerticalLayout resultWrapper;
	private Label capturedLogs;
	private HtmlLabel logsLabel;
	private Label hr_2;
	private VerticalLayout mappingResultWrap;
	private Label hr_1;
	private VerticalLayout remoteIdpWrap;
	private Label hr_3;
	private Label authnResultLabel;
	private VerticalLayout progressWrapper;
	private InputTranslationActionsRegistry taRegistry;
	private TranslationProfileManagement tpMan;
	private UnityMessageSource msg;
	private MappingResultComponent mappingResult;
	private RemotelyAuthenticatedInputComponent remoteIdpInput;
	private TranslationProfileViewer profileViewer;
	
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 * @param msg 
	 * @param sandboxURL 
	 */
	public DryRunStepComponent(UnityMessageSource msg, String sandboxURL, 
			TranslationProfileManagement tpMan, InputTranslationActionsRegistry taRegistry) 
	{
		this.msg = msg;
		this.tpMan = tpMan;
		this.taRegistry = taRegistry;
		
		setCompositionRoot(buildMainLayout());
		
		capturedLogs.setContentMode(ContentMode.PREFORMATTED);
		capturedLogs.setValue("");
		
		logsLabel.resetValue();
		
		mappingResult = new MappingResultComponent(msg);
		mappingResultWrap.addComponent(mappingResult);
		
		remoteIdpInput = new RemotelyAuthenticatedInputComponent(msg);
		remoteIdpWrap.addComponent(remoteIdpInput);
		
		progressWrapper.addComponent(new Image("", Images.loader.getResource()));
		indicateProgress();
	}

	public void handle(SandboxAuthnEvent event) 
	{
		RemoteSandboxAuthnContext ctx = (RemoteSandboxAuthnContext) event.getCtx();
		if (ctx.getAuthnException() == null)
		{
			authnResultLabel.setValue(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.success"));
			authnResultLabel.setStyleName(Styles.success.toString());
		} else
		{
			authnResultLabel.setValue(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.error"));
			authnResultLabel.setStyleName(Styles.error.toString());
		}
		logsLabel.setHtmlValue("DryRun.DryRunStepComponent.logsLabel");
		RemotelyAuthenticatedContext remoteAuthnContext = ctx.getAuthnContext();
		if (remoteAuthnContext != null)
		{
			remoteIdpInput.displayAuthnInput(remoteAuthnContext.getAuthnInput());
			mappingResult.displayMappingResult(remoteAuthnContext.getMappingResult(), 
				remoteAuthnContext.getInputTranslationProfile());
			showProfile(remoteAuthnContext.getInputTranslationProfile());
		} else
		{
			profileViewer.setVisible(false);
			hr_2.setVisible(false);
		}
		Exception e = ctx.getAuthnException();
		StringBuilder logs = new StringBuilder(ctx.getLogs());
		if (e != null)
		{
			CharArrayWriter writer = new CharArrayWriter();
			e.printStackTrace(new PrintWriter(writer));
			logs.append("\n\n").append(writer.toString());
		}
		capturedLogs.setValue(logs.toString());
				
		hideProgressShowResult();
	}

	public void indicateProgress()
	{
		resultWrapper.setVisible(false);
		progressWrapper.setVisible(true);
	}
	
	private void hideProgressShowResult()
	{
		progressWrapper.setVisible(false);
		resultWrapper.setVisible(true);
	}
	
	private void showProfile(String profile)
	{
		boolean isHRVisible = (profile != null);
		try
		{
			TranslationProfile tp = tpMan.listInputProfiles().get(profile);
			profileViewer.setInput(tp, taRegistry);
			profileViewer.setVisible(true);
		} catch (EngineException e)
		{
			isHRVisible = false;
			log.error(e);
		}
		hr_2.setVisible(isHRVisible);
	}
	
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		VerticalLayout mainLayout = new VerticalLayout();
		
		// progressWrapper
		progressWrapper = new VerticalLayout();
		progressWrapper.setSizeUndefined();
		progressWrapper.setMargin(false);
		mainLayout.addComponent(progressWrapper);
		
		// resultWrapper
		resultWrapper = buildResultWrapper();
		mainLayout.addComponent(resultWrapper);
		
		return mainLayout;
	}

	private VerticalLayout buildResultWrapper() {
		// common part: create layout
		resultWrapper = new VerticalLayout();
		resultWrapper.setHeightUndefined();
		
		// authnResultLabel
		authnResultLabel = new Label();
		authnResultLabel.setWidth(100, Unit.PERCENTAGE);
		authnResultLabel.setHeightUndefined();
		authnResultLabel.setValue("Label");
		resultWrapper.addComponent(authnResultLabel);
		
		// hr_3
		hr_3 = HtmlTag.horizontalLine();
		hr_3.setWidth("100.0%");
		hr_3.setHeight("-1px");
		resultWrapper.addComponent(hr_3);
		
		// remoteIdpWrap
		remoteIdpWrap = new VerticalLayout();
		remoteIdpWrap.setMargin(false);

		// mappingResultWrap
		mappingResultWrap = new VerticalLayout();
		mappingResultWrap.setHeight("-1px");
		mappingResultWrap.setMargin(false);
		
		HorizontalSplitPanel hr = new  HorizontalSplitPanel(remoteIdpWrap, mappingResultWrap);
		//hr.setMargin(false);
		hr.setSizeFull();
		hr.setSplitPosition(50, Unit.PERCENTAGE);
		
		resultWrapper.addComponent(hr);
		
		// hr_1
		hr_1 = HtmlTag.horizontalLine();
		resultWrapper.addComponent(hr_1);
		
		profileViewer = new TranslationProfileViewer(msg);
		
		resultWrapper.addComponent(profileViewer);
		
		// hr_2
		hr_2 = HtmlTag.horizontalLine();
		resultWrapper.addComponent(hr_2);
		
		// logsLabel
		logsLabel = new HtmlLabel(msg);
		logsLabel.setWidth(100, Unit.PERCENTAGE);
		logsLabel.setHeight("-1px");
		resultWrapper.addComponent(logsLabel);
		
		// capturedLogs
		capturedLogs = new Label();
		capturedLogs.setWidth(100, Unit.PERCENTAGE);
		capturedLogs.setHeight("-1px");
		capturedLogs.setValue("Label");
		resultWrapper.addComponent(capturedLogs);
		
		return resultWrapper;
	}

}
