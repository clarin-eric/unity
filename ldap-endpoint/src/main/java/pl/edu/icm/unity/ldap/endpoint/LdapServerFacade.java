/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.*;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.logging.log4j.core.config.ConfigurationException;

import eu.emi.security.authn.x509.X509Credential;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Note: default settings contain one system user with "cn=system
 * administrator".
 */
public class LdapServerFacade
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapServerFacade.class);
        
	private LdapServer impl;

	private DirectoryService ds;

	private String host;

	private int port;

	private String name;

	private String workdir;

	//If true allows for loading of schemas with errors, otherwise any schema with errors will fail to start the LDAP
	//endpoint
	private boolean relaxedSchemaLoading;

	public String partitionResource = "default.zip";
	public String partitionExtResource = "ext.zip";

	public LdapServerFacade(String host, int port, String nameOrNull, String workdir) {
		this(host, port, nameOrNull, workdir, false);
	}

	public LdapServerFacade(String host, int port, String nameOrNull, String workdir, boolean relaxedSchemaLoading) {
		this.host = host;
		this.port = port;
		this.name = nameOrNull;
		this.workdir = workdir;
		this.relaxedSchemaLoading = relaxedSchemaLoading;

		this.impl = null;
		this.ds = null;
	}

	public DirectoryService getDs()
	{
		return ds;
	}

	public Attribute getAttribute(String uid, String upId) throws LdapException {
		if (ds == null) {
			throw new NullPointerException("DirectoryService is null");
		}
		AttributeType at = ds.getSchemaManager()
				.lookupAttributeTypeRegistry(null == upId ? uid : upId);
		Attribute da = new DefaultAttribute(uid, at);
		return da;
	}

	/**
	 * Initialise the LDAP server and the directory service.
	 *
	 * @param deleteWorkDir
	 *                - should we reuse the LDAP initialisation data
	 * @param interceptor
	 *                - unity's LDAP interceptor
         * @param credential
         * @param startTlsSupport
         * @throws java.lang.Exception
	 */
	public void init(boolean deleteWorkDir, BaseInterceptor interceptor, boolean ldapsEnabled, boolean startTlsEnabled, boolean startTlsForceConfidentiality, X509Credential credential, String keystoreFileName, String keystorePassword) throws Exception {
		impl = new UnityLdapServer(credential);
		impl.setServiceName(name);
		TcpTransport transport = new TcpTransport(host, port);

		if(ldapsEnabled || startTlsEnabled) {
			 if(keystoreFileName != null && !keystoreFileName.isEmpty() && keystorePassword != null && !keystorePassword.isEmpty()) {
				impl.setKeystoreFile(LdapServerKeys.getKeystore(keystoreFileName, keystorePassword).getAbsolutePath());
				impl.setCertificatePassword(keystorePassword);
			}
		}
		if(ldapsEnabled) { //LDAP over SSL
			transport.setEnableSSL(true);
		} else if (startTlsEnabled) { //Plain LDAP with STARTTLS support
			StartTlsHandler handler = new StartTlsHandler();
			impl.addExtendedOperationHandler(handler);
			impl.setConfidentialityRequired(startTlsForceConfidentiality);
		}
		LOG.info("Enabling {} on port: {}", ldapsEnabled ? "LDAPS" : "LDAP", port);
		if(!ldapsEnabled) {
			LOG.info("Starttls={}, force confidentiality=", startTlsEnabled, startTlsForceConfidentiality);
		}
		impl.setTransports(transport);
                
		ds = new DefaultDirectoryService();
		ds.getChangeLog().setEnabled(false);
		// ?
		ds.setDenormalizeOpAttrsEnabled(true);
		// prepare the working dir with all the required settings
		boolean shouldStartClose = prepareWorkDir(deleteWorkDir);

		// load the required data
		loadData();
		// see https://issues.apache.org/jira/browse/DIRSERVER-1954
		if (shouldStartClose) {
			ds.startup();
			ds.shutdown();
		}
		impl.setDirectoryService(ds);
		interceptor.init(ds);
                
		// "inject" unity's code                
		setUnityInterceptor(interceptor);
	}

	/**
	 * Enable TLS support
	 */
	public void initTLS(boolean forceTls) {
		try {
			StartTlsHandler handler = new StartTlsHandler();
			impl.addExtendedOperationHandler(handler);
			impl.setConfidentialityRequired(forceTls);
		} catch (Exception e) {
			throw new ConfigurationException("Can not initialize TLS", e); 
		}
	}

	public String getAdminDN()
	{
		return ServerDNConstants.ADMIN_SYSTEM_DN;
	}

	/**
	 * Apache DS has to be initialised with schemas etc. - this directory
	 * will be used to hold all the required configuration which is then
	 * loaded
	 *
	 * @return True if the work directory has been cleaned
	 */
	private boolean prepareWorkDir(boolean delete_work_dir) throws IOException {
		boolean fromScratch = false;
		File workdirF = new File(workdir);
		if (delete_work_dir && workdirF.exists()) {
			FileUtils.deleteDirectory(workdirF);
		}
		boolean shouldExtract = !workdirF.exists();
		ds.setInstanceLayout(new InstanceLayout(workdirF));

		if (shouldExtract) {
			// indicate that we start for the first time
			fromScratch = true;
			if(!extractPartitionResource(workdirF, partitionResource)) {
				throw new IOException("Failed to extract default LDAP schema from "+partitionExtResource);
			}
			if(!extractPartitionResource(workdirF, partitionExtResource)) {
				LOG.warn("Failed to extract LDAP schema extension. "+partitionExtResource+" was not found.");
			}
		}

		return fromScratch;
	}

	private boolean extractPartitionResource(File workdir, String resource) throws IOException {
		// copy resources (kind-of minimalistic version from apacheds) to the destination directory
		InputStream jarZipIs = LdapServerFacade.class.getClassLoader().getResourceAsStream(resource);
		if(jarZipIs == null) {
			return false;
		}
		ZipInputStream zis = new ZipInputStream(jarZipIs);
		try {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File entryDestination = new File(workdir, entry.getName());
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					entryDestination.getParentFile().mkdirs();
					OutputStream out = new FileOutputStream(entryDestination);
					IOUtils.copy(zis, out);
					out.close();
				}
			}
		} finally {
			zis.close();
		}

		return true;
	}

	/**
	 * Load data required by the ldap directory implementation from a
	 * directory.
	 */
	private void loadData() throws Exception {
		File schemaPartitionDirectory = new File(ds.getInstanceLayout().getPartitionsDirectory(), "schema");
		SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
		SchemaManager schemaManager = new DefaultSchemaManager(loader);
		if(this.relaxedSchemaLoading) {
			schemaManager.setRelaxed();
		}
		schemaManager.loadAllEnabled();
		ds.setSchemaManager(schemaManager);

		int num_errors = schemaManager.getErrors().size();
		if(schemaManager.isStrict() && num_errors > 0) {
			LOG.error("Failed to start LDAP endpoint. Schema manager in strict mode, aborting with errors > 0 (errors="+num_errors+")");
			for(Throwable t : schemaManager.getErrors()) {
				LOG.error("\t"+t.getMessage());
			}
			throw new Exception("Failed to load schema data. Schema manager in strict mode encountered "+num_errors+" errors");
		}

		LdifPartition schemaLdifPartition = new LdifPartition(schemaManager, ds.getDnFactory());
		schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
		SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
		schemaPartition.setWrappedPartition(schemaLdifPartition);
		ds.setSchemaPartition(schemaPartition);

		JdbmPartition systemPartition = new JdbmPartition(ds.getSchemaManager(), ds.getDnFactory());
		systemPartition.setId("system");
		systemPartition.setPartitionPath(
				new File(ds.getInstanceLayout().getPartitionsDirectory(),
				systemPartition.getId()).toURI());
		systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
		systemPartition.setSchemaManager(ds.getSchemaManager());

		ds.setSystemPartition(systemPartition);
	}

	private void setUnityInterceptor(BaseInterceptor injectedInterceptor) {
		List<Interceptor> interceptors = ds.getInterceptors();
		// find Normalization interceptor in chain
		int insertionPosition = -1;
		for (int pos = 0; pos < interceptors.size(); ++pos) {
			Interceptor interceptor = interceptors.get(pos);
			if (interceptor instanceof NormalizationInterceptor) {
				insertionPosition = pos;
				break;
			}
		}
		// insert our new interceptor just behind
		interceptors.add(insertionPosition + 1, injectedInterceptor);
		ds.setInterceptors(interceptors);
	}

	/**
	 * Start the directory service and the embedded ldap server
	 */
	public void start() throws Exception {
		LOG.info("LdapServerFacade.start()");
		ds.startup();
		impl.start();
	}

	public void stop() throws Exception {
		impl.stop();
		ds.shutdown();
	}
}
