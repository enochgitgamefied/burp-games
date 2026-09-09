#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p .deps build dist
# These directories contain generated classes only; clean to avoid packaging obsolete classes.
rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes
api=.deps/montoya-api-2026.7.jar
if [ ! -f "$api" ]; then
  curl --fail --location --connect-timeout 15 --max-time 120 \
    https://repo.maven.apache.org/maven2/net/portswigger/burp/extensions/montoya-api/2026.7/montoya-api-2026.7.jar \
    --output "$api.tmp"
  mv "$api.tmp" "$api"
fi
expected=cb852b7b883290fcc1d21b13d80fa10f5b13a0f57335d710d2b1e5956ecb2a94
if command -v sha256sum >/dev/null 2>&1; then
  actual=$(sha256sum "$api" | cut -d ' ' -f 1)
else
  actual=$(shasum -a 256 "$api" | cut -d ' ' -f 1)
fi
if [ "$actual" != "$expected" ]; then
  echo "Montoya API checksum mismatch. Delete $api and retry." >&2
  exit 1
fi
javac --release 17 -encoding UTF-8 -cp "$api" -d build/classes src/main/java/dev/velocity/*.java
if [ -d src/main/resources ]; then cp -R src/main/resources/. build/classes/; fi
mkdir -p build/classes/META-INF/licenses
cp LICENSE build/classes/META-INF/licenses/
cp THIRD-PARTY-ASSETS.md build/classes/META-INF/
printf 'Implementation-Title: Burp Games\nImplementation-Version: 6.0.0\n' > build/games-manifest.mf
jar --create --file dist/burp-games.jar --manifest build/games-manifest.mf --main-class dev.velocity.Standalone -C build/classes .
if [ "${1:-}" = "test" ]; then
  javac --release 17 -encoding UTF-8 -cp "build/classes:$api" -d build/test-classes src/test/java/dev/velocity/*.java
  mkdir -p screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.GameTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.RendererTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.ArcadeTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.UpgradeTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.AudioTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.TextureTest screenshots
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.SettingsRestartTest
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.KartRaceTest
  java -Djava.awt.headless=true -ea -cp "build/test-classes:build/classes:$api" dev.velocity.KartRenderTest screenshots
fi
echo "Built dist/burp-games.jar"
