#!/bin/bash

# Function to load env vars from a file
load_env() {
  if [ -f "$1" ]; then
    echo "Loading environment variables from $1"
    export $(grep -v '^#' "$1" | xargs)
  else
    echo "Environment file $1 not found."
  fi
}

# First try to load from .env.development, then fall back to .env
if [ -f .env.development ]; then
  load_env .env.development
else
  load_env .env
fi

# Run the Maven exec plugin
mvn exec:java