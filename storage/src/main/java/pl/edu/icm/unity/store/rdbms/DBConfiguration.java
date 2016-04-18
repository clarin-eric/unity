/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Database configuration
 * @author K. Benedyczak
 */
@Component
public class DBConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, DBConfiguration.class);
	public enum Dialect {h2, mysql, psql};

	@DocumentationReferencePrefix
	public static final String PREFIX = "unityServer.db.";
	
	public static final String DBCONFIG_FILE = "mapconfigFile";
	public static final String IGNORE_ALTERNATIVE_DB_CONFIG = "ignoreAlternativeDbConfig";
	public static final String MAX_POOL_SIZE = "maxConnectionPoolSize";
	public static final String MIN_POOL_SIZE = "minConnectionPoolSize";
	public static final String MAX_IDLE_CONNECTION_TIME = "maxIdleConnectionLifetime";
	
	/**
	 * System property: if set it is providing an alternative path to a file with DB configuration.
	 * It can also accept predefined values 'h2' or 'mysql' which load default h2 or mysql configurations. 
	 */
	public static final String ALTERNATIVE_DB_CONFIG = "unityDbConfig";
	
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META;
	static 
	{
		META = DBPropertiesHelper.getMetadata(Driver.class, "jdbc:h2:file:./data/unitydb.bin", 
				Dialect.h2, "");
		META.put(DBCONFIG_FILE, new PropertyMD().setPath().setHidden().
				setDescription("Path of the low level database file with mappings configuration."));
		META.put(MAX_IDLE_CONNECTION_TIME, new PropertyMD("1800").
				setDescription("Time in seconds after which an idle connection "
						+ "is closed (and recreated if needed). Set to 0 to disable. "
						+ "This setting is needed if stale connections become unoperational "
						+ "what unfortunately do happen."));
		META.put(MAX_POOL_SIZE, new PropertyMD("10").
				setDescription("Maximum number of DB connections allowed in pool"));
		META.put(MIN_POOL_SIZE, new PropertyMD("1").
				setDescription("Minimum number of DB connections to be kept in pool"));
		
		META.put(IGNORE_ALTERNATIVE_DB_CONFIG, new PropertyMD("false").setHidden().
				setDescription("For unity tests: if set in the main configuration then the system "
						+ "property with alternative DB config is ignored. It is useful "
						+ "when test case works only with specific DB configuration and"
						+ "manual, general purpose config has no sense."));
	}
	
	@Autowired
	public DBConfiguration(StoragePropertiesSource src) throws ConfigurationException
	{
		super(PREFIX, loadDbConfig(src.getProperties()), META, log);
	}
	
	private static Properties loadDbConfig(Properties main)
	{
		String alternativeDB = System.getProperty(ALTERNATIVE_DB_CONFIG);
		String ignoreAlt = main.getProperty(PREFIX+IGNORE_ALTERNATIVE_DB_CONFIG);
		if (alternativeDB == null || "true".equals(ignoreAlt))
			return main;
		Properties p = new Properties();
		InputStream is;
		File f = new File(alternativeDB);
		if (f.exists() && f.isFile())
		{
			log.info("Loading alternative DB config from file " + alternativeDB);
			try
			{
				is = new FileInputStream(f);
			} catch (FileNotFoundException e)
			{
				throw new ConfigurationException("Cannot read DB config file", e);
			}
		} else
		{
			String path = "/dbConfigs/" + alternativeDB + ".conf";
			log.info("Loading alternative DB config from classpath resource " + path);
			is = DBConfiguration.class.getResourceAsStream(path);
		}

		try
		{
			Reader isReader = new BufferedReader(new InputStreamReader(is, "UTF-8")); 
			p.load(isReader);
		} catch (Exception e)
		{
			throw new ConfigurationException("Cannot load alternative DB config", e);
		} finally
		{
			try
			{
				is.close();
			} catch (IOException e)
			{
				log.error("Problem closing file", e);
			}
		}
		return p;
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
