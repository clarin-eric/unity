/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Link;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
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
	
	public AdminTopHeader(String title, AuthenticationProcessor authnProcessor, UnityMessageSource msg, 
			ViewSwitchCallback callback)
	{
		super(title, authnProcessor, msg);
		this.callback = callback;
	}

	@Override
	protected void addButtons(HorizontalLayout loggedPanel)
	{
		Link supportB = createSupportButton();
		loggedPanel.addComponent(supportB);		
		
		Button switchView = createSwitchButton();
		loggedPanel.addComponent(switchView);

		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
	}

	protected Link createSupportButton()
	{
		Link support = new Link();
		support.setResource(new ExternalResource("http://unity-idm.eu/site/support"));
		support.setTargetName("_blank");
		support.setDescription(msg.getMessage("AdminTopHeader.toSupport"));
		support.setIcon(Images.support32.getResource());
		return support;
	}
	
	protected Button createSwitchButton()
	{
		switchView = new Button();
		switchView.setStyleName(Reindeer.BUTTON_LINK);
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
			switchView.setIcon(Images.toAdmin32.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toAdmin"));
			adminView = false;
		} else
		{
			switchView.setIcon(Images.toProfile32.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toProfile"));
			adminView = true;
		}
	}
	
	public interface ViewSwitchCallback
	{
		public void showView(boolean admin);
	}
}
