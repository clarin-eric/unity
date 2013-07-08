/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationProcessorUtil;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * TODO - this is far from being complete: needs to support remote unresolved entities and
 * support fragments.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationProcessor.class);
	
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	
	@Autowired
	public AuthenticationProcessor(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg)
	{
		super();
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.credEditorReg = credEditorReg;
	}

	public void processResults(List<AuthenticationResult> results) throws AuthenticationException
	{
		AuthenticatedEntity logInfo = AuthenticationProcessorUtil.processResults(results);
		WrappedSession session = logged(logInfo);

		if (logInfo.isUsedOutdatedCredential())
		{
			showCredentialUpdate();
			return;
		}
		redirectToOrigin(session);
	}
	
	private void showCredentialUpdate()
	{
		OutdatedCredentialDialog dialog = new OutdatedCredentialDialog(msg, authnMan, idsMan, credEditorReg);
		dialog.show();
	}
	
	private static WrappedSession logged(AuthenticatedEntity authenticatedEntity) throws AuthenticationException
	{
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			log.error("BUG: Can't get VaadinSession to store authenticated user's data.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		WrappedSession session = vss.getSession();
		session.setAttribute(WebSession.USER_SESSION_KEY, authenticatedEntity);
		return session;
	}
	
	private static void redirectToOrigin(WrappedSession session) throws AuthenticationException
	{
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		String origURL = getOriginalURL(session);
		
		ui.getPage().open(origURL, "");
	}
	
	public static String getOriginalURL(WrappedSession session) throws AuthenticationException
	{
		String origURL = (String) session.getAttribute(AuthenticationFilter.ORIGINAL_ADDRESS);
		//String origFragment = (String) session.getAttribute(AuthenticationApp.ORIGINAL_FRAGMENT);
		if (origURL == null)
			throw new AuthenticationException("AuthenticationProcessor.noOriginatingAddress");
		//if (origFragment == null)
		//	origFragment = "";
		//else
		//	origFragment = "#" + origFragment;
		
		//origURL = origURL+origFragment;
		return origURL;
	}
	
	public static void logout()
	{
		VaadinSession vs = VaadinSession.getCurrent();
		WrappedSession s = vs.getSession();
		Page p = Page.getCurrent();
		URI currentLocation = p.getLocation();
		s.invalidate();
		p.setLocation(currentLocation);
	}
	
	/**
	 * Doesn't destroy the session, instead only clears information about logged user, so authN screen should be shown.
	 */
	public static void softLogout()
	{
		VaadinSession vs = VaadinSession.getCurrent();
		WrappedSession s = vs.getSession();
		s.removeAttribute(WebSession.USER_SESSION_KEY);
		Page p = Page.getCurrent();
		URI currentLocation = p.getLocation();
		p.setLocation(currentLocation);
	}
}
