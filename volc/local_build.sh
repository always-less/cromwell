#!/bin/bash

cd volc || exit

docker build -t cromwell-78-volc:local-test -f Dockerfile .