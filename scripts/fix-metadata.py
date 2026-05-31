#!/usr/bin/env python3
"""Fix wrong version metadata across all FlowChat branches."""

import subprocess
import re
import json
import os

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
    return r.stdout + r.stderr

def checkout(branch):
    r = run(f"git checkout multiplatform/{branch} 2>&1")
    actual = run("git branch --show-current").strip()
    return actual == f"multiplatform/{branch}"

def fix_forge_mods_toml(branch, mc_range):
    """Fix minecraft versionRange in forge mods.toml."""
    if not checkout(branch):
        print(f"  SKIP {branch}: checkout failed")
        return
    
    toml_path = "forge/src/main/resources/META-INF/mods.toml"
    if not os.path.exists(toml_path):
        print(f"  SKIP {branch} forge: no mods.toml")
        return
    
    with open(toml_path) as f:
        content = f.read()
    
    # Find minecraft versionRange
    m = re.search(r'(modId\s*=\s*"minecraft".*?versionRange\s*=\s*")([^"]*)', content, re.DOTALL)
    if not m:
        print(f"  WARN {branch} forge: no minecraft versionRange")
        return
    
    current = m.group(2)
    if current == mc_range:
        print(f"  OK   {branch} forge: mc={current}")
        return
    
    new_content = content[:m.start(2)] + mc_range + content[m.end(2):]
    with open(toml_path, 'w') as f:
        f.write(new_content)
    
    run(f"git add {toml_path}")
    run(f'git commit -m "Fix forge mods.toml: minecraft {current} -> {mc_range}"')
    print(f"  FIXED {branch} forge: mc {current} -> {mc_range}")

def fix_neoforge_mods_toml(branch, mc_range):
    """Fix minecraft versionRange in neoforge mods.toml."""
    if not checkout(branch):
        print(f"  SKIP {branch}: checkout failed")
        return
    
    for toml_path in ["neoforge/src/main/resources/META-INF/neoforge.mods.toml",
                      "neoforge/src/main/resources/META-INF/mods.toml"]:
        if os.path.exists(toml_path):
            break
    else:
        print(f"  SKIP {branch} neoforge: no mods.toml")
        return
    
    with open(toml_path) as f:
        content = f.read()
    
    m = re.search(r'(modId\s*=\s*"minecraft".*?versionRange\s*=\s*")([^"]*)', content, re.DOTALL)
    if not m:
        print(f"  WARN {branch} neoforge: no minecraft versionRange")
        return
    
    current = m.group(2)
    if current == mc_range:
        print(f"  OK   {branch} neoforge: mc={current}")
        return
    
    new_content = content[:m.start(2)] + mc_range + content[m.end(2):]
    with open(toml_path, 'w') as f:
        f.write(new_content)
    
    run(f"git add {toml_path}")
    run(f'git commit -m "Fix neoforge mods.toml: minecraft {current} -> {mc_range}"')
    print(f"  FIXED {branch} neoforge: mc {current} -> {mc_range}")

def fix_fabric_mod_json(branch, mc_range):
    """Fix minecraft depends in fabric.mod.json."""
    if not checkout(branch):
        print(f"  SKIP {branch}: checkout failed")
        return
    
    json_path = "fabric/src/main/resources/fabric.mod.json"
    if not os.path.exists(json_path):
        print(f"  SKIP {branch} fabric: no fabric.mod.json")
        return
    
    with open(json_path) as f:
        d = json.load(f)
    
    current = d.get("depends", {}).get("minecraft", "?")
    if current == mc_range:
        print(f"  OK   {branch} fabric: mc={current}")
        return
    
    d["depends"]["minecraft"] = mc_range
    with open(json_path, 'w') as f:
        json.dump(d, f, indent=2)
    
    run(f"git add {json_path}")
    run(f'git commit -m "Fix fabric.mod.json: minecraft {current} -> {mc_range}"')
    print(f"  FIXED {branch} fabric: mc {current} -> {mc_range}")

# Also fix the forge loaderVersion on legacy branches
def fix_forge_loader_version(branch, loader_range):
    """Fix forge loaderVersion in mods.toml."""
    if not checkout(branch):
        return
    
    toml_path = "forge/src/main/resources/META-INF/mods.toml"
    if not os.path.exists(toml_path):
        return
    
    with open(toml_path) as f:
        content = f.read()
    
    # Fix loaderVersion
    m = re.search(r'(loaderVersion\s*=\s*")([^"]*)', content)
    if m:
        current = m.group(2)
        if current != loader_range:
            new_content = content[:m.start(2)] + loader_range + content[m.end(2):]
            
            # Also fix forge modId dependency versionRange
            m2 = re.search(r'(modId\s*=\s*"forge".*?versionRange\s*=\s*")([^"]*)', new_content, re.DOTALL)
            if m2:
                new_content = new_content[:m2.start(2)] + loader_range + new_content[m2.end(2):]
            
            with open(toml_path, 'w') as f:
                f.write(new_content)
            run(f"git add {toml_path}")
            run(f'git commit -m "Fix forge loaderVersion: {current} -> {loader_range}"')
            print(f"  FIXED {branch} forge loader: {current} -> {loader_range}")

print("=== FIXING FORGE METADATA ===")
forge_fixes = {
    "1.7.10": "[1.7.10,1.13)",
    "1.12.2": "[1.12.2,1.13)",
    "1.14.4": "[1.14.4,1.15)",
    "1.16.1": "[1.16.1,1.17)",
    "1.16.5": "[1.16.5,1.17)",
    "1.18.2": "[1.18.2,1.19)",
    "1.19": "[1.19,1.20)",
    "1.19.1": "[1.19.1,1.20)",
    "1.19.2": "[1.19.2,1.20)",
    "1.19.4": "[1.19.4,1.20)",
    "1.20.1": "[1.20.1,1.20.2)",
    "1.20.2": "[1.20.2,1.20.5)",
    "1.20.4": "[1.20.4,1.20.5)",
    "1.20.6": "[1.20.6,1.21)",
    "1.21.1": "[1.21.1,1.22)",
    "1.21.5": "[1.21.5,1.22)",
    "1.21.9": "[1.21.9,1.22)",
    "1.21.11": "[1.21.11,1.22)",
    "26.1.2": "[26.1.2,26.2)",
}
for branch, mc_range in forge_fixes.items():
    fix_forge_mods_toml(branch, mc_range)

print("\n=== FIXING NEOFORGE METADATA ===")
neoforge_fixes = {
    "1.20.2": "[1.20.2,1.20.5)",
    "1.20.4": "[1.20.4,1.20.5)",
    "1.20.6": "[1.20.6,1.21)",
    "1.21.1": "[1.21.1,1.22)",
    "1.21.5": "[1.21.5,1.22)",
    "1.21.9": "[1.21.9,1.22)",
    "1.21.11": "[1.21.11,1.22)",
    "26.1.2": "[26.1.2,26.2)",
}
for branch, mc_range in neoforge_fixes.items():
    fix_neoforge_mods_toml(branch, mc_range)

print("\n=== FIXING FABRIC METADATA ===")
# Fabric uses node-semver: ~1.19 = >=1.19.0 <1.20.0
# These should be correct already (targeting own version)
fabric_fixes = {
    "1.14.4": "~1.14.4",
    "1.15.2": "~1.15.2",
    "1.16.1": "~1.16.1",
    "1.16.5": "~1.16.5",
    "1.17.1": "~1.17.1",
    "1.18.2": "~1.18.2",
    "1.19": "~1.19",
    "1.19.1": "~1.19.1",
    "1.19.2": "~1.19.2",
    "1.19.4": "~1.19.4",
    "1.20.1": "~1.20.1",
    "1.20.2": "~1.20.2",
    "1.20.4": "~1.20.4",
    "1.20.6": "~1.20.6",
    "1.21.1": "~1.21.1",
    "1.21.5": "~1.21.5",
    "1.21.9": ">=1.21.9 <=1.21.11",  # already fixed
    "1.21.11": "~1.21.11",
    "26.1.2": "~26.1.2",
}
for branch, mc_range in fabric_fixes.items():
    fix_fabric_mod_json(branch, mc_range)

print("\n=== DONE ===")
