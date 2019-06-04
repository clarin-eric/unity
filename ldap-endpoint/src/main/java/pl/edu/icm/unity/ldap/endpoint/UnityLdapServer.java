/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.security.Security;

import javax.net.ssl.KeyManagerFactory;

import org.apache.directory.server.ldap.LdapServer;

import eu.emi.security.authn.x509.X509Credential;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Minimal extension of the Apache {@link LdapServer}, allowing us to use Unity
 * credential. Original LdapServer forces us to load JKS from disk.
 *
 * @author K. Benedyczak
 */
class UnityLdapServer extends LdapServer {

    private static final Logger LOG = 
        Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, UnityLdapServer.class);

    private X509Credential credential; //Optional, can be null
    private KeyManagerFactory unityKeyManagerFactory;

    UnityLdapServer(X509Credential credential) {
        this.credential = credential;
    }

    @Override
    public void loadKeyStore() throws Exception {
        if (this.credential == null) {
            //If no credential is set, fall back to the default implementation
            LOG.info("No unity credential configured");
            super.loadKeyStore();
        } else {
            LOG.info("Using the unity credential");
            //Set the unity key manager factory based on the set credential
            String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
            if (algorithm == null) {
                algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
            LOG.info("Algorithm: {}, default algorithm: {}", algorithm, KeyManagerFactory.getDefaultAlgorithm());
            unityKeyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            unityKeyManagerFactory.init(credential.getKeyStore(), credential.getKeyPassword());
        }
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory() {
        //If no credential is set, fall back to the default implementation
        if (this.credential == null) {
            return super.getKeyManagerFactory();
        }
        //Return the unity key manager factory
        return unityKeyManagerFactory;
    }

}
