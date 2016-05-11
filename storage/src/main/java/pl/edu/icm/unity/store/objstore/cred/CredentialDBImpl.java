/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.DependencyNotificationManager;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Easy access to {@link CredentialDefinition} storage.
 * @author K. Benedyczak
 */
@Component
public class CredentialDBImpl extends GenericObjectsDAOImpl<CredentialDefinition> implements CredentialDB
{
	@Autowired
	public CredentialDBImpl(CredentialHandler handler,
			ObjectStoreDAO dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, CredentialDefinition.class,
				"credential");
	}
}
