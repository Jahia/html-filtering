#!/usr/bin/env bash

source ./set-env.sh

# TODO temp
pwd
env
ls -R

echo " == Collecting JacCoCo execution data =="

docker cp jahia:/jacoco-data/jacoco-cypress.exec ./artifacts/results/jacoco-cypress.exec

echo " == Generating JaCoCo Cypress report =="

(cd .. && mvn compile org.jacoco:jacoco-maven-plugin:report -Djacoco.dataFile=tests/artifacts/results/jacoco-cypress.exec && mv target/site/jacoco target/site/jacoco-cypress  )

# TODO
ls -R

# This is needed for the folder to be visible on Jahia QA servers
chmod -R 755 ./artifacts/results/