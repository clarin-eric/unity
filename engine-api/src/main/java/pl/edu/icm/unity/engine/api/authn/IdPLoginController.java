/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This component allows the generic Unity part (not IdP endpoint specific) to obtain information 
 * on the IdP login being performed and forcefully cleaning it. 
 * <p>
 * So far the sole use case is handling of post-registration redirects which must clean authentications in progress
 * before custom redirect (which effectively breaks the authentication protocol). 
 * <p>
 * The class is thread safe. 
 * @author K. Benedyczak
 */
@Component
public class IdPLoginController
{
	private List<IdPLoginHandler> handlers = new ArrayList<>();
	
	@Autowired
	public IdPLoginController(Optional<List<IdPLoginHandler>> handlers)
	{
		this.handlers = handlers.orElseGet(ArrayList::new);
	}

	public synchronized boolean isLoginInProgress()
	{
		for (IdPLoginHandler handler: handlers)
			if (handler.isLoginInProgress())
				return true;
		return false;
	}
	
	public synchronized void breakLogin()
	{
		for (IdPLoginHandler handler: handlers)
			handler.breakLogin();
	}
	
	/**
	 * Implemented by an IdP endpoint and registered with {@link IdPLoginController} to inform it
	 * about authentications against the IdP.
	 * @author K. Benedyczak
	 */
	public interface IdPLoginHandler 
	{
		/**
		 * @return true if IdP of this handler has a login in progress
		 */
		boolean isLoginInProgress();
		
		/**
		 * Breaks any existing login session. Shall ignore an invocation if there is no login in progress.
		 */
		void breakLogin();
	}
}
