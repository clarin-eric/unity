/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Collection;
import java.util.Map;

import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.v7.data.Validator;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.TextArea;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.IllegalVariablesException;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator.MandatoryVariablesException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Component to edit or add message template
 * 
 * @author P. Piernik
 * 
 */
public class MessageTemplateEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private MessageTemplateConsumersRegistry registry;
	private AbstractTextField name;
	private TextArea description;
	private I18nTextField subject;
	private I18nTextArea body;
	private ComboBox consumer;
	private MessageTypeComboBox messageType;
	private Label consumerDescription;
	private boolean editMode;
	private HorizontalLayout buttons;
	private MessageValidator bodyValidator;
	private MessageValidator subjectValidator;
	private AbstractTextField focussedField;
	private MessageTemplateManagement msgTemplateMgr;

	public MessageTemplateEditor(UnityMessageSource msg,
			MessageTemplateConsumersRegistry registry, MessageTemplate toEdit,
			MessageTemplateManagement msgTemplateMgr)
	{
		super();
		this.msgTemplateMgr = msgTemplateMgr;
		editMode = toEdit != null;
		this.msg = msg;
		this.registry = registry;
		initUI(toEdit);

	}

	private void initUI(MessageTemplate toEdit)
	{
		buttons = new HorizontalLayout();
		buttons.setSpacing(false);
		buttons.setMargin(false);
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("MessageTemplatesEditor.name"));
		name.setSizeFull();
		name.setValidationVisible(false);
		description = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.description"));
		consumer = new RequiredComboBox(msg.getMessage("MessageTemplatesEditor.consumer"), msg);
		consumer.setImmediate(true);
		consumer.setValidationVisible(false);
		consumer.setNullSelectionAllowed(false);
		Collection<MessageTemplateDefinition> consumers = registry.getAll();
		for (MessageTemplateDefinition c : consumers)
		{
			consumer.addItem(c.getName());
		}
		consumerDescription = new Label();
		subject = new I18nTextField(msg, msg.getMessage("MessageTemplatesEditor.subject"));
		subject.setImmediate(true);
		subject.setWidth(100, Unit.PERCENTAGE);
		subject.setValidationVisible(false);
		subject.setRequired(true);
		
		
		body = new I18nTextArea(msg, msg.getMessage("MessageTemplatesEditor.body"), 8);
		body.setImmediate(true);
		body.setValidationVisible(false);

		messageType = new MessageTypeComboBox(msg, this::getBodyForPreview);
		subjectValidator = new MessageValidator(null, false);
		bodyValidator = new MessageValidator(null, true);
		subject.addValidator(subjectValidator);
		body.addValidator(bodyValidator);
		body.setRequired(true);

		focussedField = null;
		FocusListener focusListener = event -> {
			Component c = event.getComponent();
			if (c instanceof AbstractTextField)
				focussedField = (AbstractTextField) c;
		};

		subject.addFocusListener(focusListener);
		body.addFocusListener(focusListener);
		
		consumer.addValueChangeListener(event -> {
			setMessageConsumerDesc();
			updateValidator();
			body.setComponentError(null);
			subject.setComponentError(null);
		});

		if (editMode)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			consumer.setValue(toEdit.getConsumer());
			description.setValue(toEdit.getDescription());
			// Using empty locale!
			I18nMessage ms = toEdit.getMessage();
			messageType.setValue(toEdit.getType());
			if (ms != null)
			{
				subject.setValue(ms.getSubject());
				body.setValue(ms.getBody());
			}
			setMessageConsumerDesc();
		} else
		{
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));
			if (consumer.size() > 0)
			{
				consumer.setValue(consumer.getItemIds().toArray()[0]);
			}
			messageType.setValue(MessageType.PLAIN);
		}

		addComponents(name, description, consumer, consumerDescription, buttons, subject,
				messageType, body);
		setSpacing(true);
	}

	private String getBodyForPreview(String locale)
	{
		MessageTemplate tpl = getTemplate();
		MessageTemplate tplPreprocessed;
		try
		{
			tplPreprocessed = msgTemplateMgr.getPreprocessedTemplate(tpl);
		} catch (EngineException e)
		{
			return "Broken template: " + e.toString();
		}
		String value = tplPreprocessed.getMessage().getBody().getValue(locale, null);
		return value != null ? value : "";
	}
	
	public MessageTemplate getTemplate()
	{
		if (!validate())
			return null;
		String n = name.getValue();
		String desc = description.getValue();
		String cons = getConsumer().getName();
		I18nMessage ms = new I18nMessage(subject.getValue(), body.getValue());
		MessageType msgType = messageType.getValue();
		return new MessageTemplate(n, desc, ms, cons, msgType); 
	}

	private void setMessageConsumerDesc()
	{
		MessageTemplateDefinition consumer = getConsumer();
		if (consumer == null)
			return;
		consumerDescription.setValue(msg.getMessage(consumer.getDescriptionKey()));
		updateVarButtons(consumer);
	}

	private void updateVarButtons(MessageTemplateDefinition consumer)
	{
		buttons.removeAllComponents();
		for (Map.Entry<String, MessageTemplateVariable> var : consumer.getVariables().entrySet())
		{
			final Button b = new Button();
			b.addStyleName(Styles.vButtonSmall.toString());
			b.setCaption(var.getKey());
			b.setDescription(msg.getMessage(var.getValue().getDescriptionKey()));
			b.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					if (focussedField != null)
						addVar(focussedField, b.getCaption());
				}
			});
			buttons.addComponent(b);
		}

	}

	private MessageTemplateDefinition getConsumer()
	{

		String c = (String) consumer.getValue();
		MessageTemplateDefinition consumer = null;
		try
		{
			consumer = registry.getByName(c);
		} catch (IllegalArgumentException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("MessageTemplatesEditor.errorConsumers"), e);
			return null;
		}
		return consumer;
	}

	private void addVar(AbstractTextField f, String val)
	{
		String v = f.getValue();
		String st = v.substring(0, f.getCursorPosition());
		String fi = v.substring(f.getCursorPosition());
		f.setValue(st + "${" + val + "}" + fi);
	}

	private void updateValidator()
	{
		MessageTemplateDefinition c = getConsumer();
		if (c != null)
		{
			subjectValidator.setConsumer(c);
			bodyValidator.setConsumer(c);
		}
	}

	private boolean validate()
	{
		updateValidator();
		name.setValidationVisible(true);
		consumer.setValidationVisible(true);
		subject.setValidationVisible(true);
		subject.setValidationVisible(true);
		body.setValidationVisible(true);
		return name.isValid() && consumer.isValid() && subject.isValid() && body.isValid();

	}

	private class MessageValidator implements Validator
	{
		private MessageTemplateDefinition c;
		private boolean checkMandatory;
		
		public MessageValidator(MessageTemplateDefinition c, boolean checkMandatory)
		{
			this.c = c;
			this.checkMandatory = checkMandatory;
		}

		public void setConsumer(MessageTemplateDefinition c)
		{
			this.c = c;
		}

		@Override
		public void validate(Object value) throws InvalidValueException
		{
			try
			{
				MessageTemplateValidator.validateText(c, value.toString(), checkMandatory);
			} catch (IllegalVariablesException e)
			{
				throw new InvalidValueException(msg.getMessage("MessageTemplatesEditor.errorUnknownVars", 
						e.getUnknown().toString()));
			} catch (MandatoryVariablesException e)
			{
				throw new InvalidValueException(msg.getMessage("MessageTemplatesEditor.errorMandatoryVars", 
						e.getMandatory().toString()));
			}
		}
	}
}
