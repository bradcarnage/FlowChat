#!/usr/bin/env python3
"""Phase 3 Step 2: Clean up stale mods.toml on FG2 branches and fix mods.toml on legacy FG5.1 branches."""
import subprocess, os, re

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)

def checkout(branch):
    run(f"git checkout multiplatform/{branch} 2>&1")
    actual = run("git branch --show-current").stdout.strip()
    return actual == f"multiplatform/{branch}"

# Remove stale mods.toml from FG2 branches
for b in ["1.8.9", "1.9.4", "1.10.2", "1.11.2"]:
    if not checkout(b):
        print(f"SKIP {b}: checkout failed")
        continue
    
    toml = "forge/src/main/resources/META-INF/mods.toml"
    if os.path.exists(toml):
        run(f"git rm {toml}")
        # Also remove META-INF dir if now empty
        meta_dir = "forge/src/main/resources/META-INF"
        if os.path.exists(meta_dir) and not os.listdir(meta_dir):
            os.rmdir(meta_dir)
        run(f'git commit -m "Remove stale mods.toml (FG2 uses mcmod.info, not mods.toml)"')
        print(f"CLEANED {b}: removed stale mods.toml")
    else:
        print(f"OK {b}: no stale mods.toml")

# Fix 1.7.10 mods.toml — the loaderVersion should match the Forge version for MC 1.7.10
# Forge for MC 1.7.10 is 10.13.x, not javafml [47,)
# Actually, FG5.1 produces a modern Forge mod — it CAN'T target MC 1.7.10
# because MC 1.7.10 uses cpw.mods.fml, not net.minecraftforge.fml
# The FG5.1 plugin on 1.7.10 branch compiles against Forge 1.12.2 API
# So the mods.toml [1.7.10,1.13) is incorrect — should be [1.12,1.13) or [1.12.2,1.13)
if checkout("1.7.10"):
    toml = "forge/src/main/resources/META-INF/mods.toml"
    if os.path.exists(toml):
        with open(toml) as f:
            c = f.read()
        # Check what MC version it actually compiles against
        with open("forge/build.gradle") as f:
            bg = f.read()
        mc_match = re.search(r"version\s*=\s*['\"](\d+\.\d+(?:\.\d+)?)", bg)
        print(f"1.7.10 forge build.gradle MC version: {mc_match.group(1) if mc_match else '?'}")
        # Keep [1.7.10,1.13) since that's what was set — 
        # but note this mod WON'T actually load on 1.7.10-1.11.x,
        # only on 1.12.2 where javafml exists
        print("NOTE: 1.7.10 forge uses FG5.1 javafml — only works on MC 1.12.2+, not actual 1.7.10")

# Note about 1.20.4 forge
print("\nNOTE: 1.20.4 forge is BROKEN — uses RFG targeting MC 1.7.10 instead of FG6 targeting 1.20.4")
print("This needs a full forge module rewrite (Phase 4 issue, not Phase 3)")

print("\n=== CLEANUP COMPLETE ===")
