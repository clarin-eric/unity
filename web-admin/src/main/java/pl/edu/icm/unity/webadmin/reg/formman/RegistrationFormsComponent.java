/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormEditDialog.Callback;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;

/**
 * Responsible for registration forms management.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RegistrationFormsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private EventsBus bus;
	
	private GenericElementsTable<RegistrationForm> table;
	private com.vaadin.ui.Component main;
	private ObjectFactory<RegistrationFormEditor> editorFactory;
	
	
	@Autowired
	public RegistrationFormsComponent(UnityMessageSource msg, RegistrationsManagement registrationsManagement,
			ObjectFactory<RegistrationFormEditor> editorFactory,
			RegistrationFormViewer viewer)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.editorFactory = editorFactory;
		this.bus = WebSession.getCurrent().getEventBus();
		
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("RegistrationFormsComponent.caption"));
		table = new GenericElementsTable<RegistrationForm>(msg.getMessage("RegistrationFormsComponent.formsTable"), 
				new GenericElementsTable.NameProvider<RegistrationForm>()
				{
					@Override
					public Label toRepresentation(RegistrationForm element)
					{
						return new Label(element.getName());
					}
				});
		table.setSizeFull();
		table.setMultiSelect(true);
		viewer.setInput(null);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<RegistrationForm> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}
				RegistrationForm item = items.iterator().next();	
				viewer.setInput(item);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new CopyActionHandler());
		table.addActionHandler(new DeleteActionHandler());
				
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setSizeFull();

		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, tableWithToolbar, viewer, 25);
		
		main = hl;
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			List<RegistrationForm> forms = registrationsManagement.getForms();
			table.setInput(forms);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationFormsComponent.errorGetForms"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
	{
		try
		{
			registrationsManagement.updateForm(updatedForm, ignoreRequests);
			bus.fireEvent(new RegistrationFormChangedEvent(updatedForm));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorUpdate"), e);
			return false;
		}
	}

	private boolean addForm(RegistrationForm form)
	{
		try
		{
			registrationsManagement.addForm(form);
			bus.fireEvent(new RegistrationFormChangedEvent(form));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorAdd"), e);
			return false;
		}
	}

	private boolean removeForm(String name, boolean dropRequests)
	{
		try
		{
			registrationsManagement.removeForm(name, dropRequests);
			bus.fireEvent(new RegistrationFormChangedEvent(name));
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<RegistrationForm> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<RegistrationForm> items = new ArrayList<RegistrationForm>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((RegistrationForm) i.getElement());	
		}	
		return items;
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			RegistrationFormEditor editor;
			try
			{
				editor = editorFactory.getObject().init(false);
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
					msg.getMessage("RegistrationFormsComponent.addAction"), new Callback()
					{
						@Override
						public boolean newForm(RegistrationForm form, boolean foo)
						{
							return addForm(form);
						}
					}, editor);
			dialog.show();
		}
	}

	private class EditActionHandler extends CopyEditBaseActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.editAction"), 
					Images.edit.getResource(), false);
		}
	}
	
	private class CopyActionHandler extends CopyEditBaseActionHandler
	{
		public CopyActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.copyAction"), 
					Images.copy.getResource(), true);
		}
	}

	
	private abstract class CopyEditBaseActionHandler extends SingleActionHandler
	{
		private boolean copyMode;
		private String caption;

		public CopyEditBaseActionHandler(String caption, Resource icon, boolean copyMode)
		{
			super(caption, icon);
			this.caption = caption;
			this.copyMode = copyMode;
		}

		@Override
		protected void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<RegistrationForm> item = (GenericItem<RegistrationForm>) target;
			RegistrationForm form =  item.getElement();
			RegistrationFormEditor editor;
			try
			{		
				editor = editorFactory.getObject().init(copyMode);
				editor.setForm(form);
			} catch (Exception e)
			{
				NotificationPopup.showError(msg, msg.getMessage(
						"RegistrationFormsComponent.errorInFormEdit"), e);
				return;
			}
			RegistrationFormEditDialog dialog = new RegistrationFormEditDialog(msg, 
					caption, new Callback()
					{
						@Override
						public boolean newForm(RegistrationForm form, boolean ignoreRequests)
						{
							return copyMode ? addForm(form) :
								updateForm(form, ignoreRequests);
						}
					}, editor);
			dialog.show();		
		}
	}
	
	
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("RegistrationFormsComponent.deleteAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<RegistrationForm> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);

			new ConfirmWithOptionDialog(msg, msg.getMessage("RegistrationFormsComponent.confirmDelete", 
					confirmText),
					msg.getMessage("RegistrationFormsComponent.dropRequests"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean dropRequests)
				{
							for (RegistrationForm item : items)
							{
								removeForm(item.getName(),
										dropRequests);
							}
				}
			}).show();
		}
	}
}
