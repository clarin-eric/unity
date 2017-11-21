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
