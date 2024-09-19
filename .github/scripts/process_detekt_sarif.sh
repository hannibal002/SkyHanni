#!/bin/bash

# This script processes the Detekt SARIF file and outputs results in a format
# suitable for annotation in CI/CD systems.

SARIF_FILE="$1"

# Check if SARIF file exists
if [ ! -f "$SARIF_FILE" ]; then
    echo "SARIF file not found: $SARIF_FILE"
    exit 1
fi

# Define jq command to parse SARIF file
read -r -d '' jq_command <<'EOF'
.runs[].results[] |
{
    "l": .locations[].physicalLocation,
    "level": .level,
    "message":.message,
    "ruleId":.ruleId
} |
(
    "::" + (.level) +
    " file=" + ( .l.artifactLocation.uri | sub("file://$(pwd)/"; ""))
    + ",line=" + (.l.region.startLine|tostring)
    + ",endLine=" + (.l.region.endLine|tostring)
    + ",col=" + (.l.region.startColumn|tostring)
    + ",endColumn=" + (.l.region.endColumn|tostring)
    + ",title=" + (.ruleId) + "::" + (.message.text))
EOF

# Run jq command
jq -r "$jq_command" < "$SARIF_FILE"
