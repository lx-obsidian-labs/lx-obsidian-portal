#!/bin/sh
# Dry-run by default; pass --send to broadcast once to the global topic.
exec node "$(dirname "$0")/send_fcm_cli.mjs" "$@"
