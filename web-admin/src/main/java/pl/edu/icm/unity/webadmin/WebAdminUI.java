/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.bus.RefreshEvent;
import pl.edu.icm.unity.webui.common.TopHeader;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class WebAdminUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private GroupsManagement test;

	private IdentitiesManagement testIdMan;
	
	private AttributesManagement testAttrMan;
	
	private ContentsManagementTab contentsManagement;
	
	private SchemaManagementTab schemaManagement;
	
	private EndpointDescription endpointDescription;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, GroupsManagement test,
			IdentitiesManagement testIdMan, AttributesManagement testAttrMan,
			ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement)
	{
		super(msg);
		this.test = test;
		this.testIdMan = testIdMan;
		this.testAttrMan = testAttrMan;
		this.contentsManagement = contentsManagement;
		this.schemaManagement = schemaManagement;
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		tmpInitContents();

		VerticalLayout contents = new VerticalLayout();
		TopHeader header = new TopHeader(endpointDescription.getId(), msg);
		contents.addComponent(header);

		
		MainTabPanel tabPanel = new MainTabPanel();
		contents.addComponent(tabPanel);
		
		tabPanel.addTab(contentsManagement);
		tabPanel.addTab(schemaManagement);
		tabPanel.setSizeFull();
		contents.setComponentAlignment(tabPanel, Alignment.TOP_LEFT);
		contents.setExpandRatio(tabPanel, 1.0f);
		
		contents.setSizeFull();
		setContent(contents);
		
		tmpRefreshTypes();
	}

	
	//TODO remove below this line
	private void tmpInitContents()
	{
		try
		{
			test.addGroup(new Group("/A"));
			test.addGroup(new Group("/A/B"));
			test.addGroup(new Group("/A/B/C"));
			test.addGroup(new Group("/D"));
			test.addGroup(new Group("/D/E"));
			test.addGroup(new Group("/D/G"));
			test.addGroup(new Group("/D/F"));
			
			AttributeType userPicture = new AttributeType("picture", new JpegImageAttributeSyntax());
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxSize(1400000);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxWidth(900);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxHeight(900);
			userPicture.setMaxElements(1);
			userPicture.setDescription("Picture of the user");
			testAttrMan.addAttributeType(userPicture);
			
			AttributeType postalcode = new AttributeType("postalcode", new StringAttributeSyntax());
			postalcode.setMaxElements(Integer.MAX_VALUE);
			postalcode.setDescription("Postal code");
			((StringAttributeSyntax)postalcode.getValueType()).setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
			((StringAttributeSyntax)postalcode.getValueType()).setMaxLength(6);
			testAttrMan.addAttributeType(postalcode);

			AttributeType height = new AttributeType("height", new FloatingPointAttributeSyntax());
			height.setMinElements(1);
			height.setDescription("He\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjh");
			testAttrMan.addAttributeType(height);
			
			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "foo", true, true);
			testIdMan.addIdentity(toAdd, "Password requirement", LocalAuthenticationState.outdated, false);
		} catch (Exception e)
		{
			return;
		} 
	}
	
	private void tmpRefreshTypes()
	{
		try
		{
			List<AttributeType> atList = testAttrMan.getAttributeTypes();
			EventsBus bus = WebSession.getCurrent().getEventBus();
			bus.fireEvent(new AttributeTypesUpdatedEvent(atList));
			bus.fireEvent(new RefreshEvent());
		} catch (EngineException e)
		{
			e.printStackTrace();
		}
	}
}
