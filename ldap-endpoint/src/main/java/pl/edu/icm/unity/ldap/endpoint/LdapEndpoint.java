/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.server.JettyServer;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * LDAP endpoint exposes a stripped LDAP protocol interface to Unity's database.
 */
public class LdapEndpoint extends AbstractEndpoint
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapServerProperties.class);
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
			PKIManagement pkiManagement) {
		this.httpServer = server;
		this.sessionMan = sessionMan;
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.mainConfig = mainConfig;
		this.userMapper = userMapper;
		this.pkiManagement = pkiManagement;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState) {
		properties = new Properties();
		try {
			properties.load(new StringReader(serializedState));
			configuration = new LdapServerProperties(properties);
		} catch (Exception e) {
			throw new ConfigurationException("Can't initialize the the LDAP"
					+ " endpoint's configuration", e);
		}
	}

	@Override
	public void start() throws EngineException {
		try {
			AuthenticatorInstance firstFactorAuthenticator = authenticationFlows.get(0)
				.getFirstFactorAuthenticators().iterator().next();
			LdapSimpleBindRetrieval rpr = (LdapSimpleBindRetrieval) firstFactorAuthenticator.getRetrieval();
			startLdapEmbeddedServer(rpr);
		} catch(Exception ex) {
			LOG.error("Failed to start endpoint", ex);
			throw new EngineException(ex);
		}
	}

	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticationFlows) throws UnsupportedOperationException {
	}

	@Override
	public void destroy() throws EngineException {
		stopLdapEmbeddedServer();
	}

	private String getAdvertisedHostFromServer(NetworkServer httpServer) {
		//host = httpServer.getAdvertisedAddress().getHost();
		JettyServer server = (JettyServer)httpServer;
		URL[] urls = server.getUrls();
		if(urls != null && urls.length > 0) {
			return urls[0].getHost();
		}
		return null;
	}

	private void startLdapEmbeddedServer(LdapSimpleBindRetrieval rpr) throws Exception {
		String host = configuration.getValue(LdapServerProperties.HOST);
		if (null == host || host.isEmpty()) {
			host = getAdvertisedHostFromServer(httpServer);
		}
		if (null == host || host.isEmpty()) {
			throw new Exception("Host not found in server configuration (property name = ) and no advertised url found.");
		}
		int port = configuration.getIntValue(LdapServerProperties.LDAP_PORT);

		File workDirectoryFile = new File(mainConfig.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY), SERVER_WORK_DIRECTORY);
		/*
		if(!workDirectoryFile.exists()) {
			if(!workDirectoryFile.mkdirs()) {
				throw new Exception("Failed to create ldap server work directory: " + workDirectoryFile.getPath());
			} else {
				LOG.info("Created ldap server work directory: " + workDirectoryFile.getPath());
			}
		}
		*/
		String keystoreBaseName = "ldap_certificate";
		String keystoreFileName = new File(workDirectoryFile.getPath(), keystoreBaseName).getPath();
		String keystorePassword = "verydifficulytoguesspassword";

		boolean ldapsEnabled = configuration.getBooleanValue(LdapServerProperties.LDAPS_ENABLED);
		boolean startTlsEnabled = configuration.getBooleanValue(LdapServerProperties.STARTTLS_ENABLED);
		boolean startTlsForceConfidentiality = configuration.getBooleanValue(LdapServerProperties.STARTTLS_FORCE_CONFIDENTIALITY);

		/*
		X509Credential credential = null;

		LOG.info("ldapsEnabled={}, startTlsEnabled={}, startTlsForceConfidentiality={}", ldapsEnabled, startTlsEnabled, startTlsForceConfidentiality);
		//Load a credential if ldaps or starttls is enabled or throw an exception if loading of the credential fails
		if(ldapsEnabled || startTlsEnabled) {
			String credentialName = configuration.getValue(LdapServerProperties.CREDENTIAL);
			boolean hasCredentialName = credentialName != null && !credentialName.isEmpty();
			if (!hasCredentialName) {
				credential = pkiManagement.getMainAuthnAndTrust().getCredential();
				pkiManagement.getMainAuthnAndTrust().getValidator();
				if(credential == null) {
					throw new ConfigurationException("Main credential is required when ldaps or starttls is enabled and no alternative credential is configured");
				} else {
					LOG.info("Main credential configured for ldaps or starttls.");
				}

			} else {
				credential = pkiManagement.getCredential(credentialName);
				if(credential == null) {
					throw new ConfigurationException("Credential with name = " + credentialName + " is required when ldaps or starttls is enabled");
				} else {
					LOG.info("Credential with name {} configured for ldaps or starttls.", credentialName);
				}
			}

		}

		 */
                /*


                
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
                LOG.info("Credential with name {} configured.", credentialName);
                */
		/*
                try {
                    //for(String name : pkiManagement.getAllCertificateNames()) {
					//for(String name : pkiManagement.getAllCertificateNamesWithoutAuthz()) {
                    //    LOG.info("Found certificate with name: "+name);
                    //}



                    NamedCertificate cert = pkiManagement.getCertificateWithoutAuthz(credentialName);
                } catch(EngineException ex) {
                         LOG.error("Failed to enumerate certificate names");
					throw new ConfigurationException("Can not access " + credentialName +
							" configured as LDAP server credential", ex);
                }
		*/
				////pkiManagement.getCertificate(host)

		boolean relaxedSchemaLoading = configuration.getBooleanValue(LdapServerProperties.RELAXED_SCHEMA_LOADING);
		ldapServerFacade = new LdapServerFacade(host, port, "ldap server interface", workDirectoryFile.getPath(), relaxedSchemaLoading);
		LdapApacheDSInterceptor ladi = new LdapApacheDSInterceptor(rpr, sessionMan,
				this.description.getRealm(), attributesMan, identitiesMan,
				configuration, userMapper, ldapServerFacade, rpr.getAuthenticatorId());

		try {
			ldapServerFacade.init(false, ladi, ldapsEnabled, startTlsEnabled, startTlsForceConfidentiality, keystoreFileName, keystorePassword);
			ladi.init(ldapServerFacade.getDs());
			ldapServerFacade.start();
		} catch (Exception e) {
			throw new ConfigurationException("LDAP embedded server failed to start", e);
		}
	}

	private void stopLdapEmbeddedServer() {
		try {
			ldapServerFacade.stop();
		} catch (Exception e) {
			LOG.error("LDAP embedded server was not shutdown correctly", e);
		}
	}
}
