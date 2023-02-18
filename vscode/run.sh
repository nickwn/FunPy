#!/usr/bin/env bash

# Get _this_ script's path, so we can use absolute paths for run.py
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

SNIPPY_UTILS="$SCRIPT_DIR/src/snippy.py" PYTHON3=$(which python3) JAVA=$(which java) RUNPY="$SCRIPT_DIR/src/run.py" IMGSUM="$SCRIPT_DIR/src/img-summary.py" SYNTH='' scripts/code.sh
