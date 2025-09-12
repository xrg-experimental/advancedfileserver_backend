#!/bin/bash
# Quick setup script for new Kiro features

echo "🚀 Kiro Feature Setup"
echo "===================="

read -p "Enter project/feature name: " PROJECT_NAME
read -p "Enter milestone name (optional): " MILESTONE_NAME

echo "Setting up GitHub integration for: $PROJECT_NAME"

# Trigger the GitHub workflow
if [ -z "$MILESTONE_NAME" ]; then
    gh workflow run kiro-integration.yml \
      -f project_name="$PROJECT_NAME" \
      -f requirements_file=".kiro/specs/blob-url-files/requirements.md" \
      -f design_file=".kiro/specs/blob-url-files/design.md" \
      -f tasks_file=".kiro/specs/blob-url-files/tasks.md"
else
    gh workflow run kiro-integration.yml \
      -f project_name="$PROJECT_NAME" \
      -f milestone_name="$MILESTONE_NAME" \
      -f requirements_file=".kiro/specs/blob-url-files/requirements.md" \
      -f design_file=".kiro/specs/blob-url-files/design.md" \
      -f tasks_file=".kiro/specs/blob-url-files/tasks.md"
fi

echo "✅ GitHub workflow triggered!"
echo "🔗 Check the Actions tab to see the progress"
echo "📋 Issues and milestone will be created automatically"
