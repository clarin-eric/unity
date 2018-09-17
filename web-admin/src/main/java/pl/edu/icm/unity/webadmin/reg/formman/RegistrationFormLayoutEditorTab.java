/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.function.Supplier;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.webadmin.reg.formman.layout.FormLayoutEditor;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Registration primary and secondary layouts editor. Allows for selecting
 * whether the default layout should be used or not.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationFormLayoutEditorTab extends CustomComponent
{
	private UnityMessageSource msg;
	private Supplier<RegistrationForm> formProvider;
	private CheckBox enableCustomLayout;
	private FormLayoutEditor primaryLayoutEditor;
	private FormLayoutEditor secondaryLayoutEditor;
	private CssLayout layouts;
	private boolean isInitialValueSet = false;

	public RegistrationFormLayoutEditorTab(UnityMessageSource msg, Supplier<RegistrationForm> formProvider)
	{
		super();
		this.msg = msg;
		this.formProvider = formProvider;

		initUI();
	}

	private void initUI()
	{
		layouts = new CssLayout();
		primaryLayoutEditor = new FormLayoutEditor(msg, () -> formProvider.get().getEffectivePrimaryFormLayout(msg));
		primaryLayoutEditor.setWidth(600, Unit.PIXELS);
		secondaryLayoutEditor = new FormLayoutEditor(msg, () -> formProvider.get().getEffectiveSecondaryFormLayout(msg));
		secondaryLayoutEditor.setWidth(600, Unit.PIXELS);
		Panel primaryLayoutPanel = new Panel(msg.getMessage("RegistrationFormEditor.primaryLayout"), primaryLayoutEditor);
		primaryLayoutPanel.setStyleName(Styles.bottomMargin.toString());
		primaryLayoutPanel.setStyleName(Styles.rightMargin.toString());
		primaryLayoutPanel.setSizeUndefined();
		Panel secondaryLayoutPanel = new Panel(msg.getMessage("RegistrationFormEditor.secondaryLayout"), secondaryLayoutEditor);
		secondaryLayoutPanel.setSizeUndefined();
		layouts.addComponents(primaryLayoutPanel, secondaryLayoutPanel);
		
		enableCustomLayout = new CheckBox(msg.getMessage("FormLayoutEditor.enableCustom"));
		enableCustomLayout.addValueChangeListener(event -> onEnableCustomLayout(event.getValue()));
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(true);
		main.setSpacing(true);
		main.addComponents(enableCustomLayout, layouts);
		main.setComponentAlignment(layouts, Alignment.TOP_CENTER);
		setCompositionRoot(main);
	}

	private void onEnableCustomLayout(boolean isCustomLayout)
	{
		layouts.setVisible(isCustomLayout);
		if (isCustomLayout && isInitialValueSet)
		{
			setLayoutFromProvider();
		}
	}
	
	public RegistrationFormLayouts getLayouts()
	{
		updateFromForm();
		return getCurrentLayouts();
	}
	
	public RegistrationFormLayouts getCurrentLayouts()
	{
		RegistrationFormLayouts layouts = new RegistrationFormLayouts();
		if (enableCustomLayout.getValue())
		{
			layouts.setPrimaryLayout(primaryLayoutEditor.getLayout());
			layouts.setSecondaryLayout(secondaryLayoutEditor.getLayout());
		} else
		{
			layouts.setPrimaryLayout(null);
			layouts.setSecondaryLayout(null);
		}
		return layouts;
	}

	public void setFormLayouts(RegistrationFormLayouts formLayouts)
	{
		boolean isCustomLayoutEnabled = false;
		if (formLayouts.getPrimaryLayout() == null && formLayouts.getSecondaryLayout() == null)
		{
			isCustomLayoutEnabled = false;
			primaryLayoutEditor.setLayout(null);
			secondaryLayoutEditor.setLayout(null);
		} else
		{
			isCustomLayoutEnabled = true;
			primaryLayoutEditor.setLayout(formLayouts.getPrimaryLayout());
			secondaryLayoutEditor.setLayout(formLayouts.getSecondaryLayout());
		}
		if (!isInitialValueSet)
		{
			enableCustomLayout.setValue(isCustomLayoutEnabled);
			isInitialValueSet = true;
		}
	}
	
	private void setLayoutFromProvider()
	{
		if (!enableCustomLayout.getValue())
			return;
		primaryLayoutEditor.setLayoutFromProvider();
		secondaryLayoutEditor.setLayoutFromProvider();
	}
	
	public void updateFromForm()
	{
		RegistrationForm form = formProvider.get();
		if (form == null)
			return;
		
		RegistrationFormLayouts layouts = getCurrentLayouts();
		layouts.setLocalSignupEmbeddedAsButton(form.getFormLayouts().isLocalSignupEmbeddedAsButton());
		FormLayoutUtils.updateRegistrationFormLayout(layouts, form);
		setFormLayouts(layouts);
	}

}
