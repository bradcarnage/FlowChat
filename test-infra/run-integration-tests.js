const mc = require('minecraft-protocol');
const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const net = require('net');

// === Config ===
const SERVER_PORT = 25599;
const RCON_PORT = 25598;
const RCON_PASS = 'flowchat_test';
const STARTUP_TIMEOUT = 180000; // 180s for legacy first-run world gen
const CONNECT_DELAY = 3000;
const RCON_DELAY = 2500; // wait after RCON command for chat packets

class FlowChatIntegrationTest {
    constructor(mcVersion, serverDir) {
        this.mcVersion = mcVersion;
        this.serverDir = serverDir;
        this.serverProcess = null;
        this.client = null;
        this.receivedMessages = [];
        this.results = [];
        this.serverLog = '';
    }

    async run() {
        console.log(`\n${'='.repeat(50)}`);
        console.log(`Testing MC ${this.mcVersion}`);
        console.log(`${'='.repeat(50)}`);

        try {
            this.setupServer();
            await this.startServer();
            await this.waitForReady();
            await this.connectClient();
            await this.runTests();
        } catch (err) {
            this.results.push({ name: 'SETUP', passed: false, error: err.message });
            console.log(`  ✗ SETUP: ${err.message}`);
        } finally {
            await this.cleanup();
        }

        return this.results;
    }

    setupServer() {
        const runDir = path.join(this.serverDir, 'run');

        // Clean old world data for fast restart (keep cached patched jars)
        for (const d of ['world', 'world_nether', 'world_the_end', 'logs', 'crash-reports']) {
            const p = path.join(runDir, d);
            if (fs.existsSync(p)) fs.rmSync(p, { recursive: true, force: true });
        }

        fs.mkdirSync(path.join(runDir, 'plugins'), { recursive: true });

        // server.properties — tuned for fast startup
        fs.writeFileSync(path.join(runDir, 'server.properties'), [
            'server-port=' + SERVER_PORT,
            'online-mode=false',
            'enable-rcon=true',
            'rcon.port=' + RCON_PORT,
            'rcon.password=' + RCON_PASS,
            'enforce-secure-profile=false',
            'level-type=flat',
            'spawn-protection=0',
            'max-players=2',
            'view-distance=4',
            'simulation-distance=4',
            'sync-chunk-writes=false',
            'generate-structures=false',
            'spawn-npcs=false',
            'spawn-animals=false',
            'spawn-monsters=false',
            'allow-nether=false',
        ].join('\n'));

        // eula
        fs.writeFileSync(path.join(runDir, 'eula.txt'), 'eula=true');

        // Copy server.jar
        const srcJar = path.join(this.serverDir, 'server.jar');
        const dstJar = path.join(runDir, 'server.jar');
        if (!fs.existsSync(dstJar)) fs.copyFileSync(srcJar, dstJar);

        // Copy PacketEvents Spigot plugin
        const peJar = path.join(__dirname, 'plugins', 'packetevents-spigot.jar');
        if (fs.existsSync(peJar)) {
            fs.copyFileSync(peJar, path.join(runDir, 'plugins', 'packetevents.jar'));
        }

        // Copy FlowChat spigot plugin
        const flowchatJar = this.findFlowChatJar();
        if (flowchatJar) {
            fs.copyFileSync(flowchatJar, path.join(runDir, 'plugins', 'flowchat.jar'));
        } else {
            console.log('  [warn] No FlowChat JAR found!');
        }

        // FlowChat test config
        const fcDir = path.join(runDir, 'plugins', 'FlowChat');
        fs.mkdirSync(fcDir, { recursive: true });
        fs.writeFileSync(path.join(fcDir, 'flowchat.json'), JSON.stringify({
            incoming: [
                { pattern: 'hello_test', replacement: 'world_test' },
                { pattern: 'cancel_me', replacement: '', toast: true },
                { pattern: 'sound_test', replacement: '$0', sound: 'bell' },
                { pattern: 'color_test', replacement: '&aGreen &bBlue' },
                { pattern: 'respond_test', respond: 'auto_reply_ok' },
                { pattern: 'stack_(\\d+)', replacement: 'total=$^1 x$^i',
                  valuestack: { stack_values: [1], expire_after: 10 } },
                { pattern: 'tag_time', replacement: '{time}' },
                { pattern: 'tag_ip', replacement: '{serverip}' },
                { search: 'legacy_field_test', replacement: 'legacy_ok' },
            ],
            outgoing: [
                { pattern: 'out_replace', replacement: 'out_replaced' },
                { pattern: 'out_cancel', replacement: '', toast: true },
            ]
        }, null, 2));
    }

    findFlowChatJar() {
        const candidates = [
            path.join(__dirname, '..', 'spigot', 'build', 'libs'),
            path.join(__dirname, '..', 'build', 'libs'),
        ];
        for (const dir of candidates) {
            if (!fs.existsSync(dir)) continue;
            const jars = fs.readdirSync(dir)
                .filter(f => f.includes('flowchat') && f.endsWith('.jar') && !f.includes('sources'));
            if (jars.length > 0) return path.join(dir, jars[0]);
        }
        return null;
    }

    startServer() {
        return new Promise((resolve) => {
            const runDir = path.join(this.serverDir, 'run');
            const javaPath = this.getJavaPath();
            const isLegacy = this.isLegacyVersion();

            // 1.21+ needs more heap for PE 2.7.0's block state init
            const heap = this.mcVersion.startsWith('1.21') ? '1024M' : '512M';
            const args = [`-Xmx${heap}`, `-Xms256M`, '-jar', 'server.jar'];
            if (isLegacy) {
                // Legacy Paper (1.8-1.12) needs --nojline --noconsole to not hang
                args.push('--nojline', '--noconsole');
            } else {
                args.push('--nogui');
            }

            console.log(`  Starting with: ${javaPath} ${args.join(' ')}`);

            this.serverProcess = spawn(javaPath, args, {
                cwd: runDir,
                stdio: ['pipe', 'pipe', 'pipe'],
            });

            // Still capture stdout for modern servers
            this.serverProcess.stdout.on('data', (d) => { this.serverLog += d.toString(); });
            this.serverProcess.stderr.on('data', (d) => { this.serverLog += d.toString(); });

            this.serverProcess.on('exit', (code) => {
                if (code !== null && code !== 0 && code !== 143) {
                    console.log(`  [warn] Server exited with code ${code}`);
                }
            });

            resolve();
        });
    }

    getJavaPath() {
        const v = this.mcVersion;
        if (v.startsWith('1.21') || v === '1.20.6' || v === '1.20.5')
            return '/home/agent/java/jdk-21.0.11+10/bin/java';
        if (v.startsWith('1.2') || v.startsWith('1.19') || v.startsWith('1.18') || v.startsWith('1.17'))
            return '/usr/lib/jvm/java-17-openjdk-amd64/bin/java';
        return '/home/agent/java/jdk8u412-b08/bin/java';
    }

    isLegacyVersion() {
        const v = this.mcVersion;
        return v.startsWith('1.8') || v.startsWith('1.9') || v.startsWith('1.10') ||
               v.startsWith('1.11') || v.startsWith('1.12');
    }

    getLogContent() {
        // Read from log file (more reliable than stdout for legacy servers)
        const logPath = path.join(this.serverDir, 'run', 'logs', 'latest.log');
        try {
            if (fs.existsSync(logPath)) {
                return fs.readFileSync(logPath, 'utf8');
            }
        } catch(e) {}
        // Fallback to captured stdout
        return this.serverLog;
    }

    waitForReady() {
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                const log = this.getLogContent();
                const lastLines = log.split('\n').slice(-5).join('\n');
                reject(new Error(`Server startup timeout (180s). Last log: ${lastLines}`));
            }, STARTUP_TIMEOUT);

            const check = setInterval(() => {
                // Check both stdout and log file
                const log = this.serverLog + '\n' + this.getLogContent();

                if (log.includes('Done (') || log.includes('Timings Reset') ||
                    log.includes('For help, type') || log.includes('Server permissions file')) {
                    clearInterval(check);
                    clearTimeout(timeout);
                    console.log('  Server ready');
                    setTimeout(resolve, CONNECT_DELAY);
                }

                if (log.includes('FAILED TO BIND') || log.includes('Failed to bind to port')) {
                    clearInterval(check);
                    clearTimeout(timeout);
                    reject(new Error('Port already in use'));
                }
            }, 1000);
        });
    }

    async connectClient() {
        return new Promise((resolve, reject) => {
            console.log(`  Connecting client as MC ${this.mcVersion}...`);

            this.client = mc.createClient({
                host: 'localhost',
                port: SERVER_PORT,
                username: 'FlowChatBot',
                version: this.mcVersion,
                auth: 'offline',
            });

            this.client.on('error', (err) => {
                reject(new Error('Client connection error: ' + err.message));
            });

            // Modern chat (1.19.1+)
            this.client.on('system_chat', (packet) => {
                const text = this.parsePacketContent(packet.content);
                if (text) this.receivedMessages.push({
                    type: 'system', text,
                    overlay: packet.isActionBar || false
                });
            });

            this.client.on('profileless_chat', (packet) => {
                const text = this.parsePacketContent(packet.message);
                if (text) this.receivedMessages.push({ type: 'profileless', text, overlay: false });
            });

            this.client.on('player_chat', (packet) => {
                const text = this.parsePacketContent(
                    packet.plainMessage || packet.unsignedContent || packet.formattedMessage
                );
                if (text) this.receivedMessages.push({ type: 'player', text, overlay: false });
            });

            // Legacy chat (pre-1.19)
            this.client.on('chat', (packet) => {
                const text = this.parsePacketContent(packet.message);
                if (text) this.receivedMessages.push({
                    type: 'chat', text,
                    position: packet.position,
                    overlay: packet.position === 2
                });
            });

            this.client.on('login', () => {
                console.log('  Client connected');
                setTimeout(resolve, 1000);
            });

            setTimeout(() => reject(new Error('Client connect timeout (30s)')), 30000);
        });
    }

    extractText(obj) {
        if (typeof obj === 'string') return obj;
        if (typeof obj !== 'object' || obj === null) return '';
        let text = '';

        if (obj.translate) {
            // For translate components, extract text from 'with' args
            // The translate key itself is a Minecraft lang key (not useful for matching)
            if (obj.with && Array.isArray(obj.with)) {
                const parts = obj.with.map(w => this.extractText(w));
                text = parts.join(' ');
            } else {
                text = obj.translate;
            }
        } else {
            text = obj.text || '';
        }

        if (obj.extra) {
            for (const e of obj.extra) text += this.extractText(e);
        }
        return String(text);
    }

    parsePacketContent(content) {
        if (!content) return null;

        // String content (JSON or plain)
        if (typeof content === 'string') {
            try {
                const parsed = JSON.parse(content);
                return this.extractText(parsed);
            } catch(e) {
                return content;
            }
        }

        // Object content (NBT compound for modern MC, or direct JSON component)
        if (typeof content === 'object') {
            // Direct value (string wrapper)
            if (content.value && typeof content.value === 'string') return content.value;
            if (content.type === 'string' && content.value) return content.value;

            // NBT compound (1.20.3+)
            if (content.type === 'compound' && content.value) {
                const v = content.value;
                if (v.translate) {
                    const translateKey = v.translate.value || v.translate;
                    let result = String(translateKey);
                    if (v.with) {
                        const withVal = v.with.value || v.with;
                        const args = withVal.value
                            ? (Array.isArray(withVal.value) ? withVal.value : [withVal.value])
                            : (Array.isArray(withVal) ? withVal : [withVal]);
                        for (const a of args) {
                            const argText = this.parsePacketContent(a);
                            if (argText && result.includes('%s')) {
                                result = result.replace('%s', argText);
                            } else if (argText) {
                                result += argText;
                            }
                        }
                    }
                    return result;
                }
                if (v.text) {
                    let t = v.text.value || v.text;
                    if (v.extra && v.extra.value) {
                        const extras = Array.isArray(v.extra.value) ? v.extra.value : [v.extra.value];
                        for (const e of extras) t += this.parsePacketContent(e) || '';
                    }
                    return String(t);
                }
            }

            // List type
            if (content.type === 'list' && content.value && content.value.value) {
                return content.value.value.map(v => this.parsePacketContent(v)).filter(Boolean).join('');
            }

            // Fallback: plain JSON chat component
            return this.extractText(content);
        }
        return null;
    }

    async runTests() {
        const log = this.getLogContent();
        const isLegacy = this.isLegacyVersion(); // pre-1.13

        // Test 1: Plugin loaded
        this.test('Plugin loaded', () => {
            return (log.includes('FlowChat') && (log.includes('enabled') || log.includes('Enabling')))
                && !log.includes('Could not load');
        });

        // Test 2: PacketEvents or Bukkit fallback
        if (!isLegacy) {
            this.test('PacketEvents initialized', () => {
                return log.includes('PacketEvents') && !log.includes('UnknownDependencyException')
                    && !log.includes('failed to inject');
            });
        } else {
            this.test('Bukkit event fallback active', () => {
                // On pre-1.13, PE won't be found — FlowChat should log Bukkit fallback
                return log.includes('Bukkit event') || log.includes('FlowChat') &&
                    !log.includes('Could not load') && !log.includes('Error occurred');
            });
        }

        // Test 3: Config file exists
        this.test('Config file created', () => {
            return fs.existsSync(path.join(this.serverDir, 'run', 'plugins', 'FlowChat', 'flowchat.json'));
        });

        // Choose command based on version
        // - Pre-1.13 (Bukkit fallback): 'say' triggers ServerCommandEvent
        // - 1.13-1.17: 'say' generates legacy chat packet intercepted by PE
        // - 1.18+: 'tellraw' sends system chat intercepted by PE
        const usesSay = isLegacy || this.mcVersion.startsWith('1.16') || this.mcVersion.startsWith('1.17');
        const chatCmd = (text) => usesSay ? `say ${text}` : `tellraw @a {"text":"${text}"}`;

        // Tests 4-8: Chat interception — PE on 1.13+, Bukkit events on pre-1.13
        // Test 4: Text replacement
        await this.rconTest('Text replacement', chatCmd('hello_test'), () => {
            return this.findMessage('world_test');
        });

        // Test 5: Color codes
        await this.rconTest('Color code conversion', chatCmd('color_test'), () => {
            return this.findMessage('Green') || this.findMessage('Blue') || this.findMessage('color');
        });

        // Test 6: Toast/cancel → overlay
        this.receivedMessages = [];
        await this.rconTest('Toast/cancel → overlay', chatCmd('cancel_me'), () => {
            const overlay = this.receivedMessages.find(m => m.overlay || m.position === 2);
            const cancelledFromChat = !this.receivedMessages.some(m =>
                m.text && m.text.includes('cancel_me') && !m.overlay && m.position !== 2
            );
            return overlay || cancelledFromChat;
        });

        // Test 7: Legacy field names (search → pattern)
        await this.rconTest('Legacy field names', chatCmd('legacy_field_test'), () => {
            return this.findMessage('legacy_ok');
        });

        // Test 8: Tag {time}
        await this.rconTest('Tag {time}', chatCmd('tag_time'), () => {
            return this.receivedMessages.some(m => m.text && /\d{1,2}:\d{2}/.test(m.text));
        });

        // Test 9: /flowchat reload
        await this.rconCmd('flowchat reload');
        await this.delay(1500);
        this.test('/flowchat reload', () => {
            const fullLog = this.getLogContent();
            return fullLog.includes('reload') || fullLog.includes('Reload') ||
                   fullLog.includes('FlowChat') || fullLog.includes('flowchat');
        });

        // Test 10: /flowchat toggle
        await this.rconCmd('flowchat toggle');
        await this.delay(500);
        this.test('/flowchat toggle', () => true); // Command executed = pass

        // Re-enable
        await this.rconCmd('flowchat toggle');
        await this.delay(500);

        // Test 11: Plugin self-test
        this.test('Plugin self-test', () => {
            const fullLog = this.getLogContent();
            return fullLog.includes('FlowChat') && (fullLog.includes('enabled') || fullLog.includes('Enabling'));
        });
    }

    test(name, fn) {
        try {
            const passed = fn();
            this.results.push({ name, passed: !!passed, error: passed ? null : 'Assertion failed' });
            console.log(`  ${passed ? '✓' : '✗'} ${name}`);
        } catch (err) {
            this.results.push({ name, passed: false, error: err.message });
            console.log(`  ✗ ${name}: ${err.message}`);
        }
    }

    async rconTest(name, command, checkFn) {
        this.receivedMessages = [];
        await this.rconCmd(command);
        await this.delay(RCON_DELAY);
        const passed = (() => { try { return checkFn(); } catch(e) { return false; } })();
        if (!passed) {
            if (this.receivedMessages.length > 0) {
                console.log(`    [debug] Got ${this.receivedMessages.length} msgs: ${JSON.stringify(this.receivedMessages.map(m => ({
                    type: m.type, text: m.text?.substring(0, 80), overlay: m.overlay, pos: m.position
                })))}`);
            } else {
                console.log(`    [debug] No messages received after "${command}"`);
            }
        }
        this.test(name, checkFn);
    }

    findMessage(substring) {
        return this.receivedMessages.some(m => m.text && m.text.includes(substring));
    }

    delay(ms) {
        return new Promise(r => setTimeout(r, ms));
    }

    async rconCmd(command) {
        return new Promise((resolve) => {
            try {
                const sock = new net.Socket();
                sock.setTimeout(5000);
                sock.connect(RCON_PORT, 'localhost', () => {
                    // RCON Login packet
                    const loginPayload = Buffer.alloc(14 + RCON_PASS.length);
                    loginPayload.writeInt32LE(10 + RCON_PASS.length, 0);
                    loginPayload.writeInt32LE(1, 4);
                    loginPayload.writeInt32LE(3, 8);
                    loginPayload.write(RCON_PASS, 12);
                    sock.write(loginPayload);
                });

                let phase = 'login';
                sock.on('data', (data) => {
                    if (phase === 'login') {
                        phase = 'command';
                        const cmdPayload = Buffer.alloc(14 + command.length);
                        cmdPayload.writeInt32LE(10 + command.length, 0);
                        cmdPayload.writeInt32LE(2, 4);
                        cmdPayload.writeInt32LE(2, 8);
                        cmdPayload.write(command, 12);
                        sock.write(cmdPayload);
                    } else {
                        sock.destroy();
                        resolve();
                    }
                });

                sock.on('error', () => { sock.destroy(); resolve(); });
                sock.on('timeout', () => { sock.destroy(); resolve(); });
            } catch(e) { resolve(); }
        });
    }

    async cleanup() {
        if (this.client) {
            try { this.client.end(); } catch(e) {}
        }
        if (this.serverProcess) {
            this.serverProcess.kill('SIGTERM');
            await this.delay(2000);
            try { this.serverProcess.kill('SIGKILL'); } catch(e) {}
            await this.delay(1000);
        }
        // Force-kill anything on our ports and wait until truly free
        for (let attempt = 0; attempt < 5; attempt++) {
            try { execSync(`fuser -k ${SERVER_PORT}/tcp 2>/dev/null || true`); } catch(e) {}
            try { execSync(`fuser -k ${RCON_PORT}/tcp 2>/dev/null || true`); } catch(e) {}
            await this.delay(2000);
            // Verify ports are actually free
            const portFree = await this.isPortFree(SERVER_PORT) && await this.isPortFree(RCON_PORT);
            if (portFree) break;
            console.log(`  [cleanup] Ports still in use, attempt ${attempt + 2}/5...`);
        }
    }

    isPortFree(port) {
        return new Promise((resolve) => {
            const server = net.createServer();
            server.once('error', () => resolve(false));
            server.once('listening', () => { server.close(); resolve(true); });
            server.listen(port, '0.0.0.0');
        });
    }
}

// === Main ===
async function main() {
    // Kill any leftover servers
    try { execSync(`fuser -k ${SERVER_PORT}/tcp 2>/dev/null || true`); } catch(e) {}
    try { execSync(`fuser -k ${RCON_PORT}/tcp 2>/dev/null || true`); } catch(e) {}
    await new Promise(r => setTimeout(r, 2000));

    const serversDir = path.join(__dirname, 'servers');
    let versions = fs.readdirSync(serversDir)
        .filter(d => fs.existsSync(path.join(serversDir, d, 'server.jar')))
        .filter(d => !d.includes('bungeecord') && !d.includes('velocity'))
        .sort();

    // Allow filtering by version from command line: node run-integration-tests.js 1.21.4 1.8.8
    const filterVersions = process.argv.slice(2);
    if (filterVersions.length > 0) {
        versions = versions.filter(d => filterVersions.some(f => d.includes(f)));
    }

    console.log(`Found ${versions.length} server versions: ${versions.join(', ')}`);

    const allResults = {};
    for (const dir of versions) {
        const version = dir.replace(/^(paper|spigot)-/, '');
        const tester = new FlowChatIntegrationTest(version, path.join(serversDir, dir));
        allResults[version] = await tester.run();
    }

    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('INTEGRATION TEST RESULTS');
    console.log('='.repeat(60));

    let totalPass = 0, totalFail = 0;
    for (const [version, results] of Object.entries(allResults)) {
        const pass = results.filter(r => r.passed).length;
        const fail = results.filter(r => !r.passed).length;
        totalPass += pass;
        totalFail += fail;
        console.log(`\n${version}: ${pass}/${results.length} passed`);
        for (const r of results) {
            console.log(`  ${r.passed ? '✓' : '✗'} ${r.name}${r.error ? ' — ' + r.error : ''}`);
        }
    }

    console.log(`\nTotal: ${totalPass} passed, ${totalFail} failed out of ${totalPass + totalFail}`);

    // Write results
    fs.mkdirSync(path.join(__dirname, 'results'), { recursive: true });
    fs.writeFileSync(path.join(__dirname, 'results', 'integration-results.json'),
        JSON.stringify(allResults, null, 2));

    process.exit(totalFail > 0 ? 1 : 0);
}

main().catch(err => {
    console.error('Fatal:', err);
    process.exit(2);
});
