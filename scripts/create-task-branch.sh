#!/bin/bash
# Create a branch for a specific task

if [ $# -eq 0 ]; then
    echo "Usage: $0 <task_number> [base_branch]"
    echo "Example: $0 04 main"
    exit 1
fi

TASK_NUMBER=$(printf "%02d" $1)
BASE_BRANCH=${2:-main}

BRANCH_NAME="feature/task-$TASK_NUMBER"

echo "Creating branch $BRANCH_NAME from $BASE_BRANCH..."

git checkout $BASE_BRANCH
git pull origin $BASE_BRANCH
git checkout -b $BRANCH_NAME

echo "✅ Created and switched to branch: $BRANCH_NAME"
echo "💡 When you're ready to create a PR, just push this branch:"
echo "   git push -u origin $BRANCH_NAME"
