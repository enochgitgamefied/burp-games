#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes screenshots
javac --release 17 -encoding UTF-8 -cp dist/burp-games.jar -d build/test-classes src/test/java/dev/velocity/ArcadeTest.java src/test/java/dev/velocity/KartRenderTest.java
java --limit-modules java.se,jdk.unsupported -Djava.awt.headless=true -ea -cp dist/burp-games.jar:build/test-classes dev.velocity.KartRenderTest screenshots
