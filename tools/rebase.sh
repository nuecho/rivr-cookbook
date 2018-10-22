#!/bin/sh

if [ -z "$1" ]; then
  echo "Usage: "
  echo "  $0 <old-master>"
  exit -1
fi

old_master=$1
for branch in `git for-each-ref --format="%(refname:short)" refs/heads/\*`; do
  (git rev-parse temp-rebase-$branch || git tag temp-rebase-$branch $branch) > /dev/null
done

(git rev-parse $old_master || git tag temp-rebase-master $old_master) > /dev/null 

check_not_merged() {
  (git rev-parse $1 2>&1 > /dev/null) || (echo "Missing $base branch." ; exit 1)
  list=$(git for-each-ref --no-contains=master refs/heads/$1)
  if [ -z "$list" ]; then
    return 1
  else
    return 0
  fi
}

rebase() {
  target=$1
  branch=$2
  base="temp-rebase-$target"
  git rev-parse $base > /dev/null || (echo "Missing $base base branch." ; exit 1)
  git rev-parse $target > /dev/null  || (echo "Missing target $target branch." ; exit 1)
  git rev-parse $branch > /dev/null || (echo "Missing $branch branch." ; exit 1)
  if check_not_merged $branch; then
    echo "+$branch: processing"
    echo "  git rebase --onto=$target $base $branch"
    git rebase --onto=$target $base $branch ||  exit 1
    echo "$branch rebased successfully"
  else
    echo "+$branch: already rebased."
  fi
}

rebase "master" "hello-world"
rebase "master" "hello-world-2"
rebase "master" "clid-dnis"
rebase "master" "spring"
rebase "master" "last-turn-base"
rebase "master" "log-turn"
rebase "master" "genesys-gvp-uuid"
rebase "master" "genesys-gvp-session-id"
rebase "master" "genesys-gvp-read-user-data"
rebase "master" "http-parameters"
rebase "master" "junit-dialogue-test"
rebase "master" "transfer-blind"
rebase "master" "transfer-consultation"

rebase "master" "interaction-base"

rebase "interaction-base" "simple-speech-interaction"
rebase "interaction-base" "speech-options"
rebase "interaction-base" "recording-to-variable"
rebase "interaction-base" "simple-recording"

rebase "interaction-base" "simple-dtmf-interaction"
rebase "simple-dtmf-interaction" "interruptible-message"
rebase "simple-dtmf-interaction" "dtmf-options"
rebase "simple-dtmf-interaction" "dtmf-grammar"
rebase "simple-dtmf-interaction" "dtmf-grammar-with-semantic"
rebase "simple-dtmf-interaction" "barge-in-dtmf-interaction"

rebase "master" "subdialogue-invocation"
rebase "subdialogue-invocation" "subdialogue-invocation-voicexml-parameters"
rebase "subdialogue-invocation" "subdialogue-invocation-http-parameters"

rebase "master" "subdialogue-implementation"
rebase "subdialogue-implementation" "subdialogue-implementation-voicexml-parameter"

rebase "master" "message-base"
rebase "message-base" "message-audio-file"
rebase "message-base" "message-audio-file-with-alternate"
rebase "message-base" "message-barge-in"
rebase "message-base" "message-language"
rebase "message-base" "message-multiple-items"
rebase "message-base" "message-pause"
rebase "message-base" "message-speech-synthesis"
rebase "message-base" "message-ssml"
