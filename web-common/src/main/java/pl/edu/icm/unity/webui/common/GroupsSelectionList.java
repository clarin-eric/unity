/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.engine.api.GroupsManagement;


/**
 * {@link TwinColSelect} allowing to choose a set of groups. This component can automatically populate the list 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupsSelectionList extends TwinColSelect<String>
{
	private Collection<String> groups;
	private GroupsManagement groupsMan;
	private List<String> processedGroups = new ArrayList<>();

	public GroupsSelectionList(String caption, Collection<String> groups)
	{
		super(caption);
		this.groups = groups;
		initContent();
	}

	public GroupsSelectionList(String caption, GroupsManagement groupsMan)
	{
		super(caption);
		this.groupsMan = groupsMan;
		initContent();
	}
	
	public Collection<String> getSelectedGroups()
	{
		return getValue();
	}
	
	public void setSelectedGroups(Collection<String> groups)
	{
		setValue(Sets.newHashSet(groups));
	}

	public List<String> getAllGroups()
	{
		return new ArrayList<>(processedGroups);
	}

	public void setInput(String rootGroup, boolean inclusive)
	{
		processedGroups = GroupSelectionUtils.establishGroups(
				rootGroup, inclusive, groupsMan, groups);
		setItems(processedGroups);
	}

	private void initContent()
	{
		setRows(5);
	}
}
