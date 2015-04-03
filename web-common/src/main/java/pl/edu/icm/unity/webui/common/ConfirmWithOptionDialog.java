/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Confirmation dialog, witch an additional checkbox. Useful for situations when confirmation
 * is conditional, e.g. user can confirm and additionally choose the recursive delete.
 * 
 * @author K. Benedyczak
 */
public class ConfirmWithOptionDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private Callback callback;
	private String question;
	private String option;
	private CheckBox optionCb;

	public ConfirmWithOptionDialog(UnityMessageSource msg, String question, String option, Callback callback) 
	{
		this(msg, msg.getMessage("ConfirmDialog.confirm"), question, option, callback);
	}
	
	public ConfirmWithOptionDialog(UnityMessageSource msg, String caption, String question, String option,
			Callback callback) 
	{
		super(msg, caption);
		this.question = question;
		this.callback = callback;
		this.option = option;
		setSizeMode(SizeMode.SMALL);
	}

	public interface Callback 
	{
		public void onConfirm(boolean optionSelected);
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.addComponent(new Label(question));
		optionCb = new CheckBox(option);
		vl.addComponent(optionCb);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onConfirm(optionCb.getValue());
	}
}