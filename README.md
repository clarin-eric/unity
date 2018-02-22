# unity
This is a fork of the principal repository of Unity IdM service: https://github.com/unity-idm/unity

This fork is the home for:
1. Development of all CLARIN related LDAP requirements for unity-idm. 
1. Development of specific CLARIN related customizations.

Currently a unity-idm 1.9.6 instance is deployed in production.

# Contributing

 There are two main branches, both are protected so no direct commits are possible:

1. `ldapEndpoint` - this is a 1.9.5 branch which is used to create pull request into the upstream unity-idm repository in order to contribute our changes back into the upstream repository and the merge into the `ldapEndpoint-clarin-1.9.6` branch.
2. `ldapEndpoint-clarin-1.9.6` - this is the branch where we develop our customization for our production deployment. These 
changes are usually not contributed back into the upstream repository.

## Contributing code / new features

When developing new LDAP features, please create a new feature branch based on the `ldapEndpoint` branch and create a pull 
after finished the new feature.

When developinf a new CLARIN specific feature, create a new feature branch based on the `ldapEndpoint-clarin-1.9.6` and create
a pull request after finishing the feature.


# LDAP partitions

At first start the `ldap/src/main/resources/partitions.zip` file is extracted 
and used to initialise the LDAP directory. In order to customize or add new LDAP
definitions:

1. extract this file in a temporary location.
2. make your changes.
3. zip the `partitions` directory again and make sure it is included as the root.
of the zip archive.
4. replace the old `ldap/src/main/resources/partitions.zip` with the new `partitions.zip` file.

# Running integration tests

1. Download a [chrome](http://chromedriver.storage.googleapis.com/index.html) or [firefox](https://developer.mozilla.org/en-US/docs/Mozilla/QA/Marionette/WebDriver) driver
2. Specify the path to the driver you want to use via the `webdriver.chrome.driver` system property.

Example:
```
mvn clean install -DargLine="-Dwebdriver.chrome.driver=/Users/wilelb/Downloads/chromedriver"
```

# Configuration

The LDAP server endpoint configuration is stored in `<conf>/endpoints/ldap.properties`.
The following properties can be used the configure the LDAP endpoint:

| Property                                          | Type    | Description          |
| ------------------------------------------------- | ------- | -------------------- |
| unity.ldapServer.host                             | string  | Bind to a specific ip address or use 0.0.0.0 to bind to all addresses. Leave empty for default server address. |
| unity.ldapServer.ldapPort                         | integer | =10000|
| unity.ldapServer.ldapsPort                        | integer | =10443|
| unity.ldapServer.tls                              | boolean | =true |
| unity.ldapServer.certPassword                     | string  | =p4ss|
| unity.ldapServer.keystoreName                     | string  | # keystore filename - relative to working directory, =ldap.keystore|
| unity.ldapServer.groupMember                      | string  | =member |
| unity.ldapServer.groupMemberDnRegexp              | string  | =cn=([^,]+)(,.+)?|
| #unity.ldapServer.groupOfNamesReturnFormat        | string  | |
| unity.ldapServer.returnedUserAttributes           | string  | =cn,entryDN,jpegPhoto # return these attributes if the operation requests all user attributes \ # e.g., values of SchemaConstants.CN_AT |
| unity.ldapServer.userNameAliases                  | string  | =uid,cn,mail|

## Attribute mapping

Configure how to map the unity attributes to LDAP attributes:

| Property                                          | Type   | Description          |
| ------------------------------------------------- | ------ | -------------------- |
| unity.ldapServer.attributes.<num>.ldap.at         | string | LDAP at name         |
| unity.ldapServer.attributes.<num>.ldap.oid        | string | LDAP oid name        |
| unity.ldapServer.attributes.<num>.unity.identity  | string | unity identity type  |
| unity.ldapServer.attributes.<num>.unity.attribute | string | unity attribute name |

## Example
```bash
# leave empty for default server address
unity.ldapServer.host=0.0.0.0
# default ldap port is 389
unity.ldapServer.ldapPort=10000
unity.ldapServer.ldapsPort=10443

# see LdapServer::setCertificatePassword
unity.ldapServer.tls=true
# self signed certificate and keystore password
unity.ldapServer.certPassword=verysafeanddifficulttoguesspassword
# keystore filename - relative to working directory
unity.ldapServer.keystoreName=ldap.keystore

unity.ldapServer.groupMember=member
unity.ldapServer.groupMemberDnRegexp=cn=([^,]+)(,.+)?
#unity.ldapServer.groupOfNamesReturnFormat=

# return these attributes if the operation requests all user attributes
# e.g., values of SchemaConstants.CN_AT
unity.ldapServer.returnedUserAttributes=cn,entryDN,jpegPhoto
unity.ldapServer.userNameAliases=uid,cn,mail

#Unity attribute to LDAP attribute mappings
#Map an unity email identity to the LDAP mail attribute
unity.ldapServer.attributes.1.ldap.at=mail
unity.ldapServer.attributes.1.ldap.oid=0.9.2342.19200300.100.1.3
unity.ldapServer.attributes.1.unity.identity=email
#Also map the unity fullName attribute to the LDAP displayName attribute
unity.ldapServer.attributes.2.ldap.at=displayName
unity.ldapServer.attributes.2.ldap.oid=2.16.840.1.113730.3.1.241
unity.ldapServer.attributes.2.unity.identity=email
```

# Logging

Logging of the LDAP module can be configured as follows:

```
log4j.logger.unity.server.ldap.endpoint=DEBUG
log4j.logger.org.apache.directory.server=DEBUG
```

Where `log4j.logger.unity.server.ldap.endpoint` controls the log verbosity
of this endpoint and `log4j.logger.org.apache.directory.server` controls
 the log verbosity of the underlying LDAP library.