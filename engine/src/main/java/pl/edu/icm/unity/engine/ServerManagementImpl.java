/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.InitDB;
import pl.edu.icm.unity.db.export.ImportExport;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Implementation of general maintenance.
 * @author K. Benedyczak
 */
@Component
public class ServerManagementImpl implements ServerManagement
{
	private Logger log = Log.getLogger(Log.U_SERVER, ServerManagementImpl.class);
	private DBSessionManager db;
	private ImportExport dbDump;
	private InitDB initDb;
	private EngineInitialization engineInit;
	private JettyServer httpServer;
	private AuthorizationManager authz;
	
	@Autowired
	public ServerManagementImpl(DBSessionManager db, ImportExport dbDump, InitDB initDb,
			EngineInitialization engineInit, JettyServer httpServer,
			AuthorizationManager authz, ExecutorsService executorsService)
	{
		this.db = db;
		this.dbDump = dbDump;
		this.initDb = initDb;
		this.engineInit = engineInit;
		this.httpServer = httpServer;
		this.authz = authz;
		executorsService.getService().scheduleWithFixedDelay(new ClenupDumpsTask(), 20, 60, TimeUnit.SECONDS);
	}


	@Override
	public void resetDatabase() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		initDb.resetDatabase();
		httpServer.undeployAllEndpoints();
		engineInit.initializeDatabaseContents();
	}


	@Override
	public File exportDb() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			File ret;
			try
			{
				ret = dbDump.exportDB(sql);
			} catch (JsonGenerationException e)
			{
				throw new InternalException("Error creating JSON from database contents", e);
			} catch (IOException e)
			{
				throw new InternalException("Error writing database contents to disk", e);
			}
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}


	@Override
	public void importDb(File from, boolean resetIndexes) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			initDb.deleteEverything(sql, resetIndexes);
			try
			{
				dbDump.importDB(from, sql);
			} catch (Exception e)
			{
				throw new InternalException("Database import failed. " +
						"Database should not be changed.", e);
			}
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		httpServer.undeployAllEndpoints();
		engineInit.initializeDatabaseContents();
	}
	
	private class ClenupDumpsTask implements Runnable
	{
		private static final long DUMP_STORE_TIME = 600000;
		
		@Override
		public void run()
		{
			File exportsDirectory = dbDump.getExportDirectory();
			File[] files = exportsDirectory.listFiles();
			long now = System.currentTimeMillis();
			
			for (File file: files)
			{
				if (file.lastModified() + DUMP_STORE_TIME < now)
				{
					log.debug("Removing the old, temporary, database dump from the workspace: " 
							+ file);
					file.delete();
					continue;
				}
			}
		}
	}
}
