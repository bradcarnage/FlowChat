#!/usr/bin/env python3
"""Phase 4: Build all branches × all loaders, collect JAR paths and status."""
import subprocess, os, sys, json

os.chdir(os.path.expanduser("~/Developer/FlowChat"))

def run(cmd, timeout=600):
    try:
        r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
        return r.returncode, r.stdout + r.stderr
    except subprocess.TimeoutExpired:
        return -1, "TIMEOUT"

def jdk_for_branch(mc):
    if mc.startswith("26"):
        return os.path.expanduser("~/jdk/jdk-25.0.3+9")
    v = mc.split(".")
    if len(v) >= 2:
        major, minor = int(v[0]), int(v[1])
        if major == 1 and minor >= 21:
            return os.path.expanduser("~/jdk/jdk-21.0.11+10")
        if major == 1 and minor == 20 and len(v) >= 3 and int(v[2]) >= 5:
            return os.path.expanduser("~/jdk/jdk-21.0.11+10")
    return os.path.expanduser("~/jdk/jdk-17.0.19+10")

def fg2_jdk():
    return os.path.expanduser("~/jdk/jdk8u492-b09")

branches = [
    "1.7.10", "1.8.9", "1.9.4", "1.10.2", "1.11.2", "1.12.2",
    "1.14.4", "1.15.2", "1.16.1", "1.16.5",
    "1.17.1", "1.18.2", "1.19", "1.19.1", "1.19.2",
    "1.19.4", "1.20.1", "1.20.4", "1.20.6",
    "1.21.1", "1.21.5", "1.21.9", "1.21.11", "26.1.2"
]

results = {}

for mc in branches:
    branch = f"multiplatform/{mc}"
    rc, out = run(f"git checkout {branch} 2>&1")
    actual = run("git branch --show-current")[1].strip()
    if actual != branch:
        results[mc] = {"status": "SKIP", "error": "checkout failed"}
        continue

    jdk = jdk_for_branch(mc)
    env = f"JAVA_HOME={jdk} PATH={jdk}/bin:$PATH"
    
    results[mc] = {"loaders": {}}
    
    # Clean first
    run(f"{env} ./gradlew clean --no-daemon 2>&1", timeout=120)
    
    # Fabric (root gradle project)
    if os.path.exists("fabric/build.gradle"):
        rc, out = run(f"{env} ./gradlew :fabric:build --no-daemon --no-build-cache -x test 2>&1", timeout=300)
        if rc == 0:
            _, jars = run("find fabric/build/libs -name '*.jar' -not -name '*sources*' -not -name '*dev*' 2>/dev/null | head -3")
            results[mc]["loaders"]["fabric"] = {"status": "PASS", "jars": jars.strip()}
        else:
            err = out.strip().split('\n')[-3:]
            results[mc]["loaders"]["fabric"] = {"status": "FAIL", "error": '\n'.join(err)}
    
    # Forge (standalone)
    if os.path.exists("forge/build.gradle"):
        # Check if FG2 (needs JDK8)
        with open("forge/build.gradle") as f:
            fg_content = f.read()
        
        if "2.1-SNAPSHOT" in fg_content or "2.2-SNAPSHOT" in fg_content:
            forge_jdk = fg2_jdk()
            forge_env = f"JAVA_HOME={forge_jdk} PATH={forge_jdk}/bin:$PATH"
            forge_cmd = f"cd forge && {forge_env} ./gradlew build --no-daemon --no-build-cache -x test 2>&1"
        elif "RetroFuturaGradle" in fg_content:
            forge_env = env  # RFG runs on JDK17 but needs JDK8 toolchain
            forge_cmd = f"cd forge && {forge_env} ./gradlew build --no-daemon --no-build-cache -x test 2>&1"
        else:
            forge_env = env
            # Check for standalone gradlew
            if os.path.exists("forge/gradlew"):
                forge_cmd = f"cd forge && {forge_env} ./gradlew build --no-daemon --no-build-cache -x test 2>&1"
            else:
                forge_cmd = f"{forge_env} ./gradlew :forge:build --no-daemon --no-build-cache -x test 2>&1"
        
        rc, out = run(forge_cmd, timeout=300)
        if rc == 0:
            _, jars = run("find forge/build/libs -name '*.jar' -not -name '*sources*' -not -name '*dev*' -not -name '*slim*' 2>/dev/null | head -3")
            results[mc]["loaders"]["forge"] = {"status": "PASS", "jars": jars.strip()}
        else:
            err = out.strip().split('\n')[-3:]
            results[mc]["loaders"]["forge"] = {"status": "FAIL", "error": '\n'.join(err)}
    
    # NeoForge (standalone)
    if os.path.exists("neoforge/build.gradle"):
        if os.path.exists("neoforge/gradlew"):
            neo_cmd = f"cd neoforge && {env} ./gradlew build --no-daemon --no-build-cache -x test 2>&1"
        else:
            neo_cmd = f"{env} ./gradlew :neoforge:build --no-daemon --no-build-cache -x test 2>&1"
        
        rc, out = run(neo_cmd, timeout=300)
        if rc == 0:
            _, jars = run("find neoforge/build/libs -name '*.jar' -not -name '*sources*' -not -name '*dev*' 2>/dev/null | head -3")
            results[mc]["loaders"]["neoforge"] = {"status": "PASS", "jars": jars.strip()}
        else:
            err = out.strip().split('\n')[-3:]
            results[mc]["loaders"]["neoforge"] = {"status": "FAIL", "error": '\n'.join(err)}
    
    # Common tests
    rc, out = run(f"{env} ./gradlew :common:test --no-daemon --no-build-cache 2>&1", timeout=120)
    results[mc]["loaders"]["common-test"] = {"status": "PASS" if rc == 0 else "FAIL"}

    print(f"{mc:8s} ", end="", flush=True)
    for loader, info in results[mc].get("loaders", {}).items():
        print(f" {loader}={info['status']}", end="")
    print()

# Summary
print("\n=== FULL BUILD MATRIX ===")
print(f"{'MC':8s} {'common':8s} {'fabric':8s} {'forge':8s} {'neoforge':8s}")
for mc in branches:
    if "loaders" not in results[mc]:
        print(f"{mc:8s} SKIP")
        continue
    loaders = results[mc]["loaders"]
    common = loaders.get("common-test", {}).get("status", "-")
    fabric = loaders.get("fabric", {}).get("status", "-")
    forge = loaders.get("forge", {}).get("status", "-")
    neo = loaders.get("neoforge", {}).get("status", "-")
    print(f"{mc:8s} {common:8s} {fabric:8s} {forge:8s} {neo:8s}")

# Save results
with open("/tmp/flowchat-build-results.json", "w") as f:
    json.dump(results, f, indent=2)
print(f"\nResults saved to /tmp/flowchat-build-results.json")
