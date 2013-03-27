/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.webui.VaadinAuthentication;

/**
 * Produces password retrievals for the Vaadin authn binding
 * @author K. Benedyczak
 */
@Component
public class PasswordRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "web-password";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Allows for retrieving password typed in a web widget";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		//TODO
		return null;
	}

	@Override
	public String getSupportedBinding()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
