#!/usr/bin/env python3
"""Phase 3 Step 3: Verify builds still pass on all modified branches."""
import subprocess, os, sys

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd, timeout=300):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    return r.returncode, r.stdout, r.stderr

# Branches with metadata changes
modified = [
    "1.8.9", "1.9.4", "1.10.2", "1.11.2",  # removed stale mods.toml
    "1.19.2",  # fixed fabric ~1.19.1 -> ~1.19.2
    "1.20.4",  # fixed neoforge [1.21,) -> [1.20.4,1.20.5)
    "1.20.6",  # fixed neoforge [1.21,) -> [1.20.6,1.21)
    "1.21.1",  # fixed neoforge [1.21,) -> [1.21.1,1.22)
    "1.21.5",  # fixed forge [1.20.1,) -> [1.21.5,1.22) + neoforge [1.21,) -> [1.21.5,1.22)
    "1.21.9",  # fixed fabric ~1.21.11 -> >=1.21.9 <=1.21.11, forge + neoforge
    "1.21.11", # fixed forge [1.20.1,) -> [1.21.11,1.22), neoforge [1.21,) -> [1.21.11,1.22)
]

results = []
for branch in modified:
    run(f"git checkout multiplatform/{branch} 2>&1")
    actual_branch = run("git branch --show-current")[1].strip()
    if actual_branch != f"multiplatform/{branch}":
        results.append((branch, "SKIP", "checkout failed"))
        continue
    
    # Clean build
    rc, out, err = run("./gradlew clean build --no-daemon -x test 2>&1", timeout=300)
    if rc == 0:
        # Find built JARs
        _, jars, _ = run("find . -path '*/build/libs/*.jar' -not -name '*-sources*' -not -name '*-dev*' | head -5")
        results.append((branch, "PASS", jars.strip()))
    else:
        # Get last 5 lines of error
        error_lines = (out + err).strip().split('\n')[-5:]
        results.append((branch, "FAIL", '\n'.join(error_lines)))

print("=== BUILD VERIFICATION RESULTS ===")
for branch, status, detail in results:
    print(f"  {branch:10s} {status:5s} {detail}")
    
pass_count = sum(1 for _, s, _ in results if s == "PASS")
fail_count = sum(1 for _, s, _ in results if s == "FAIL")
skip_count = sum(1 for _, s, _ in results if s == "SKIP")
print(f"\n  {pass_count} PASS / {fail_count} FAIL / {skip_count} SKIP out of {len(modified)}")
