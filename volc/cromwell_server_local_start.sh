#!/bin/sh

cd /app || exit

#export CROMWELL_BUILD_CENTAUR_SLICK_PROFILE=slick.jdbc.MySQLProfile$
#export CROMWELL_BUILD_CENTAUR_JDBC_DRIVER=com.mysql.cj.jdbc.Driver
#export CROMWELL_BUILD_CENTAUR_JDBC_USER=root
#export CROMWELL_BUILD_RESOURCES_DIRECTORY=target/ci/resources
#export CROMWELL_BUILD_CENTAUR_JDBC_URL="jdbc:mysql://localhost:3306/cromwell_test?allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true&serverTimezone=UTC&useInformationSchema=true"
#export CROMWELL_BUILD_PAPI_JSON_FILE=target/ci/resources/cromwell-centaur-service-account.json
#export CROMWELL_BUILD_CENTAUR_READ_LINES_LIMIT="12800"

export TOS_ENDPOINT_URL=http://tos-s3-cn-beijing.volces.com
export TOS_REGION=cn-beijing
export SERVICE_REGION=cn-beijing
export VOLC_ML_PLATFORM_ENV=PROD
export VOLC_AK=AKLTOGI4OTJhZDA5MDZhNDc2ZTk4NTE4ZGMyYTdjMDAxY2E
export VOLC_SK=TldGbU4yVTRZV0U0WVRBd05HRXdPV0UyT1RCak56ZzBNMk14TVRZMVptUQ==
export CROMWELL_TOS_BUCKET=cromwell-test
export SWAGGER_BASE_PATH=/pipeline-cromwell-test
export CROMWELL_TOS_PREFIX=""
export CROMWELL_TOS_MOUNT_PATH=/cromwell-test
export CROMWELL_BUILD_CENTAUR_SLICK_PROFILE=slick.jdbc.MySQLProfile$
export CROMWELL_BUILD_CENTAUR_JDBC_DRIVER=com.mysql.cj.jdbc.Driver
export CROMWELL_BUILD_CENTAUR_JDBC_URL="jdbc:mysql://localhost:3306/cromwell_test?allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true&serverTimezone=UTC&useInformationSchema=true"
export CROMWELL_BUILD_CENTAUR_JDBC_USER=root
export CROMWELL_BUILD_CENTAUR_JDBC_PASSWORD=123456
export CROMWELL_BUILD_RESOURCES_DIRECTORY=target/ci/resources
export CROMWELL_BUILD_PAPI_JSON_FILE=target/ci/resources/cromwell-centaur-service-account.json
export CROMWELL_BUILD_CENTAUR_READ_LINES_LIMIT=128000

java -cp /app/cromwell-volc.jar:/app/cromwell-volc-backend.jar \
  -Dconfig.file=cromwell_server.conf \
  cromwell.CromwellApp server | tee cromwell.log 2>&1 &

child=`pidof java`
echo "this is the pid of java"
echo $child


wait $child
EXIT_STATUS=$?
echo "end of cromwell, good luck"

echo $EXIT_STATUS