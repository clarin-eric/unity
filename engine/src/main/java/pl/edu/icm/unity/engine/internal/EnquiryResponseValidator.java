/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;

/**
 * Helper component with methods to validate {@link EnquiryResponse}.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseValidator extends BaseRequestValidator
{
	public void validateSubmittedResponse(EnquiryForm form, EnquiryResponse response,
			boolean doCredentialCheckAndUpdate, SqlSession sql) throws EngineException
	{
		super.validateSubmittedRequest(form, response, doCredentialCheckAndUpdate, sql);
	}

	public void validateTranslatedRequest(EnquiryForm form, EnquiryResponse response, 
			TranslatedRegistrationRequest request, SqlSession sql) throws EngineException
	{
		validateFinalAttributes(request.getAttributes(), sql);
		validateFinalCredentials(response.getCredentials(), sql);
		validateFinalIdentities(request.getIdentities(), sql);
	}
}
