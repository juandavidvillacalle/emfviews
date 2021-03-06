#!/bin/bash

# Deploy the built EMF Views manual by committing on top of the gh-pages branch,
# preserving history because the gh-pages branch also contains other part of the
# website which are not updated by hand.  Besides, the generated HTML is small.

export DEPLOY_REPO=`git config remote.origin.url`
export TARGET_BRANCH="gh-pages"
export KEEP_HISTORY=true
export SRC_FOLDER="doc/html"
export DEST_FOLDER="manual"
export DEPLOY_KEY="deploy-key-manual"

export OUT_DIR="out-manual"

bash .travis/safe-deploy.sh
