#!/bin/bash
export JAVA_HOME=${JAVA_HOME:-}
exec gradle "$@"
