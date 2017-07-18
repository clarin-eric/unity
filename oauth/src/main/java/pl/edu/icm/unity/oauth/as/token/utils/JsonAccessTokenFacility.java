/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.JsonBytesFacility;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map access token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class JsonAccessTokenFacility extends JsonBytesFacility
{

	@Override
	public String getDescription()
	{
		return "Access token JSON formatter";
	}
	
	@Override
	public String getName()
	{
		return OAuthProcessor.INTERNAL_ACCESS_TOKEN;
	}

}
