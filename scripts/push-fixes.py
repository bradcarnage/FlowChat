#!/usr/bin/env python3
"""Push metadata fixes to all remotes."""
import subprocess, os

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=60)
    return r.returncode, r.stdout + r.stderr

modified = [
    "1.8.9", "1.9.4", "1.10.2", "1.11.2",
    "1.19.2", "1.20.4", "1.20.6",
    "1.21.1", "1.21.5", "1.21.9", "1.21.11",
]

for branch in modified:
    full = f"multiplatform/{branch}"
    run(f"git checkout {full} 2>&1")
    
    # Push to all remotes that have this branch
    for remote in ["origin", "forgejo-brad", "github"]:
        rc, out = run(f"git push {remote} {full} 2>&1")
        status = "OK" if rc == 0 else "SKIP"
        if "Everything up-to-date" in out:
            status = "NOOP"
        elif "does not appear to be" in out or "Could not read" in out:
            status = "NO_REMOTE"
        print(f"  {branch:10s} → {remote:15s} {status}")
