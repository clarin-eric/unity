/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeader;

/**
 * Top header for admin UI. Allows to switch to (and from) user home UI view.
 * @author K. Benedyczak
 */
public class AdminTopHeader extends TopHeader
{
	private boolean adminView = true;
	private Button switchView;
	private ViewSwitchCallback callback;
	private final String supportLink;
        
	public AdminTopHeader(String title, StandardWebAuthenticationProcessor authnProcessor, UnityMessageSource msg, 
			ViewSwitchCallback callback, String supportLink)
	{
		super(title, authnProcessor, msg);
		this.callback = callback;
                this.supportLink = supportLink;
	}

	@Override
	protected void addButtons(HorizontalLayout loggedPanel)
	{
		Button supportB = createSupportButton();
		loggedPanel.addComponent(supportB);
		loggedPanel.setComponentAlignment(supportB, Alignment.MIDDLE_CENTER);
		
		Button switchView = createSwitchButton();
		loggedPanel.addComponent(switchView);
		loggedPanel.setComponentAlignment(switchView, Alignment.MIDDLE_CENTER);

		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
		loggedPanel.setComponentAlignment(logout, Alignment.MIDDLE_CENTER);
	}

	protected Button createSupportButton()
	{
		Button support = new Button();
		support.addStyleName(Styles.vButtonLink.toString());
		support.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Page.getCurrent().open(supportLink, "_blank", false);
			}
		});
		support.setDescription(msg.getMessage("AdminTopHeader.toSupport"));
		support.setIcon(Images.support.getResource());
		support.addStyleName(Styles.largeIcon.toString());
		return support;
	}
	
	protected Button createSwitchButton()
	{
		switchView = new Button();
		switchView.addStyleName(Styles.vButtonLink.toString());
		switchView.addStyleName(Styles.largeIcon.toString());
		switchView.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				switchView();
				callback.showView(adminView);
			}
		});
		switchView();
		return switchView;
	}

	private void switchView()
	{
		if (adminView)
		{
			switchView.setIcon(Images.toAdmin.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toAdmin"));
			adminView = false;
		} else
		{
			switchView.setIcon(Images.toProfile.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toProfile"));
			adminView = true;
		}
	}
	
	public interface ViewSwitchCallback
	{
		public void showView(boolean admin);
	}
}
