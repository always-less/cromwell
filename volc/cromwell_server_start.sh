#!/bin/sh

cd /root/.volc || exit
echo '[default]
region = '${SERVICE_REGION}'

[ml_platform]
env = '${VOLC_ML_PLATFORM_ENV} > config

echo '[default]
access_key_id     = '${VOLC_AK}'
secret_access_key = '${VOLC_SK} > credentials

#echo '[ -r ~/.volc/.profile ] && source ~/.volc/.profile #[volc installer]' >> /root/.zshrc
#echo 'export PATH=$HOME/.volc/bin:$PATH' >> /root/.zshrc

cd /app || exit

#export CROMWELL_BUILD_CENTAUR_SLICK_PROFILE=slick.jdbc.MySQLProfile$
#export CROMWELL_BUILD_CENTAUR_JDBC_DRIVER=com.mysql.cj.jdbc.Driver
#export CROMWELL_BUILD_CENTAUR_JDBC_USER=root
#export CROMWELL_BUILD_RESOURCES_DIRECTORY=target/ci/resources
#export CROMWELL_BUILD_CENTAUR_JDBC_URL="jdbc:mysql://localhost:3306/cromwell_test?allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true&serverTimezone=UTC&useInformationSchema=true"
#export CROMWELL_BUILD_PAPI_JSON_FILE=target/ci/resources/cromwell-centaur-service-account.json
#export CROMWELL_BUILD_CENTAUR_READ_LINES_LIMIT="12800"

java -jar -Dconfig.file=cromwell_server.conf cromwell-volc.jar server  | tee cromwell.log 2>&1 &

child=`pidof java`
echo "this is the pid of java"
echo $child


wait $child
EXIT_STATUS=$?
echo "end of cromwell, good luck"

echo $EXIT_STATUS