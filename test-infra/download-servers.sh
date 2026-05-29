#!/bin/bash
# Download server JARs for FlowChat integration testing
set -e
cd ~/Developer/FlowChat/test-infra/servers

echo "=== Downloading server JARs ==="

# Paper API for downloading
PAPER_API="https://api.papermc.io/v2/projects/paper/versions"

download_paper() {
    local mc_version=$1
    local dir="paper-${mc_version}"
    mkdir -p "$dir"
    
    if [ -f "$dir/server.jar" ]; then
        echo "  $mc_version: already exists"
        return
    fi
    
    echo "  Downloading Paper $mc_version..."
    local build=$(curl -s "${PAPER_API}/${mc_version}/builds" | python3 -c "import json,sys; builds=json.load(sys.stdin)['builds']; print(builds[-1]['build'])" 2>/dev/null)
    if [ -z "$build" ]; then
        echo "  WARN: No Paper build for $mc_version, trying Spigot..."
        return 1
    fi
    local filename=$(curl -s "${PAPER_API}/${mc_version}/builds/${build}" | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['application']['name'])" 2>/dev/null)
    curl -sL "${PAPER_API}/${mc_version}/builds/${build}/downloads/${filename}" -o "$dir/server.jar"
    echo "  $mc_version: downloaded (build $build)"
}

# Download each version
for v in 1.21.4 1.20.4 1.20.1 1.19.2 1.18.2 1.16.5; do
    download_paper "$v" || true
done

# 1.12.2 and 1.8.9 need Spigot (Paper doesn't support them well)
# Use BuildTools or prebuilt. For now, skip — use Paper for what's available.
for v in 1.12.2 1.8.9; do
    mkdir -p "spigot-${v}"
    if [ ! -f "spigot-${v}/server.jar" ]; then
        echo "  $v: needs Spigot BuildTools (manual setup required)"
    else
        echo "  $v: already exists"
    fi
done

# Download PacketEvents
echo ""
echo "=== Downloading PacketEvents ==="
PLUGINS_DIR="../plugins"
mkdir -p "$PLUGINS_DIR"
if [ ! -f "$PLUGINS_DIR/packetevents.jar" ]; then
    # Get latest from Modrinth
    PE_URL=$(curl -s "https://api.modrinth.com/v2/project/packetevents/version?loaders=%5B%22spigot%22%5D&limit=1" | python3 -c "import json,sys; v=json.load(sys.stdin)[0]; f=[x for x in v['files'] if x['primary']][0]; print(f['url'])" 2>/dev/null)
    if [ -n "$PE_URL" ]; then
        curl -sL "$PE_URL" -o "$PLUGINS_DIR/packetevents.jar"
        echo "  PacketEvents downloaded"
    else
        echo "  WARN: Could not download PacketEvents"
    fi
else
    echo "  PacketEvents: already exists"
fi

echo ""
echo "=== Server JARs ==="
find . -name "server.jar" | while read f; do
    size=$(stat -c%s "$f" 2>/dev/null || echo "?")
    echo "  $f ($size bytes)"
done
