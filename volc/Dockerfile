FROM ubuntu:focal

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update -q && \
    apt-get upgrade -qq && \
    apt-get install -y curl wget gnupg gnupg2 vim zsh git zip inetutils-ping telnet && \
    rm -rf /var/lib/apt/lists/* && \
    sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

#
## AdoptOpenJDK Hotspot
#
# https://adoptopenjdk.net/installation.html#linux-pkg-deb
RUN \
wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add - && \
echo \
"deb https://adoptopenjdk.jfrog.io/adoptopenjdk/deb $(grep UBUNTU_CODENAME /etc/os-release | cut -d = -f 2) main" | \
tee /etc/apt/sources.list.d/adoptopenjdk.list && \
apt-get update && \
apt-get install -y adoptopenjdk-11-hotspot

# volc cli todo Do you wish to add volc to PATH and install auto complete automatically (y/n)?
#RUN sh -c "$(curl -fsSL https://ml-platform-public-examples-cn-beijing.tos-cn-beijing.volces.com/cli-binary/install.sh)" && \
#    export PATH=$HOME/.volc/bin:$PATH

#ADD ./jar/cromwell-volc.jar /app/cromwell-volc.jar
ADD ./jar/cromwell-78.jar /app/cromwell-volc.jar
ADD ./jar/cromwell-volc-backend-78.jar /app/cromwell-volc-backend.jar
ADD ./cromwell_server.conf /app/cromwell_server.conf
ADD ./cromwell_server_start.sh /app/cromwell_server_start.sh
ADD ./cromwell_server_local_start.sh /app/cromwell_server_local_start.sh
ADD graceful_exit.sh /app/graceful_exit.sh
ADD ./.volc /root/.volc
RUN chmod -R 777 /app/graceful_exit.sh
ENV PATH=/root/.volc/bin:$PATH

CMD cd /app && sh cromwell_server_local_start.sh