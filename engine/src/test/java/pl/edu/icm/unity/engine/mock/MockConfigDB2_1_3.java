/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.server.utils.ConfigurationLocationProvider;

public class MockConfigDB2_1_3 implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return "src/test/resources/dbUpdate/to2_1_3/unityServer.conf";
	}
}
