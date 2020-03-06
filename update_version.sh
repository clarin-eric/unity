#!/bin/sh

HELP=0
VERSION=""

#
# Process script arguments
#
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -h|--help)
        HELP=1
        ;;
    -v)
        VERSION=$2
	shift # past "argument value"
        ;;
    --version=*)
    	VERSION="${key#*=}"
    	shift # past "argument=value"
	;;
    *)
        echo "Unkown option: $key"
        HELP=2
        ;;
esac
shift # past argument or value
done

if [ ${HELP} -gt 0 ]; then
	echo "Usage:"
	echo "  ./update_version.sh [options]"
	echo ""
	echo "Options:"
	echo " -v <value>|--version=<value>	Set the version for this project and all submodules to <value>"
	echo " -h|--help			Show this help"
	exit 1
fi

if [ "${VERSION}" == "" ]; then
	echo "Version is required"
	exit 1
fi

mvn versions:set "-DnewVersion=${VERSION}" versions:commit
