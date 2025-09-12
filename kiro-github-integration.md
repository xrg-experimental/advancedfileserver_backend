# Kiro to GitHub Integration

This repository has been set up with automated workflows to convert Kiro planning documents into GitHub issues, pull requests, and project management.

## Quick Start

1. **Initial Setup**: Run the integration workflow
   ```bash
   ./scripts/setup-kiro-feature.sh
   ```

2. **Create Task Branch**:
   ```bash
   ./scripts/create-task-branch.sh <task_number>
   ```

3. **Auto PR Creation**: When you push a task branch, a PR is automatically created

## Workflow Overview

### Files Structure
- `.kiro/specs/blob-url-files/requirements.md` - Kiro requirements document
- `.kiro/specs/blob-url-files/design.md` - Kiro design document
- `.kiro/specs/blob-url-files/tasks.md` - Kiro implementation tasks
- `.github/workflows/` - Automated workflows
- `scripts/` - Helper scripts

### Epic and Task Linking Implementation

**Epic Structure:**
- **GitHub Construct**: Standard GitHub Issue with labels `epic,enhancement`
- **Title Format**: "Epic: [Project Name]" (e.g., "Epic: File Action Bar")
- **Linking Method**: Task issues reference the epic using issue body text

**Task Structure:**
- **GitHub Construct**: Standard GitHub Issues with labels `task,enhancement`
- **Title Format**: "Task [NUMBER]: [Title]" (e.g., "Task 04: Create FileActionBarComponent")
- **Epic Link**: Each task issue body contains "Part of epic #[EPIC_NUMBER]"

**Example Linking:**
Epic issue #15 "Epic: File Action Bar" is referenced by task issue #16 "Task 04: Create FileActionBarComponent" which contains "Part of epic #15" in its description, creating clickable backlinks in GitHub's interface.

### Automated Workflows

#### 1. Kiro Integration Workflow
**Trigger**: Manual workflow dispatch
**Purpose**: Creates issues and milestones from Kiro documents

Features:
- Creates epic issue from requirements/design
- Creates individual task issues
- Sets up milestone tracking
- Links all issues together using issue references

#### 2. Auto PR Creation
**Trigger**: Push to `feature/task-*` or `task/*` branches
**Purpose**: Automatically creates PRs for task branches

Features:
- Links PR to related issue using "Resolves #[ISSUE_NUMBER]"
- Links PR to epic using "Related to Epic #[EPIC_NUMBER]"
- Generates PR description from commits
- Adds appropriate labels (`task,kiro-generated`)
- Sets up comprehensive review checklist

### Branch Naming Convention

Use these branch naming patterns for automatic PR creation:
- `feature/task-01` - Feature branch for task 1
- `task/05` - Task branch for task 5
- `feat/task-12` - Alternative feature branch

### Issue Management

Issues are automatically created with:
- Task number and title from Kiro tasks
- Requirements coverage information from Kiro requirements.md
- Definition of done checklist
- Links to epic and related Kiro documents
- Appropriate labels (`task,enhancement` or `epic,enhancement`)
- Milestone assignment (if specified)

### Usage Tips

1. **Start with Planning**: Ensure your Kiro files are complete in `.kiro/specs/blob-url-files/`
2. **Run Integration**: Use the setup script to create all GitHub issues
3. **Work on Tasks**: Create branches using the helper script
4. **Auto PRs**: Push branches to automatically create pull requests
5. **Track Progress**: Use GitHub Projects or milestone views to track progress

### Customization

You can customize the integration by:
- Modifying workflow files in `.github/workflows/`
- Updating templates in `.github/ISSUE_TEMPLATE/` and `.github/PULL_REQUEST_TEMPLATE/`
- Adjusting helper scripts in `scripts/`
- Changing Kiro file paths in workflow inputs

### Requirements

- GitHub CLI (`gh`) installed and authenticated
- Python 3.x
- Proper repository permissions (issues, PRs, workflows)
- Kiro planning documents in `.kiro/specs/blob-url-files/` directory

## Troubleshooting

### Common Issues

1. **Workflow fails**: Check that all Kiro files exist at `.kiro/specs/blob-url-files/` and are properly formatted
2. **Issues not created**: Verify GitHub token permissions include issues and milestones
3. **Auto PR fails**: Ensure branch naming follows the convention (`feature/task-XX`, `task/XX`, `feat/task-XX`)
4. **Epic linking broken**: Check that epic issue was created first and task issues reference the correct epic number

### File Path Issues

The workflows expect Kiro files at:
- `.kiro/specs/blob-url-files/requirements.md`
- `.kiro/specs/blob-url-files/design.md`
- `.kiro/specs/blob-url-files/tasks.md`

If your files are in different locations, update the workflow inputs in `scripts/setup-kiro-feature.sh`.

### Getting Help

Check the workflow logs in the Actions tab for detailed error information. Look for:
- File not found errors (check paths)
- GitHub API errors (check permissions)
- Task parsing errors (check tasks.md format)
