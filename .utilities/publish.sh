#!/usr/bin/env bash
if [ "${BITBUCKET_BRANCH}" == "deploy" ]; then
  if [[ $(./gradlew -q getVersion) == *SNAPSHOT* ]]; then
      echo 'The deploy branch is only to deploy final releases. No snapshots allowed.'
      exit 1
  fi

  echo -e "Starting publish to Sonatype...\n"

  ./gradlew uploadArchives -PnexusUsername="${NEXUS_USERNAME}" -PnexusPassword="${NEXUS_PASSWORD}" -Psigning.keyId="${SIGNING_KEY_ID}" -Psigning.password="${SIGNING_KEY_PASSPHRASE}" -Psigning.secretKeyRingFile=.utilities/secring.gpg
  RETVAL=$?

  if [ ${RETVAL} -eq 0 ]; then
    echo 'Completed publish!'
  else
    echo 'Publish failed.'
    exit 1
  fi

  echo -e "Attempting to promote package...\n"

  ./gradlew closeAndPromoteRepository
  RETVAL=$?

  if [ ${RETVAL} -eq 0 ]; then
    echo 'Package promoted!'
  else
    echo 'Could not promote package. Please check everything in the grdale-nexus-staging-plugin configuration or at Nexus repository and try it manually'
    exit 1
  fi
fi
