#!/bin/bash
# Port onJoinServer and bump version across all FlowChat branches
set -e

REPO=~/Developer/FlowChat
COMMIT="f7b5f5e"
NEW_VER="2.2.0"
export JAVA_HOME=~/jdk21
export PATH=~/jdk21/bin:$PATH

cd "$REPO"

BRANCHES=(
    1.8.9 1.9.4 1.10.2 1.11.2 1.12.2
    1.14.4 1.15.2 1.16.5 1.17.1 1.18.2
    1.19.2 1.19.4 1.20.1 1.20.4 1.20.6
    1.21.1 1.21.4 1.21.5 1.21.9
)

RESULTS_FILE="/tmp/flowchat_port_results.txt"
> "$RESULTS_FILE"

for VER in "${BRANCHES[@]}"; do
    BRANCH="multiplatform/$VER"
    echo "=========================================="
    echo "Processing $BRANCH"
    echo "=========================================="

    # Checkout branch
    git checkout "$BRANCH" 2>&1 || { echo "FAIL: checkout $BRANCH" >> "$RESULTS_FILE"; continue; }

    # Cherry-pick onJoinServer commit
    HAS_OJS=$(grep -c "onJoinServer" common/src/main/java/computer/brads/flowchat/core/FlowChatConfig.java 2>/dev/null || echo 0)
    if [ "$HAS_OJS" -gt 0 ]; then
        echo "  [SKIP] Already has onJoinServer"
    else
        if git cherry-pick "$COMMIT" --no-edit 2>&1; then
            echo "  [OK] Cherry-picked onJoinServer"
        else
            echo "  [FAIL] Cherry-pick conflict" 
            echo "FAIL: cherry-pick $VER" >> "$RESULTS_FILE"
            git cherry-pick --abort 2>/dev/null
            continue
        fi
    fi

    # Version bump
    CURRENT_VER=$(grep 'mod_version=' gradle.properties | cut -d= -f2)
    if [ "$CURRENT_VER" != "$NEW_VER" ]; then
        sed -i "s/mod_version=.*/mod_version=$NEW_VER/" gradle.properties
        
        # Also update version in Forge @Mod annotation if it exists (old era)
        find forge/src -name "FlowChatForge.java" 2>/dev/null | while read f; do
            sed -i "s/version = \"[0-9.]*\"/version = \"$NEW_VER\"/" "$f" 2>/dev/null
        done
        
        git add -A
        git commit -m "Bump version to $NEW_VER" --no-edit 2>&1
        echo "  [OK] Version bumped to $NEW_VER"
    else
        echo "  [SKIP] Already at $NEW_VER"
    fi

    # Build
    echo "  Building..."
    if ./gradlew build --no-daemon 2>&1 | tail -5; then
        echo "  [OK] Build successful"
    else
        echo "FAIL: build $VER" >> "$RESULTS_FILE"
        echo "  [FAIL] Build failed"
        continue
    fi

    # Run tests
    echo "  Testing..."
    if ./gradlew :common:test --rerun --no-daemon 2>&1 | tail -5; then
        echo "  [OK] Tests passed"
    else
        echo "FAIL: test $VER" >> "$RESULTS_FILE"
        echo "  [FAIL] Tests failed"
        continue
    fi

    echo "OK: $VER" >> "$RESULTS_FILE"
    echo "  === $VER COMPLETE ==="
done

echo ""
echo "=========================================="
echo "RESULTS:"
echo "=========================================="
cat "$RESULTS_FILE"
