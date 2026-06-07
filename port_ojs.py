#!/usr/bin/env python3
"""
Port onJoinServer feature to all FlowChat branches and bump version to 2.2.0.
Run from ~/Developer/FlowChat/
"""
import subprocess, sys, os, json

REPO = os.path.expanduser("~/Developer/FlowChat")
NEW_VERSION = "2.2.0"

# Branches that need onJoinServer ported (all except 1.21.11 which has it, 1.7.10 which has it in worktree)
BRANCHES = [
    "1.8.9", "1.9.4", "1.10.2", "1.11.2", "1.12.2",
    "1.14.4", "1.15.2", "1.16.5", "1.17.1", "1.18.2",
    "1.19.2", "1.19.4", "1.20.1", "1.20.4", "1.20.6",
    "1.21.1", "1.21.4", "1.21.5", "1.21.9"
]

# Platform loaders per branch
BRANCH_PLATFORMS = {
    "1.8.9": ["forge"], "1.9.4": ["forge"], "1.10.2": ["forge"],
    "1.11.2": ["forge"], "1.12.2": ["forge"],
    "1.14.4": ["fabric", "forge"], "1.15.2": ["fabric"],
    "1.16.5": ["fabric", "forge"], "1.17.1": ["fabric"],
    "1.18.2": ["fabric", "forge"], "1.19.2": ["fabric", "forge"],
    "1.19.4": ["fabric", "forge"], "1.20.1": ["fabric"],
    "1.20.4": ["fabric", "forge", "neoforge"],
    "1.20.6": ["fabric", "forge", "neoforge"],
    "1.21.1": ["fabric", "forge", "neoforge"],
    "1.21.4": ["fabric", "forge", "neoforge"],
    "1.21.5": ["fabric", "forge", "neoforge"],
    "1.21.9": ["fabric", "forge", "neoforge"],
}

# Forge API eras determine how to add the onJoinServer handler
# Era 1: 1.8.9-1.12.2 — @Mod(modid=...), @EventHandler init, Log4j, ChatComponentText/TextComponentString, no FMLLoader/Dist
# Era 2: 1.14.4-1.15.2 — transition (Log4j on forge, SLF4J on fabric, Forge has FMLLoader)  
# Era 3: 1.16.5-1.18.2 — @Mod("flowchat"), FMLLoader, Log4j on forge
# Era 4: 1.19.2+ — @Mod("flowchat"), FMLLoader, SLF4J, Component.literal
FORGE_ERA = {
    "1.8.9": 1, "1.9.4": 1, "1.10.2": 1, "1.11.2": 1, "1.12.2": 1,
    "1.14.4": 2, "1.16.5": 3, "1.18.2": 3,
    "1.19.2": 4, "1.19.4": 4, "1.20.1": 4, "1.20.4": 4, "1.20.6": 4,
    "1.21.1": 4, "1.21.4": 4, "1.21.5": 4, "1.21.9": 4,
}

def run(cmd, cwd=REPO, check=True):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, cwd=cwd)
    if check and r.returncode != 0:
        print(f"FAIL: {cmd}")
        print(r.stderr)
        return None
    return r.stdout.strip()

def cherry_pick_common(branch):
    """Cherry-pick the onJoinServer commit for common code only."""
    # We'll use git show to extract the patch and apply manually
    # This avoids conflicts from loader files
    
    # Check if onJoinServer already exists
    out = run(f"git show multiplatform/{branch}:common/src/main/java/computer/brads/flowchat/core/FlowChatConfig.java | grep -c onJoinServer", check=False)
    if out and int(out) > 0:
        print(f"  [SKIP] Common code already has onJoinServer")
        return True
    
    # Generate patch for common code only from f7b5f5e
    run(f"git format-patch -1 f7b5f5e --stdout -- common/ example_rules.json > /tmp/ojs_common.patch", check=False)
    
    # Apply it
    result = run(f"git apply --3way /tmp/ojs_common.patch", check=False)
    if result is None:
        # Try with more tolerance
        result = run(f"git apply --3way --reject /tmp/ojs_common.patch", check=False)
    return True

print("FlowChat onJoinServer port script")
print(f"Target version: {NEW_VERSION}")
print(f"Branches: {len(BRANCHES)}")
print()

# Just output the plan for now
for b in BRANCHES:
    platforms = BRANCH_PLATFORMS[b]
    forge_era = FORGE_ERA.get(b, "N/A")
    print(f"  {b:>8}: platforms={','.join(platforms)}, forge_era={forge_era}")
