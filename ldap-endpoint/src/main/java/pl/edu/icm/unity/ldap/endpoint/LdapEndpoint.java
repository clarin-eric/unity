/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import java.security.cert.X509Certificate;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * LDAP endpoint exposes a stripped LDAP protocol interface to Unity's database.
 */
public class LdapEndpoint extends AbstractEndpoint
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT,
			LdapServerProperties.class);

	private static final String SERVER_WORK_DIRECTORY = "/ldapServer";

	private final SessionManagement sessionMan;
	private final AttributesManagement attributesMan;
	private final EntityManagement identitiesMan;
	private final UnityServerConfiguration mainConfig;
	private final NetworkServer httpServer;
	private final UserMapper userMapper;
	private final PKIManagement pkiManagement;

	private LdapServerProperties configuration;
	private LdapServerFacade ldapServerFacade;

	public LdapEndpoint(NetworkServer server, SessionManagement sessionMan,
			AttributesManagement attributesMan, EntityManagement identitiesMan,
			UnityServerConfiguration mainConfig, UserMapper userMapper,
			PKIManagement pkiManagement)
	{
		this.httpServer = server;
		this.sessionMan = sessionMan;
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.mainConfig = mainConfig;
		this.userMapper = userMapper;
		this.pkiManagement = pkiManagement;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			configuration = new LdapServerProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the LDAP"
					+ " endpoint's configuration", e);
		}
	}

	@Override
	public void start() throws EngineException
	{
		AuthenticatorInstance firstFactorAuthenticator = authenticationFlows.get(0)
				.getFirstFactorAuthenticators().iterator().next();
		LdapSimpleBindRetrieval rpr = (LdapSimpleBindRetrieval) firstFactorAuthenticator.getRetrieval();
		startLdapEmbeddedServer(rpr);
	}

	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticationFlows)
			throws UnsupportedOperationException
	{
	}

	@Override
	public void destroy() throws EngineException
	{
		stopLdapEmbeddedServer();
	}

	private void startLdapEmbeddedServer(LdapSimpleBindRetrieval rpr)
	{
		String host = configuration.getValue(LdapServerProperties.HOST);
		if (null == host || host.isEmpty())
		{
			host = httpServer.getAdvertisedAddress().getHost();
		}
		int port = configuration.getIntValue(LdapServerProperties.LDAP_PORT);

		String workDirectory = new File(
				mainConfig.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY),
				SERVER_WORK_DIRECTORY).getPath();

                String keystoreBaseName = "ldap_certificate";
                String keystoreFileName = new File(workDirectory, keystoreBaseName).getPath();        
                String keystorePassword = "verydifficulytoguesspassword";
                    
                boolean ldapsEnabled = configuration.getBooleanValue(LdapServerProperties.LDAPS_ENABLED);
                
		boolean startTlsEnabled = configuration
				.getBooleanValue(LdapServerProperties.STARTTLS_ENABLED);
                
		String credentialName = configuration.getValue(LdapServerProperties.CREDENTIAL);
		X509Credential credential = null;
                
                if(credentialName != null && !credentialName.isEmpty()) {
                    //X509Certificate cert;
                    try {
                            pkiManagement.getCertificate("MAIN");
                            credential = pkiManagement.getCredential(credentialName);
                    } catch (EngineException e1) {
                            throw new ConfigurationException("Can not access " + credentialName + 
                                            " configured as LDAP server credential", e1);
                    }
                }
                LOG.info("Credential with name {} configued.", credentialName);
                
		
                try {
                    for(String name : pkiManagement.getCertificateNames()) {
                        LOG.info("Found certificate with name: "+name);
                    }
                    X509Certificate cert = pkiManagement.getCertificate("MAIN");
                } catch(EngineException ex) {
                         LOG.error("Failed to enumerate certificate names");
                }
                ////pkiManagement.getCertificate(host)
                
		ldapServerFacade = new LdapServerFacade(host, port, "ldap server interface",
				workDirectory);
		LdapApacheDSInterceptor ladi = new LdapApacheDSInterceptor(rpr, sessionMan,
				this.description.getRealm(), attributesMan, identitiesMan,
				configuration, userMapper, ldapServerFacade, rpr.getAuthenticatorId());

		try
		{
                        boolean startTlsForceConfidentiality = false;
			ldapServerFacade.init(false, ladi, ldapsEnabled, startTlsEnabled, startTlsForceConfidentiality, credential, keystoreBaseName, keystorePassword);
			//if (tlsSupport) {
                        //        LOG.info("Enabling LDAP TLS");
			//	ldapServerFacade.initTLS(false);
                        //}
			ldapServerFacade.start();
		} catch (Exception e)
		{
			throw new ConfigurationException("LDAP ebedded server failed to start", e);
		}
	}

	private void stopLdapEmbeddedServer()
	{
		try
		{
			ldapServerFacade.stop();
		} catch (Exception e)
		{
			LOG.error("LDAP embedded server was not shutdown correctly", e);
		}
	}
}
