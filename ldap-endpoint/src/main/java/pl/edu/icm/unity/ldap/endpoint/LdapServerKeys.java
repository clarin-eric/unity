/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import sun.security.x509.*;

/**
 *
 * @author wilelb
 */
public class LdapServerKeys {
    private static final int keysize = 1024;
    private static final String commonName = "unity-idm";
    private static final String organizationalUnit = "UAA";
    private static final String organization = "unity-idm";
    private static final String city = "Local";
    private static final String state = "EU";
    private static final String country = "EU";
    private static final long validity = 1096; // 3 years
    private static final String alias = "uaa-ldap";


    private static X509Certificate getSelfCertificate(X500Name x500Name, Date issueDate, long validForSeconds, KeyPair keyPair, String signatureAlgorithm)
        throws CertificateEncodingException
    {
        try {
            Date expirationDate = new Date();
            expirationDate.setTime(issueDate.getTime() + validForSeconds * 1000L);

            X509CertInfo certInfo = new X509CertInfo();
            certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber((new Random()).nextInt() & Integer.MAX_VALUE));
            certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get(signatureAlgorithm)));

            certInfo.set(X509CertInfo.SUBJECT, x500Name);
            certInfo.set(X509CertInfo.ISSUER, x500Name);

            certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
            certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(issueDate, expirationDate));

            X509CertImpl selfSignedCert = new X509CertImpl(certInfo);
            selfSignedCert.sign(keyPair.getPrivate(), signatureAlgorithm);
            return selfSignedCert;
        } catch (Exception ioe) {
            throw new CertificateEncodingException("Error during creation of self-signed Certificate: " + ioe.getMessage());
        }
    }

    /**
     * Get the keystore (or create it).
     */
    public static File getKeystore(String keystoreFileName, String password) throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File keystore = new File(keystoreFileName);

        if (keystore.exists()) {
            return keystore;
        }
        keyStore.load(null, null);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keysize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509Certificate[] chain = {
            getSelfCertificate(new X500Name(
                commonName, organizationalUnit, organization, city, state, country), new Date(), (long) validity * 24 * 60 * 60, keyPair, "SHA1WithRSA"
            )
        };
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), chain);

        if (!keystore.createNewFile()) {
            throw new FileNotFoundException("Unable to create file:" + keystore);
        }
        keyStore.store(new FileOutputStream(keystore, false), password.toCharArray());
        return keystore;
    }
}
