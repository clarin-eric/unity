/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
//import sun.security.x509.*;

//import org.bouncycastle.x509.
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V3CertificateGenerator;

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
    private static final long validityInDays = 1096; // 3 years
    private static final String alias = "uaa-ldap";

    private static X509Certificate getSelfCertificate(String commonName, String organizationalUnit, String organization, String city, String state, String country, KeyPair keyPair)
        throws Exception
    {
        Date issueDate = new Date();
        long validForSeconds = validityInDays * 24 * 60 * 60;
        Date expirationDate = new Date(issueDate.getTime()+validForSeconds*1000);
        String signatureAlgorithm = "SHA1WithRSA"; //"SHA256WithRSAEncryption"?

        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, commonName);
        nameBuilder.addRDN(BCStyle.OU, organizationalUnit);
        nameBuilder.addRDN(BCStyle.O, organization);


        //final X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        //final java.security.cert.X509Certificate cert = generator.generate(keyPair.getPrivate(), BouncyCastleProvider.PROVIDER_NAME);

        //X500Name x500Name = new X500Name(commonName, organizationalUnit, organization, city, state, country);
        //return createSelfSignedCertifcate(x500Name, issueDate, expirationDate, keyPair, signatureAlgorithm);

        return createSelfSignedCertifcate(nameBuilder.build(), issueDate, expirationDate, keyPair, signatureAlgorithm);
    }

    /**
     * Reference:
     *  https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/test/java/org/apache/zookeeper/server/quorum/QuorumSSLTest.java#L275
     *
     * @param name
     * @param certStartTime
     * @param certEndTime
     * @param keyPair
     * @param signatureAlgorithm
     * @return
     * @throws Exception
     */
    private static X509Certificate createSelfSignedCertifcate(X500Name name, Date certStartTime, Date certEndTime, KeyPair keyPair, String signatureAlgorithm) throws Exception {
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());

        BigInteger serialNumber = new BigInteger(128, new Random());
        X509v3CertificateBuilder certificateBuilder =
                new JcaX509v3CertificateBuilder(name, serialNumber, certStartTime, certEndTime,
                        name, keyPair.getPublic())
                        .addExtension(Extension.basicConstraints, true, new BasicConstraints(0))
                        .addExtension(Extension.keyUsage, true,
                                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(contentSigner));
    }

    /*
    private static X509Certificate getSelfCertificate(X500Name x500Name, Date issueDate, Date expirationDate, KeyPair keyPair, String signatureAlgorithm)
        throws CertificateEncodingException
    {
        try {
            //Date expirationDate = new Date();
            //expirationDate.setTime(issueDate.getTime() + validForSeconds * 1000L);

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
    */
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
            getSelfCertificate(commonName, organizationalUnit, organization, city, state, country,keyPair)
        };
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), chain);

        if (!keystore.createNewFile()) {
            throw new FileNotFoundException("Unable to create file:" + keystore);
        }
        keyStore.store(new FileOutputStream(keystore, false), password.toCharArray());
        return keystore;
    }
}
