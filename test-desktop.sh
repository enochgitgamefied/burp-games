#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes
javac --release 17 -encoding UTF-8 -cp dist/burp-games.jar -d build/test-classes src/test/java/dev/velocity/KartIntegrationTest.java
java -ea -cp dist/burp-games.jar:build/test-classes dev.velocity.KartIntegrationTest
java --limit-modules java.se,jdk.unsupported -ea -cp dist/burp-games.jar:build/test-classes dev.velocity.KartIntegrationTest reduced
