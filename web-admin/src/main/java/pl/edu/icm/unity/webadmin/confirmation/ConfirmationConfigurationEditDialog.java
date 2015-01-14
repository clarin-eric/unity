package pl.edu.icm.unity.webadmin.confirmation;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.msgtemplate.MessageTemplateEditor;
import pl.edu.icm.unity.webadmin.msgtemplate.MessageTemplateEditDialog.Callback;
import pl.edu.icm.unity.webui.common.AbstractDialog;

public class ConfirmationConfigurationEditDialog extends AbstractDialog
{
	private ConfirmationConfigurationEditor editor;
	private Callback callback;

	public ConfirmationConfigurationEditDialog(UnityMessageSource msg, String caption,
			Callback callback, ConfirmationConfigurationEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setWidth(50, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		ConfirmationConfiguration cfg = editor.getConfirmationConfiguration();
		if (cfg == null)
			return;
		if (callback.newConfirmationConfiguration(cfg))
			close();

	}

	public interface Callback
	{
		public boolean newConfirmationConfiguration(ConfirmationConfiguration configuration);
	}

}
