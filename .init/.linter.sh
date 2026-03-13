#!/bin/bash
cd /home/kavia/workspace/code-generation/github-repository-access-mapper-43869-43898/github_access_report_backend
./gradlew checkstyleMain
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

