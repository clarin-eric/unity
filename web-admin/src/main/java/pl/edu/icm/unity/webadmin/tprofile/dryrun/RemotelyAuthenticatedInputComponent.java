/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.dryrun;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component that displays RemotelyAuthenticatedInput.
 * 
 * @author Roman Krysinski
 */
public class RemotelyAuthenticatedInputComponent extends CustomComponent 
{

	private VerticalLayout mainLayout;
	private VerticalLayout mappingResultWrap;
	private HorizontalLayout groupsWrap;
	private Label groupsLabel;
	private Label groupsTitleLabel;
	private VerticalLayout attrsWrap;
	private Grid<RemoteAttribute> attrsTable;
	private Label attrsTitleLabel;
	private VerticalLayout idsWrap;
	private Grid<RemoteIdentity> idsTable;
	private Label idsTitleLabel;
	private HorizontalLayout titleWrap;
	private Label noneLabel;
	private HtmlLabel titleLabel;
	private UnityMessageSource msg;
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 * @param msg 
	 */
	public RemotelyAuthenticatedInputComponent(UnityMessageSource msg) 
	{
		this.msg = msg;
		buildMainLayout();
		setCompositionRoot(mainLayout);
		
		mappingResultWrap.setStyleName(Styles.smallMargin.toString());
		setVisible(false);
		initLabels();
		initTables();
	}

	private void initLabels() 
	{
		idsTitleLabel.setValue(msg.getMessage("MappingResultComponent.idsTitle"));
		attrsTitleLabel.setValue(msg.getMessage("MappingResultComponent.attrsTitle"));
		groupsTitleLabel.setValue(msg.getMessage("MappingResultComponent.groupsTitle"));
		noneLabel.setValue(msg.getMessage("MappingResultComponent.none"));
		groupsLabel.setValue("");
	}
	
	private void initTables()
	{
		idsTable.addColumn(RemoteIdentity::getIdentityType)
				.setCaption(msg.getMessage("MappingResultComponent.idsTable.type"));
		idsTable.addColumn(RemoteIdentity::getName).setCaption(
				msg.getMessage("MappingResultComponent.idsTable.value"));

		attrsTable.addColumn(RemoteAttribute::getName).setCaption(
				msg.getMessage("MappingResultComponent.attrsTable.name"));
		attrsTable.addColumn(RemoteAttribute::getValues).setCaption(
				msg.getMessage("MappingResultComponent.attrsTable.value"));
	}

	public void displayAuthnInput(RemotelyAuthenticatedInput input)
	{
		if (input == null 
				|| (input.getIdentities().isEmpty()
					&& input.getAttributes().isEmpty()
					&& input.getGroups().isEmpty()))
		{
			displayItsTables(Collections.<RemoteIdentity>emptyList());
			displayAttrsTable(Collections.<RemoteAttribute>emptyList());
			displayGroups(Collections.<RemoteGroupMembership>emptyList());
			noneLabel.setVisible(true);
		} else
		{
			titleLabel.setHtmlValue("DryRun.RemotelyAuthenticatedContextComponent.title", 
					input.getIdpName());
			displayItsTables(input.getIdentities().values());
			displayAttrsTable(input.getAttributes().values());
			displayGroups(input.getGroups().values());
			noneLabel.setVisible(false);
		}
		setVisible(true);
	}

	private void displayItsTables(Collection<RemoteIdentity> collection) 
	{
		idsTable.setItems(Collections.emptyList());
		if (collection.isEmpty())
		{
			idsWrap.setVisible(false);
		} else
		{
			idsWrap.setVisible(true);
			RemoteIdentity[] identityArray = collection.toArray(new RemoteIdentity[collection.size()]);
			idsTable.setItems(identityArray);
	
			idsTable.setHeightMode(HeightMode.ROW);
			idsTable.setHeightByRows(identityArray.length);		}
	}
	
	private void displayAttrsTable(Collection<RemoteAttribute> collection) 
	{
		attrsTable.setItems(Collections.emptyList());
		if (collection.isEmpty())
		{
			attrsWrap.setVisible(false);
		} else
		{
			attrsWrap.setVisible(true);
			RemoteAttribute[] attributeArray = collection.toArray(new RemoteAttribute[collection.size()]);
			attrsTable.setItems(attributeArray);
			
			attrsTable.setHeightMode(HeightMode.ROW);
			attrsTable.setHeightByRows(attributeArray.length);
		}
	}
	
	private void displayGroups(Collection<RemoteGroupMembership> collection) 
	{
		if (collection.isEmpty())
		{
			groupsWrap.setVisible(false);
		} else
		{
			groupsWrap.setVisible(true);
			groupsLabel.setValue(collection.toString());
		}
	}
	
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(false);
		
			
		// top-level component properties
		setSizeFull();
		
		// titleWrap
		titleWrap = buildTitleWrap();
		mainLayout.addComponent(titleWrap);
		
		// mappingResultWrap
		mappingResultWrap = buildMappingResultWrap();
		mainLayout.addComponent(mappingResultWrap);
		mainLayout.setExpandRatio(mappingResultWrap, 1.0f);
		
		return mainLayout;
	}

	private HorizontalLayout buildTitleWrap() {
		// common part: create layout
		titleWrap = new HorizontalLayout();
		titleWrap.setWidth("-1px");
		titleWrap.setHeight("-1px");
		titleWrap.setMargin(false);
		//titleWrap.setSpacing(true);
		
		// titleLabel
		titleLabel = new HtmlLabel(msg);
		titleLabel.setWidth("-1px");
		titleLabel.setHeight("-1px");
		titleWrap.addComponent(titleLabel);
		
		// noneLabel
		noneLabel = new Label();
		noneLabel.setWidth(100, Unit.PERCENTAGE);
		noneLabel.setHeight("-1px");
		noneLabel.setValue("Label");
		titleWrap.addComponent(noneLabel);
		
		return titleWrap;
	}

	private VerticalLayout buildMappingResultWrap() {
		// common part: create layout
		mappingResultWrap = new VerticalLayout();
		mappingResultWrap.setHeight("-1px");
		
		// idsWrap
		idsWrap = buildIdsWrap();
		mappingResultWrap.addComponent(idsWrap);
		
		// attrsWrap
		attrsWrap = buildAttrsWrap();
		mappingResultWrap.addComponent(attrsWrap);
		
		// groupsWrap
		groupsWrap = buildGroupsWrap();
		mappingResultWrap.addComponent(groupsWrap);
		
		return mappingResultWrap;
	}

	private VerticalLayout buildIdsWrap() {
		// common part: create layout
		idsWrap = new VerticalLayout();
		idsWrap.setHeight("-1px");
		idsWrap.setMargin(false);
	
		// idsTitleLabel
		idsTitleLabel = new Label();
		idsTitleLabel.setWidth(100, Unit.PERCENTAGE);
		idsTitleLabel.setHeight("-1px");
		idsTitleLabel.setValue("Label");
		idsWrap.addComponent(idsTitleLabel);
		
		// idsTable
		idsTable = new Grid<>();
		idsTable.setWidth("100.0%");
		idsTable.setHeight("-1px");
		idsTable.setSizeFull();
		idsWrap.addComponent(idsTable);
		
		return idsWrap;
	}

	private VerticalLayout buildAttrsWrap() {
		// common part: create layout
		attrsWrap = new VerticalLayout();
		attrsWrap.setHeight("-1px");
		attrsWrap.setMargin(false);
		
		// attrsTitleLabel
		attrsTitleLabel = new Label();
		attrsTitleLabel.setWidth(100, Unit.PERCENTAGE);
		attrsTitleLabel.setHeight("-1px");
		attrsTitleLabel.setValue("Label");
		attrsWrap.addComponent(attrsTitleLabel);
		
		// attrsTable
		attrsTable = new Grid<>();
		attrsTable.setWidth("100.0%");
		attrsTable.setHeight("-1px");
		attrsWrap.addComponent(attrsTable);
		
		return attrsWrap;
	}

	private HorizontalLayout buildGroupsWrap() {
		// common part: create layout
		groupsWrap = new HorizontalLayout();
		groupsWrap.setWidth("-1px");
		groupsWrap.setHeight("-1px");
		groupsWrap.setMargin(false);
		//groupsWrap.setSpacing(true);
		
		// groupsTitleLabel
		groupsTitleLabel = new Label();
		groupsTitleLabel.setWidth(100, Unit.PERCENTAGE);
		groupsTitleLabel.setHeight("-1px");
		groupsTitleLabel.setValue("Label");
		groupsWrap.addComponent(groupsTitleLabel);
		
		// groupsLabel
		groupsLabel = new Label();
		groupsLabel.setWidth(100, Unit.PERCENTAGE);
		groupsLabel.setHeight("-1px");
		groupsLabel.setValue("Label");
		groupsWrap.addComponent(groupsLabel);
		
		return groupsWrap;
	}

}
