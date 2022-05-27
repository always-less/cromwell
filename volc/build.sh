#!/bin/bash

function usage() {
    echo "usage: sh volc/build.sh [options]

options:
    -h        help
    -e [arg]  envName, eg: boe/prod
    -v [arg]  cromwell version, eg: 78
    -t [arg]  test tag/version, used to label the cromwell server image tag
    -p        the flag of whether to push local build image to remote volc container registry, need to login first
    -b        the flag of whether to trigger 'sbt clean assembly' to build a new version jar OR use the previous one from volc/jar/cromwell-volc.jar
"
}

function check_cmd() {
  if ! type $1 > /dev/null 2>&1; then
    echo $1 is required
  fi
}

function ensure_env() {
  check_cmd docker
  check_cmd sbt
}

function parse_args() {
  while getopts "e:v:t:bph" opt
  do
    case $opt in
      e)
        env=${OPTARG}
        ;;
      v)
        cromwell_version=${OPTARG}
        ;;
      t)
        test_version=${OPTARG}
        test_version_suffix=-${test_version}
        ;;
      b)
        build_jar=true
        ;;
      p)
        push_image=true
        ;;
      h)
        usage
        exit 0
        ;;
      ?)
        usage
        echo unknown args: ${OPTARG}
        exit 1
    esac
  done

  if [ ! ${cromwell_version} ]; then
    echo -v [cromwell_version] option is required
    echo
    usage
    exit 0
  fi

}

function build_cromwell_jar() {
  echo =============== Build Cromwell Server Jar Start ===============
  sbt clean assembly

  if [ ! -d 'volc/jar' ]; then
    mkdir volc/jar
  fi

  source_jar="server/target/scala-2.12/cromwell-${cromwell_version}-*.jar"
  target_jar=volc/jar/cromwell-${cromwell_version}-volc-${env}${test_version_suffix}.jar
  docker_ref_jar=volc/jar/cromwell-volc.jar

  if [ -f ${target_jar}.last ]; then
    rm -f ${target_jar}.last
  fi

  if [ -f ${target_jar} ]; then
    echo backup last ${target_jar} to ${target_jar}.last
    mv ${target_jar} ${target_jar}.last
  fi

  cp -a ${source_jar} ${target_jar}

  if [ -f ${docker_ref_jar} ]; then
    rm -f ${docker_ref_jar}
  fi
  ln ${target_jar} ${docker_ref_jar}

  echo =============== Build Cromwell Server Jar End ===============
}

function build_and_push_image() {
  echo =============== Build And Push Cromwell Image Start ===============
  #export DOCKER_BUILDKIT=0
  image_tag=cromwell-${cromwell_version}-volc-${env}${test_version_suffix}
  echo image_tag: ${image_tag}

  cd volc || exit

  volc_cr=cr-cn-guilin-boe.volces.com/machinelearning/pipeline
  if [ $env == "prod" ]; then
    volc_cr=cr-cn-beijing.volces.com/machinelearning/pipeline
  fi

  docker build -t cromwell-volc:${image_tag} -f Dockerfile .
  docker tag cromwell-volc:${image_tag} ${volc_cr}:${image_tag}
  echo
  docker images | grep -e 'IMAGE ID' -e ${image_tag}
  echo
  if [ ${push_image} == "true" ]; then
    echo local build finished, try to push to remote volc container registry
    docker push ${volc_cr}:${image_tag}
#    volc ml_image register -s ${volc_cr}:${image_tag} -t pipeline:${image_tag}
  fi

  cd ..
  echo =============== Build And Push Cromwell Image End ===============
}

function push_image() {
  docker push ${volc_cr}:${image_tag}
  volc ml_image register -s ${volc_cr}:${image_tag} -t pipeline:${image_tag}

}

function main() {

  env=boe
  cromwell_version=""
  test_version=""
  test_version_suffix=""
  build_jar=false
  push_image=false

  parse_args "$@"

  ensure_env

  script_path=$(cd $(dirname $0); pwd)
  cd $script_path/..
  pwd

  echo =============== Build Config ===============
  echo env=$env
  echo cromwell_version=${cromwell_version}
  echo test_version=${test_version}
  echo build_jar=${build_jar}
  echo push_image=${push_image}
  echo =============== Build Config ===============
  echo

  if [ ${build_jar} == "true" ]; then
    build_cromwell_jar
  fi

  echo
  build_and_push_image
}

main "$@"



