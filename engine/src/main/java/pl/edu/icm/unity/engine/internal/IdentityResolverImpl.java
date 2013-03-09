/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Default implementation of the identity resolver. Immutable.
 * @author K. Benedyczak
 */
@Component
public class IdentityResolverImpl implements IdentityResolver
{
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private IdentitiesResolver dbResolver;
	
	@Autowired
	public IdentityResolverImpl(DBSessionManager db, DBAttributes dbAttributes,
			IdentitiesResolver dbResolver)
	{
		super();
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.dbResolver = dbResolver;
	}

	@Override
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes,
			String credentialName) throws IllegalIdentityValueException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = getEntity(identity, identityTypes, sql);
			EntityWithCredential ret = new EntityWithCredential();
			List<Attribute<?>> credAttributes = dbAttributes.getAllAttributes(entityId, "/", 
					SystemAttributeTypes.CREDENTIAL_PREFIX+credentialName, sql);
			if (credAttributes.size() > 0)
			{
				Attribute<?> a = credAttributes.get(0);
				ret.setCredentialValue((String)a.getValues().get(0));
			}
			ret.setCredentialName(credentialName);
			ret.setEntityId(entityId);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private long getEntity(String identity, String[] identityTypes, SqlSession sqlMap)
	{
		for (String identityType: identityTypes)
		{
			EntityParam entityParam = new EntityParam(new IdentityTaV(identity, identityType));
			try
			{
				return dbResolver.getEntityId(entityParam, sqlMap);
			} catch (IllegalIdentityValueException e)
			{
				//ignored
			}
		}
		throw new IllegalIdentityValueException("No identity with value " + identity);
	}

}
