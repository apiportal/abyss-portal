#!/usr/bin/env bash

export LAUNCHER="com.verapi.starter.PortalLauncher"
export VERTICLE="com.verapi.starter.InitVerticle"
export CMD="mvn compile"
export VERTX_CMD="run"

mvn compile dependency:copy-dependencies
java \
  -cp  $(echo target/dependency/*.jar | tr ' ' ':'):"target/classes" \
  $LAUNCHER $VERTX_CMD $VERTICLE \
  --redeploy="src/main/**/*" --on-redeploy="$CMD" \
  --launcher-class=$LAUNCHER \
  $@
