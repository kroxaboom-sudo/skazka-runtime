#!/usr/bin/env bash
set -euo pipefail

rm -rf build/self-test
mkdir -p build/self-test

javac -encoding UTF-8 -d build/self-test \
  runtime-core/src/main/java/com/kroxaboom/skazka/runtime/*.java \
  tests/RuntimeCoreSelfTest.java

java -cp build/self-test RuntimeCoreSelfTest
