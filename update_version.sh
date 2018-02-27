#/bin/sh

#
# Version structure:
# <unity version>-<tag>-<clarin version>
#
# <unity version>	Main unity version this release is based off
# <tag>			Tag with a static value of "clarin" to  indicate this is a clarin specific release
# <clarin version>	Clarin release specific version, 
#				_alpha#, _beta# and _rc# postfixes can be added to specify alpha, beta or 
#				release candidate releases where # is replaced by a sequence number
#

CURRENT_VERSION="1.9.6-clarin-2.2.0_beta2"
NEW_VERSION="1.9.6-clarin-2.2.0_beta3"

echo "Updating all versions from ${CURRENT_VERSION} to ${NEW_VERSION}"
find . -name pom.xml -exec sed -i '' "s/<version>${CURRENT_VERSION}<\/version>/<version>${NEW_VERSION}<\/version>/g" {} \;

echo "Building new release"
mvn clean install -DskipTests
