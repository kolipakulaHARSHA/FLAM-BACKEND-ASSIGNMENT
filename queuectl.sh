#!/bin/bash
# queuectl - Background Job Queue System
# Unix/Linux/Mac shell script wrapper

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java -jar "$SCRIPT_DIR/target/queuectl.jar" "$@"
