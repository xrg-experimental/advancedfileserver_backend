#!/usr/bin/env python3

"""
Kiro to GitHub Integration Setup Script
This script sets up the complete workflow for integrating Kiro planning with GitHub
"""

import os
import sys
import json
import subprocess
import shutil
from pathlib import Path

class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color

def print_status(message):
    print(f"{Colors.GREEN}✅ {message}{Colors.NC}")

def print_warning(message):
    print(f"{Colors.YELLOW}⚠️  {message}{Colors.NC}")

def print_error(message):
    print(f"{Colors.RED}❌ {message}{Colors.NC}")

def print_info(message):
    print(f"{Colors.BLUE}ℹ️  {message}{Colors.NC}")

def check_prerequisites():
    """Check if we're in a git repository and required files exist"""
    print_info("Checking prerequisites...")

    # Check if we're in a git repository
    if not Path('.git').exists():
        print_error("This script must be run from the root of a git repository")
        sys.exit(1)

    # Check for required Kiro files
    required_files = [
        ".kiro/specs/blob-url-files/requirements.md",
        ".kiro/specs/blob-url-files/design.md",
        ".kiro/specs/blob-url-files/tasks.md"
    ]

    for file_path in required_files:
        if not Path(file_path).exists():
            print_error(f"{file_path} not found")
            print(f"Please ensure your Kiro file exists at the expected location")
            sys.exit(1)

    print_status("All Kiro planning files found")

def create_directory_structure():
    """Create the GitHub directory structure"""
    print_info("Creating GitHub workflow structure...")

    directories = [
        ".github/workflows",
        ".github/ISSUE_TEMPLATE",
        ".github/PULL_REQUEST_TEMPLATE",
        "scripts"
    ]

    for directory in directories:
        Path(directory).mkdir(parents=True, exist_ok=True)

def create_kiro_integration_workflow():
    """Create the main Kiro integration workflow"""
    workflow_content = '''# .github/workflows/kiro-integration.yml
name: Kiro to GitHub Integration

on:
  workflow_dispatch:
    inputs:
      requirements_file:
        description: 'Path to requirements.md file'
        required: true
        default: '.kiro/specs/blob-url-files/requirements.md'
      design_file:
        description: 'Path to design.md file'
        required: true
        default: '.kiro/specs/blob-url-files/design.md'
      tasks_file:
        description: 'Path to tasks.md file'
        required: true
        default: '.kiro/specs/blob-url-files/tasks.md'
      project_name:
        description: 'Project name for issue titles'
        required: true
        default: 'File Action Bar'
      milestone_name:
        description: 'Milestone name (optional)'
        required: false

jobs:
  create-project-structure:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      issues: write
      pull-requests: write

    outputs:
      milestone_number: ${{ steps.create-milestone.outputs.milestone_number }}
      epic_issue_number: ${{ steps.create-epic.outputs.issue_number }}

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.x'

      - name: Create milestone
        id: create-milestone
        if: ${{ inputs.milestone_name != '' }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          python3 << 'PYTHON_SCRIPT'
          import subprocess
          import json
          import os
          import sys

          milestone_name = "${{ inputs.milestone_name }}"
          repo = "${{ github.repository }}"

          try:
              # First, try to get existing milestones
              get_cmd = [
                  'gh', 'api', f'repos/{repo}/milestones'
              ]
              
              result = subprocess.run(get_cmd, capture_output=True, text=True, check=True)
              milestones = json.loads(result.stdout)
              
              # Check if milestone with this title already exists
              existing_milestone = None
              for milestone in milestones:
                  if milestone['title'] == milestone_name:
                      existing_milestone = milestone
                      break
              
              if existing_milestone:
                  milestone_number = existing_milestone['number']
                  print(f"Found existing milestone: {milestone_name} (#{milestone_number})")
              else:
                  # Create new milestone
                  create_cmd = [
                      'gh', 'api', f'repos/{repo}/milestones',
                      '--method', 'POST',
                      '--field', f'title={milestone_name}',
                      '--field', 'description=Auto-generated milestone from Kiro planning'
                  ]
                  
                  result = subprocess.run(create_cmd, capture_output=True, text=True, check=True)
                  response = json.loads(result.stdout)
                  milestone_number = response['number']
                  
                  print(f"Created new milestone: {milestone_name} (#{milestone_number})")
              
              # Set GitHub output
              with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
                  f.write(f"milestone_number={milestone_number}\\n")
                  
          except subprocess.CalledProcessError as e:
              print(f"Error with milestone operation: {e}")
              print(f"Error output: {e.stderr}")
              sys.exit(1)
          except json.JSONDecodeError as e:
              print(f"Error parsing JSON response: {e}")
              sys.exit(1)
          PYTHON_SCRIPT

      - name: Parse Kiro files and create epic issue
        id: create-epic
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          python3 << 'PYTHON_SCRIPT'
          import os
          import subprocess
          import sys

          def read_file_section(file_path, start_marker, end_marker=None):
              """Read a section from a markdown file between markers"""
              try:
                  with open(file_path, 'r') as f:
                      content = f.read()

                  start_idx = content.find(start_marker)
                  if start_idx == -1:
                      return "See attached document"

                  start_idx += len(start_marker)

                  if end_marker:
                      end_idx = content.find(end_marker, start_idx)
                      if end_idx != -1:
                        return content[start_idx:end_idx].strip()

                  # If no end marker or not found, take next 500 chars
                  return content[start_idx:start_idx+500].strip()
              except Exception as e:
                  return f"Error reading file: {e}"

          # Read file sections
          requirements_summary = read_file_section(
              '${{ inputs.requirements_file }}',
              '## Requirements',
              '### Requirement 1'
          )

          architecture_overview = read_file_section(
              '${{ inputs.design_file }}',
              '## Architecture',
              '### Component Structure'
          )

          # Build proper GitHub URLs for the documents
          repo = '${{ github.repository }}'
          branch = '${{ github.ref_name }}'
          base_url = f'https://github.com/{repo}/blob/{branch}'

          requirements_url = f'{base_url}/${{ inputs.requirements_file }}'
          design_url = f'{base_url}/${{ inputs.design_file }}'
          tasks_url = f'{base_url}/${{ inputs.tasks_file }}'

          epic_body = f"""## Epic: ${{ inputs.project_name }}

          ### Overview
          This epic tracks the implementation of the ${{ inputs.project_name }} feature based on Kiro-generated planning documents.

          ### Requirements Summary
          {requirements_summary}

          ### Architecture Overview
          {architecture_overview}

          ### Related Documents
          - [Requirements]({requirements_url})
          - [Design]({design_url})
          - [Tasks]({tasks_url})

          ### Acceptance Criteria
          - [ ] All task items completed
          - [ ] Requirements validated
          - [ ] Code reviewed and approved
          - [ ] Tests passing

          *Auto-generated from Kiro planning documents*"""

          # Create epic issue
          milestone_arg = []
          milestone_number = '${{ steps.create-milestone.outputs.milestone_number }}'
          if milestone_number and milestone_number != '':
              milestone_arg = ['--milestone', milestone_number]

          cmd = [
              'gh', 'issue', 'create',
              '--title', 'Epic: ${{ inputs.project_name }}',
              '--body', epic_body,
              '--label', 'epic,enhancement'
          ] + milestone_arg

          try:
              result = subprocess.run(cmd, capture_output=True, text=True, check=True)
              issue_url = result.stdout.strip()

              # Extract issue number from URL
              epic_number = issue_url.rsplit('/', 1)[-1] if issue_url else None
              if epic_number and not epic_number.isdigit():
                  print(f"Unexpected issue URL format: {issue_url}")
                  sys.exit(1)

              if epic_number:
                  print(f"issue_number={epic_number}")
                  print(f"Created epic issue #{epic_number}")

                  # Set GitHub output
                  with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
                      f.write(f"issue_number={epic_number}\\n")
              else:
                  print("Failed to extract issue number from URL")
                  sys.exit(1)

          except subprocess.CalledProcessError as e:
              print(f"Error creating epic issue: {e}")
              print(f"Error output: {e.stderr}")
              sys.exit(1)
          PYTHON_SCRIPT

  create-task-issues:
    needs: create-project-structure
    runs-on: ubuntu-latest
    permissions:
      contents: write
      issues: write
      pull-requests: write

    steps:
    - name: Checkout repository
      uses: actions/checkout@v4

    - name: Setup Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.x'

    - name: Parse tasks and create issues
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      run: |
        python3 << 'PYTHON_SCRIPT'
        import re
        import subprocess
        import sys

        def parse_tasks_file(file_path):
            """Parse the tasks.md file and extract task information"""
            try:
                with open(file_path, 'r') as f:
                    content = f.read()
            except FileNotFoundError:
                print(f"Tasks file not found: {file_path}")
                return []

            # Improved regex to capture task details
            task_pattern = r'- \\[([ x])\\] (\\d+)\\. (.*?)(?=\\n- \\[|\\n\\n|$)'
            matches = re.findall(task_pattern, content, re.DOTALL)

            tasks = []
            for completed, task_num, content_block in matches:
                is_completed = completed == 'x'

                # Split content into lines and process
                lines = content_block.strip().split('\\n')
                title = lines[0].strip()

                # Extract description and requirements
                description_lines = []
                requirements = 'Not specified'

                for line in lines[1:]:
                    line = line.strip()
                    if line.startswith('_Requirements:') and line.endswith('_'):
                        requirements = line.replace('_Requirements: ', '').replace('_', '')
                    elif line and not line.startswith('_'):
                        description_lines.append(line)

                description = '\\n'.join(description_lines).strip()

                tasks.append({
                    'number': task_num,
                    'title': title,
                    'description': description,
                    'requirements': requirements,
                    'completed': is_completed
                })

            return tasks

        def create_task_issue(task, epic_number, milestone_number):
            """Create a GitHub issue for a task"""
            issue_body = f"""## Task #{task['number']}: {task['title']}

            ### Description
            {task['description'] if task['description'] else 'Implementation details to be determined during development.'}

            ### Requirements Covered
            {task['requirements']}

            ### Related Epic
            Part of epic #{epic_number}

            ### Definition of Done
            - [ ] Implementation completed
            - [ ] Unit tests written and passing
            - [ ] Code reviewed
            - [ ] Requirements validated
            - [ ] Documentation updated

            *Auto-generated from Kiro tasks*
            """

            labels = 'task,enhancement'
            if task['completed']:
                labels += ',completed'

            cmd = [
                'gh', 'issue', 'create',
                '--title', f"Task {task['number']}: {task['title']}",
                '--body', issue_body,
                '--label', labels
            ]

            if milestone_number and milestone_number != 'null' and milestone_number != '':
                cmd.extend(['--milestone', milestone_number])

            try:
                result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                issue_url = result.stdout.strip()
                print(f"Created issue for task {task['number']}: {task['title']}")

                return True
            except subprocess.CalledProcessError as e:
                print(f"Failed to create issue for task {task['number']}: {e}")
                print(f"Error output: {e.stderr}")
                return False

        # Main execution
        epic_number = '${{ needs.create-project-structure.outputs.epic_issue_number }}'
        milestone_number = '${{ needs.create-project-structure.outputs.milestone_number }}'

        print(f"Epic number: {epic_number}")
        print(f"Milestone number: {milestone_number}")

        # Parse tasks
        tasks = parse_tasks_file('${{ inputs.tasks_file }}')
        print(f"Found {len(tasks)} tasks to process")

        # Create issues for each task
        success_count = 0
        for task in tasks:
            if create_task_issue(task, epic_number, milestone_number):
                success_count += 1

        print(f"Successfully created {success_count} out of {len(tasks)} task issues")
        PYTHON_SCRIPT
'''

    with open('.github/workflows/kiro-integration.yml', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(workflow_content)

def create_auto_pr_workflow():
    """Create the auto PR creation workflow"""
    workflow_content = '''# .github/workflows/auto-pr-creation.yml
name: Auto PR Creation from Completed Tasks

on:
  push:
    branches:
      - 'feature/task-*'
      - 'task/*'
      - 'feat/task-*'
  workflow_dispatch:
    inputs:
      task_number:
        description: 'Task number to create PR for'
        required: true
      base_branch:
        description: 'Base branch for PR'
        required: false
        default: 'main'

jobs:
  create-pr:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
      issues: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.x'

      - name: Extract task number and create PR
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          python3 << 'PYTHON_SCRIPT'
          import os
          import subprocess
          import sys
          import json

          def extract_task_number():
              """Extract task number from branch name or workflow input"""
              if os.environ.get('GITHUB_EVENT_NAME') == 'workflow_dispatch':
                  return os.environ.get('INPUT_TASK_NUMBER', '')
              else:
                  branch_name = os.environ.get('GITHUB_REF_NAME', '')
                  import re
                  match = re.search(r'\\d+', branch_name)
                  return match.group() if match else ''

          def find_issue_for_task(task_number):
              """Find the GitHub issue for a specific task number"""
              cmd = ['gh', 'issue', 'list', '--search', f'Task {task_number}:', '--state', 'open', '--limit', '1']

              try:
                  result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                  lines = result.stdout.strip().split('\\n')

                  if not lines or not lines[0]:
                      return None, None, None

                  # Parse the first line to extract issue number and title
                  first_line = lines[0]
                  parts = first_line.split('\\t')
                  if len(parts) >= 2:
                      issue_number = parts[0]
                      issue_title = parts[1]
                      return issue_number, issue_title, None

                  return None, None, None
              except subprocess.CalledProcessError as e:
                  print(f"Error finding issue: {e}")
                  return None, None, None

          def check_existing_pr(branch_name, base_branch):
              """Check if PR already exists for this branch"""
              cmd = ['gh', 'pr', 'list', '--head', branch_name, '--base', base_branch]

              try:
                  result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                  lines = result.stdout.strip().split('\\n')

                  if not lines or not lines[0]:
                      return None

                  # Parse the first line to extract PR number
                  first_line = lines[0]
                  parts = first_line.split('\\t')
                  if len(parts) >= 1:
                      return parts[0]

                  return None
              except subprocess.CalledProcessError:
                  return None

          def get_commit_summary(base_branch, current_branch):
              """Get commit summary since branching"""
              cmd = ['git', 'log', '--pretty=format:- %s', f'{base_branch}..{current_branch}']

              try:
                  result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                  commits = result.stdout.strip()
                  return commits if commits else '- Initial commit for this task'
              except subprocess.CalledProcessError:
                  return '- Initial commit for this task'

          def find_epic_issue():
              """Find the epic issue"""
              cmd = ['gh', 'issue', 'list', '--search', 'Epic:', '--state', 'open']

              try:
                  result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                  lines = result.stdout.strip().split('\\n')

                  if not lines or not lines[0]:
                      return None

                  # Parse the first line to extract issue number
                  first_line = lines[0]
                  parts = first_line.split('\\t')
                  if len(parts) >= 1:
                      return parts[0]

                  return None
              except subprocess.CalledProcessError:
                  return None

          def create_pull_request(task_number, issue_number, issue_title, milestone_number, commits, base_branch, current_branch):
              """Create the pull request"""
              epic_number = find_epic_issue()

              # Clean up issue title for PR title
              pr_title = issue_title.replace(f'Task {task_number}: ', '') if issue_title else f'Task {task_number}'

              pr_body = f"""## Task {task_number}: {pr_title}

          ### Description
          This PR implements the changes for Task {task_number} as part of the Kiro-planned feature development.

          ### Related Issues
          - Resolves #{issue_number}"""

              if epic_number:
                  pr_body += f"\\n- Related to Epic #{epic_number}"

              pr_body += f"""

          ### Changes Made
          {commits}

          ### Testing Checklist
          - [ ] Unit tests added/updated
          - [ ] Integration tests pass
          - [ ] Manual testing completed
          - [ ] Edge cases tested
          - [ ] Error handling tested

          ### Code Quality Checklist
          - [ ] Code follows project standards
          - [ ] TypeScript types properly defined
          - [ ] Error handling implemented
          - [ ] Loading states implemented
          - [ ] No console.log statements left in code

          ### Review Checklist
          - [ ] Functionality matches requirements
          - [ ] Code is readable and maintainable
          - [ ] Performance considerations addressed
          - [ ] Security considerations addressed
          - [ ] Accessibility requirements met

          ### Deployment Notes
          <!-- Add any special deployment considerations -->

          ---
          *This PR was auto-generated from Kiro task tracking*"""

              cmd = [
                  'gh', 'pr', 'create',
                  '--title', pr_title,
                  '--body', pr_body,
                  '--base', base_branch,
                  '--head', current_branch,
                  '--label', 'task,kiro-generated'
              ]

              try:
                  result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                  # Extract PR URL from output
                  pr_url = (result.stdout or "").strip()

                  if pr_url:
                      # Extract PR number from URL
                      pr_number = pr_url.rsplit('/', 1)[-1] if pr_url.startswith('http') else None

                      if pr_number and issue_number:
                          # Link PR to issue
                          comment_cmd = ['gh', 'issue', 'comment', str(issue_number), '--body', f'🔗 Pull Request created: {pr_url}']
                          subprocess.run(comment_cmd, check=True)

                      print(f"✅ Created PR: {pr_title}")
                      print(f"PR URL: {pr_url}")

                      return pr_number
                  else:
                      print(f"✅ Created PR: {pr_title}")
                      return True
              except subprocess.CalledProcessError as e:
                  print(f"Error creating PR: {e}")
                  print(f"Command output: {e.stdout if hasattr(e, 'stdout') else 'N/A'}")
                  print(f"Command error: {e.stderr if hasattr(e, 'stderr') else 'N/A'}")
                  return None

          # Main execution
          task_number = extract_task_number()
          if not task_number:
              print("No task number found in branch name or input")
              sys.exit(1)

          print(f"Task number: {task_number}")

          branch_name = os.environ.get('GITHUB_REF_NAME', '')
          base_branch = os.environ.get('INPUT_BASE_BRANCH', 'main')

          print(f"Branch: {branch_name}")
          print(f"Base branch: {base_branch}")

          # Check if PR already exists
          existing_pr = check_existing_pr(branch_name, base_branch)
          if existing_pr:
              print(f"PR already exists: #{existing_pr}")
              sys.exit(0)

          # Find corresponding issue
          issue_number, issue_title, milestone_number = find_issue_for_task(task_number)
          if not issue_number:
              print(f"No open issue found for task {task_number}")
              print("Creating PR without linked issue...")
              issue_number = None
              issue_title = f"Task {task_number}"

          if issue_number:
              print(f"Found issue #{issue_number}: {issue_title}")

          # Get commit summary
          commits = get_commit_summary(base_branch, branch_name)

          # Create PR
          pr_result = create_pull_request(
              task_number, issue_number, issue_title,
              milestone_number, commits, base_branch, branch_name
          )

          if pr_result:
              print(f"Successfully created PR for task {task_number}")
          else:
              print("Failed to create PR")
              sys.exit(1)
          PYTHON_SCRIPT
'''

    with open('.github/workflows/auto-pr-creation.yml', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(workflow_content)

def create_templates():
    """Create GitHub issue and PR templates"""
    # Issue template
    issue_template = '''---
name: Kiro Task
about: Template for tasks generated from Kiro planning
title: 'Task [NUMBER]: [TITLE]'
labels: ['task', 'enhancement']
assignees: ''
---

## Task Description
<!-- Detailed description of the task from Kiro tasks.md -->

## Requirements Covered
<!-- List the requirement IDs this task addresses from Kiro requirements.md -->

## Definition of Done
- [ ] Implementation completed
- [ ] Unit tests written and passing
- [ ] Code reviewed and approved
- [ ] Requirements validated against Kiro specifications
- [ ] Documentation updated

## Related Epic
<!-- This will be automatically filled: Part of epic #[EPIC_NUMBER] -->

## Kiro Planning References
- [Requirements](.kiro/specs/blob-url-files/requirements.md)
- [Design](.kiro/specs/blob-url-files/design.md)
- [Tasks](.kiro/specs/blob-url-files/tasks.md)

## Additional Context
<!-- Any additional context or notes specific to this task -->

---
*This issue was auto-generated from Kiro planning documents*
'''

    with open('.github/ISSUE_TEMPLATE/kiro-task.md', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(issue_template)

    # PR template
    pr_template = '''## Pull Request

### Description
Brief description of the changes implemented in this PR.

### Related Issues
- Resolves #[ISSUE_NUMBER]
- Related to Epic #[EPIC_NUMBER]

### Kiro Planning References
- [Requirements](.kiro/specs/blob-url-files/requirements.md)
- [Design](.kiro/specs/blob-url-files/design.md)
- [Tasks](.kiro/specs/blob-url-files/tasks.md)

### Changes Made
- [ ] Component implementation
- [ ] Service implementation
- [ ] Template updates
- [ ] Styling updates
- [ ] API integration
- [ ] Tests added

### Requirements Validation
<!-- Check off requirements from Kiro requirements.md that this PR addresses -->
- [ ] Requirement 1: User interface requirements met
- [ ] Requirement 2: Selection state requirements met
- [ ] Requirement 3: File operations requirements met
- [ ] Requirement 4: Permissions requirements met
- [ ] Requirement 5: Visual feedback requirements met
- [ ] Requirement 6: Progress tracking requirements met

### Testing Checklist
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Edge cases tested
- [ ] Error handling tested

### Code Quality Checklist
- [ ] Code follows project standards
- [ ] TypeScript types properly defined
- [ ] Error handling implemented
- [ ] Loading states implemented
- [ ] No console.log statements left

### Review Checklist
- [ ] Functionality matches Kiro requirements
- [ ] Code follows Kiro design specifications
- [ ] Performance considerations addressed
- [ ] Security considerations addressed
- [ ] Accessibility requirements met

### Screenshots/Demo
<!-- Add screenshots or GIF demos of the functionality -->

### Deployment Notes
<!-- Any special deployment considerations -->

---
*This PR was created as part of Kiro-planned feature development*
'''

    with open('.github/pull_request_template.md', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(pr_template)

def create_helper_scripts():
    """Create helper scripts"""
    # Task branch creation script
    branch_script = '''#!/bin/bash
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
'''

    with open('scripts/create-task-branch.sh', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(branch_script)
    os.chmod('scripts/create-task-branch.sh', 0o755)

    # Feature setup script
    setup_script = '''#!/bin/bash
# Quick setup script for new Kiro features

echo "🚀 Kiro Feature Setup"
echo "===================="

read -p "Enter project/feature name: " PROJECT_NAME
read -p "Enter milestone name (optional): " MILESTONE_NAME

echo "Setting up GitHub integration for: $PROJECT_NAME"

# Trigger the GitHub workflow
if [ -z "$MILESTONE_NAME" ]; then
    gh workflow run kiro-integration.yml \\
      -f project_name="$PROJECT_NAME" \\
      -f requirements_file=".kiro/specs/blob-url-files/requirements.md" \\
      -f design_file=".kiro/specs/blob-url-files/design.md" \\
      -f tasks_file=".kiro/specs/blob-url-files/tasks.md"
else
    gh workflow run kiro-integration.yml \\
      -f project_name="$PROJECT_NAME" \\
      -f milestone_name="$MILESTONE_NAME" \\
      -f requirements_file=".kiro/specs/blob-url-files/requirements.md" \\
      -f design_file=".kiro/specs/blob-url-files/design.md" \\
      -f tasks_file=".kiro/specs/blob-url-files/tasks.md"
fi

echo "✅ GitHub workflow triggered!"
echo "🔗 Check the Actions tab to see the progress"
echo "📋 Issues and milestone will be created automatically"
'''

    with open('scripts/setup-kiro-feature.sh', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(setup_script)
    os.chmod('scripts/setup-kiro-feature.sh', 0o755)

def create_documentation():
    """Create integration documentation"""
    doc_content = '''# Kiro to GitHub Integration

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
'''

    with open('kiro-github-integration.md', 'w', encoding='utf-8', newline='\n'
) as f:
        f.write(doc_content)

def check_tools():
    """Check if required tools are available"""
    print_info("Checking for required tools...")

    # Check GitHub CLI
    if shutil.which('gh'):
        print_status("GitHub CLI is available")

        # Check if authenticated
        try:
            subprocess.run(['gh', 'auth', 'status'],
                         capture_output=True, check=True)
            print_status("GitHub CLI is authenticated")
        except subprocess.CalledProcessError:
            print_warning("GitHub CLI is not authenticated. Run 'gh auth login' to authenticate.")
    else:
        print_warning("GitHub CLI (gh) is not installed. Please install it to use the helper scripts.")
        print_info("Install from: https://cli.github.com/")

def main():
    """Main setup function"""
    print("🚀 Setting up Kiro to GitHub Integration...")

    try:
        check_prerequisites()
        create_directory_structure()
        create_kiro_integration_workflow()
        create_auto_pr_workflow()
        create_templates()
        create_helper_scripts()
        create_documentation()
        check_tools()

        print_status("Kiro GitHub Integration setup complete!")

        print("\n📋 What was created:")
        print("   - GitHub workflows for automation")
        print("   - Issue and PR templates")
        print("   - Helper scripts in scripts/")
        print("   - Integration documentation")
        print("\n🚀 Next steps:")
        print("   1. Review your Kiro files in .kiro/specs/blob-url-files/")
        print("   2. Run: ./scripts/setup-kiro-feature.sh")
        print("   3. Start working on tasks using: ./scripts/create-task-branch.sh <task_number>")
        print("\n📖 For detailed instructions, see: kiro-github-integration.md")

    except Exception as e:
        print_error(f"Setup failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
