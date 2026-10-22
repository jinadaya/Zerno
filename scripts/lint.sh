#!/usr/bin/env bash
# ktlint and detekt over the whole repo, as CI runs them. Pass --format to let ktlint fix what it can.
# The CLIs are downloaded once into build/lint-tools; versions live here.
set -euo pipefail

KTLINT_VERSION=1.8.0
DETEKT_VERSION=1.23.8

cd "$(dirname "$0")/.."
tools=build/lint-tools
mkdir -p "$tools"

fetch() { # name url
    [ -s "$tools/$1" ] || curl -fsSL -o "$tools/$1" "$2"
}
fetch "ktlint-$KTLINT_VERSION.jar" \
    "https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-cli/$KTLINT_VERSION/ktlint-cli-$KTLINT_VERSION-all.jar"
fetch "detekt-$DETEKT_VERSION.jar" \
    "https://repo1.maven.org/maven2/io/gitlab/arturbosch/detekt/detekt-cli/$DETEKT_VERSION/detekt-cli-$DETEKT_VERSION-all.jar"

format=()
[ "${1:-}" = "--format" ] && format=(--format)

status=0
# Style: .editorconfig.
java -jar "$tools/ktlint-$KTLINT_VERSION.jar" "${format[@]}" "**/*.kt" "**/*.kts" "!**/build/**" || status=1
# Code smells: config/detekt/detekt.yml on top of the defaults; the baseline holds findings from before detekt.
java -jar "$tools/detekt-$DETEKT_VERSION.jar" \
    --input . \
    --excludes "**/build/**,**/build-logic/**" \
    --build-upon-default-config \
    --config config/detekt/detekt.yml \
    --baseline config/detekt/baseline.xml || status=1
exit $status
