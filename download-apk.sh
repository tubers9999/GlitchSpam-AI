#!/usr/bin/env bash
# Downloads the latest built GlitchSpam APK from GitHub Actions Artifacts
set -euo pipefail

echo "==> Fetching latest GlitchSpam APK artifact from GitHub Actions..."
mkdir -p ./apk-output
gh run download --name GlitchSpam-AI-APKs --dir ./apk-output || {
    echo "Note: If no artifact was downloaded, ensure the GitHub Actions workflow has completed first:"
    echo "  gh run list --workflow=android.yml"
    exit 1
}

echo "==> Download complete! APK is located at:"
ls -lh ./apk-output/*.apk
