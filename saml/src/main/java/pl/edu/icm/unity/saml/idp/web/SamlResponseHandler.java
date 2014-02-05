/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.xml.security.utils.Base64;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

import eu.unicore.samly2.exceptions.SAMLServerException;

/**
 * Code used by various components to produce and initialize sending of SAML response.
 * 
 * @author K. Benedyczak
 */
public class SamlResponseHandler
{
	protected FreemarkerHandler freemarkerHandler;
	protected AuthnResponseProcessor samlProcessor;
	protected String thisAddress;
	
	public SamlResponseHandler(FreemarkerHandler freemarkerHandler,
			AuthnResponseProcessor samlProcessor, String contextAddress)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.samlProcessor = samlProcessor;
		this.thisAddress = contextAddress;
	}

	public void handleException(Exception e, boolean destroySession) throws EopException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		returnSamlErrorResponse(respDoc, convertedException, destroySession);
		throw new EopException();
	}
	
	public void returnSamlErrorResponse(ResponseDocument respDoc, SAMLServerException error, boolean destroySession)
	{
		VaadinSession.getCurrent().setAttribute(SessionDisposal.class, 
				new SessionDisposal(error, destroySession));
		VaadinSession.getCurrent().setAttribute(SAMLServerException.class, error);
		returnSamlResponse(respDoc);
	}
	
	public void returnSamlResponse(ResponseDocument respDoc)
	{
		VaadinSession.getCurrent().setAttribute(ResponseDocument.class, respDoc);
		VaadinSession.getCurrent().addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().open(thisAddress, null);		
	}
	
	/**
	 * This handler intercept all messages and checks if there is a SAML response in the session.
	 * If it is present then the appropriate Freemarker page is rendered which redirects the user's browser 
	 * back to the requesting SP.
	 * @author K. Benedyczak
	 */
	public class SendResponseRequestHandler implements RequestHandler
	{
		@Override
		public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
						throws IOException
		{
			ResponseDocument samlResponse = session.getAttribute(ResponseDocument.class);
			if (samlResponse == null)
				return false;
			String assertion = samlResponse.xmlText();
			String encodedAssertion = Base64.encode(assertion.getBytes());
			SessionDisposal error = session.getAttribute(SessionDisposal.class);
			
			SAMLAuthnContext samlCtx = getContext();
			String serviceUrl = samlCtx.getRequestDocument().getAuthnRequest().getAssertionConsumerServiceURL();
			Map<String, String> data = new HashMap<String, String>();
			data.put("SAMLResponse", encodedAssertion);
			data.put("samlService", serviceUrl);
			if (error != null)
				data.put("error", error.getE().getMessage());
			if (samlCtx.getRelayState() != null)
				data.put("RelayState", samlCtx.getRelayState());
			
			cleanContext();
			if (error!= null && error.isDestroySession())
				session.getSession().invalidate();
			response.setContentType("application/xhtml+xml; charset=utf-8");
			PrintWriter writer = response.getWriter();
			freemarkerHandler.process("finishSaml.ftl", data, writer);
			return true;
		}
	}
	
	
	public static SAMLAuthnContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		SAMLAuthnContext ret = (SAMLAuthnContext) httpSession.getAttribute(SamlGuardFilter.SESSION_SAML_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No SAML context in UI");
		return ret;
	}
	
	public static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		vSession.setAttribute(ResponseDocument.class, null);
		WrappedSession httpSession = vSession.getSession();
		httpSession.removeAttribute(SamlGuardFilter.SESSION_SAML_CONTEXT);
	}
	
	private static class SessionDisposal
	{
		private SAMLServerException e;
		private boolean destroySession;
		
		public SessionDisposal(SAMLServerException e, boolean destroySession)
		{
			this.e = e;
			this.destroySession = destroySession;
		}

		protected SAMLServerException getE()
		{
			return e;
		}

		protected boolean isDestroySession()
		{
			return destroySession;
		}
	}
}