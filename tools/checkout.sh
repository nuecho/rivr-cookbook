#!/bin/sh

reference=cookbook

mkdir recipes
for branch in `GIT_DIR=$reference/.git git for-each-ref refs/heads --format "%(refname:short)"`; do
	git clone $reference -b $branch recipes/$branch 
        echo "rootProject.name = 'rivr-cookbook-$branch'" > recipes/$branch/settings.gradle
        #(cd recipes/$branch && ./gradlew eclipse)
done
