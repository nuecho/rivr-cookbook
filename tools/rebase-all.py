#!/usr/bin/env python

"""\
Show rebase command order to rebase all Cookbook recipes on a new trunk.

Usage: rebase-all.py <location of .git reference>
"""

__metaclass__ = type
import subprocess, re, sys

class Main:

    parentIds = dict()
    parentBranches = dict()
    ids = dict()
    branches = dict()
        
    def main(self, *arguments):
        git_dir_ref = arguments[0]

        for line in self.getLinesFromProcess(['git', '--git-dir=%s' % git_dir_ref, 'for-each-ref', 'refs/heads/', '--format=%(refname:short)']):
            branch = line.strip()
            #print "Reading %s" % branch
            if (branch == 'wikidoc'):
                continue
            
            id = self.getLinesFromProcess(['git', '--git-dir=%s' % git_dir_ref,'rev-parse', branch+"^0"])[0].strip()
            parentId = self.getLinesFromProcess(['git', '--git-dir=%s' % git_dir_ref, 'rev-parse', branch+"~1"])[0].strip()
            
            self.ids[branch]=id
            self.branches[id]=branch
            self.parentIds[branch]=parentId
            
        #resolve branch hierarchy
        for branch in self.ids: 
            #print "Resolving hierarchy of %s" % branch;
            parentId = self.parentIds[branch]
            if parentId in self.branches:
                self.parentBranches[branch]=self.branches[parentId]
                #print "Parent branch of %s is %s" % (branch, self.parentBranches[branch])
                
        
        processed = set()
        
        for branch in sorted(self.ids):
            
            plan = []
            root = False
            while (not root):
                plan.append(branch)
                if branch in self.parentBranches:
                    if branch == self.parentBranches[branch]:
                        root = True
                    else:
                        branch = self.parentBranches[branch]
                else:
                    root = True 

            for currentBranch in reversed(plan):
                if (not currentBranch in processed):
                    if not currentBranch in self.parentBranches:
                        continue

                    parent = self.parentBranches[currentBranch]
                    
                    print "git rebase -X theirs %s~1 %s --onto=%s" % (currentBranch, currentBranch, parent)
                    processed.add(currentBranch)
            

    def getLinesFromProcess(self, args, failOnError=True):
       	process = subprocess.Popen(args, stdout=subprocess.PIPE)
        lines = list(process.stdout)

        process.wait()
        if process.returncode != 0 and failOnError:
            sys.exit("Error while calling: " + " ".join(args))
        return lines

    

run = Main()
main = run.main
if __name__ == '__main__':
    main(*sys.argv[1:])
