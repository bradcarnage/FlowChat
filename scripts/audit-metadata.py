#!/usr/bin/env python3
import re, os, subprocess, json

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)

# Fix 1.20.4 neoforge
f = "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
with open(f) as fh:
    c = fh.read()
c = re.sub(r'(modId\s*=\s*"minecraft".*?versionRange\s*=\s*")[^"]*', r'\g<1>[1.20.4,1.20.5)', c, flags=re.DOTALL)
with open(f, 'w') as fh:
    fh.write(c)
run(f"git add {f}")
run('git commit -m "Fix neoforge mods.toml: minecraft -> [1.20.4,1.20.5)"')
print("Fixed 1.20.4 neoforge")

# Now verify ALL branches have correct metadata - do a full audit
print("\n=== FINAL METADATA AUDIT ===")
branches = subprocess.run("git branch | grep multiplatform | sed 's/^..//'", 
    shell=True, capture_output=True, text=True).stdout.strip().split('\n')

for branch in sorted(branches):
    ver = branch.replace("multiplatform/", "")
    run(f"git checkout {branch}")
    
    results = []
    
    # Fabric
    fmj = "fabric/src/main/resources/fabric.mod.json"
    if os.path.exists(fmj):
        with open(fmj) as fh:
            d = json.load(fh)
        mc = d.get("depends", {}).get("minecraft", "?")
        results.append(f"fab={mc}")
    
    # Forge mods.toml
    fmt = "forge/src/main/resources/META-INF/mods.toml"
    if os.path.exists(fmt):
        with open(fmt) as fh:
            c = fh.read()
        m = re.search(r'modId\s*=\s*"minecraft".*?versionRange\s*=\s*"([^"]*)"', c, re.DOTALL)
        results.append(f"frg={m.group(1) if m else '?'}")
    
    # Forge mcmod.info
    fmi = "forge/src/main/resources/mcmod.info"
    if os.path.exists(fmi) and not os.path.exists(fmt):
        with open(fmi) as fh:
            d = json.load(fh)
        if isinstance(d, list):
            mc = d[0].get("mcversion", "?")
        else:
            mc = d.get("mcversion", "?")
        results.append(f"frg_mci={mc}")
    
    # NeoForge
    for nf in ["neoforge/src/main/resources/META-INF/neoforge.mods.toml",
               "neoforge/src/main/resources/META-INF/mods.toml"]:
        if os.path.exists(nf):
            with open(nf) as fh:
                c = fh.read()
            m = re.search(r'modId\s*=\s*"minecraft".*?versionRange\s*=\s*"([^"]*)"', c, re.DOTALL)
            results.append(f"neo={m.group(1) if m else '?'}")
            break
    
    print(f"  {ver:10s} | {' | '.join(results)}")
