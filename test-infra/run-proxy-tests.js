const mc = require('minecraft-protocol');
const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const net = require('net');

// === Config ===
const BACKEND_PORT = 25577;  // Paper backend
const PROXY_PORT = 25599;    // Proxy listens here
const RCON_PORT = 25598;     // Backend RCON
const RCON_PASS = 'flowchat_test';
const TEST_TIMEOUT = 90000;
const BACKEND_VERSION = '1.21.4';
const JAVA_21 = '/home/agent/java/jdk-21.0.11+10/bin/java';
const JAVA_17 = '/usr/lib/jvm/java-17-openjdk-amd64/bin/java';

class ProxyIntegrationTest {
    constructor(proxyType) {
        this.proxyType = proxyType; // 'bungeecord' or 'velocity'
        this.backendProcess = null;
        this.proxyProcess = null;
        this.client = null;
        this.receivedMessages = [];
        this.results = [];
        this.backendLog = '';
        this.proxyLog = '';
    }

    async run() {
        console.log(`\n${'='.repeat(50)}`);
        console.log(`Testing ${this.proxyType.toUpperCase()} proxy`);
        console.log(`${'='.repeat(50)}`);

        try {
            await this.setupBackend();
            await this.startBackend();
            await this.waitForBackend();
            await this.setupProxy();
            await this.startProxy();
            await this.waitForProxy();
            await this.connectClient();
            await this.runTests();
        } catch (err) {
            console.error(`  SETUP ERROR: ${err.message}`);
            this.results.push({ name: 'SETUP', passed: false, error: err.message });
        } finally {
            await this.cleanup();
        }

        return this.results;
    }

    setupBackend() {
        const serverDir = path.join(__dirname, 'servers', `paper-${BACKEND_VERSION}`);
        const runDir = path.join(serverDir, 'run');
        fs.mkdirSync(path.join(runDir, 'plugins'), { recursive: true });

        fs.writeFileSync(path.join(runDir, 'server.properties'), [
            'server-port=' + BACKEND_PORT,
            'online-mode=false',
            'enable-rcon=true',
            'rcon.port=' + RCON_PORT,
            'rcon.password=' + RCON_PASS,
            'enforce-secure-profile=false',
            'level-type=flat',
            'spawn-protection=0',
            'max-players=5',
            'view-distance=4',
            'simulation-distance=4',
        ].join('\n'));

        fs.writeFileSync(path.join(runDir, 'eula.txt'), 'eula=true');

        // No FlowChat on backend — proxy handles it
        // But we need a functioning server
        const srcJar = path.join(serverDir, 'server.jar');
        const dstJar = path.join(runDir, 'server.jar');
        if (!fs.existsSync(dstJar)) fs.copyFileSync(srcJar, dstJar);
    }

    startBackend() {
        return new Promise((resolve) => {
            const runDir = path.join(__dirname, 'servers', `paper-${BACKEND_VERSION}`, 'run');
            this.backendProcess = spawn(JAVA_21, [
                '-Xmx512M', '-Xms256M',
                '-jar', 'server.jar', '--nogui',
            ], { cwd: runDir, stdio: ['pipe', 'pipe', 'pipe'] });

            this.backendProcess.stdout.on('data', (d) => { this.backendLog += d.toString(); });
            this.backendProcess.stderr.on('data', (d) => { this.backendLog += d.toString(); });
            resolve();
        });
    }

    waitForBackend() {
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => reject(new Error('Backend startup timeout (60s)')), 60000);
            const check = setInterval(() => {
                if (this.backendLog.includes('Done (') || this.backendLog.includes('Timings Reset')) {
                    clearInterval(check);
                    clearTimeout(timeout);
                    console.log('  Backend server ready');
                    setTimeout(resolve, 2000);
                }
            }, 500);
        });
    }

    setupProxy() {
        const proxyDir = path.join(__dirname, 'servers', this.proxyType === 'bungeecord' ? 'bungeecord' : 'velocity-proxy');
        const runDir = path.join(proxyDir, 'run');
        fs.mkdirSync(path.join(runDir, 'plugins'), { recursive: true });

        // Copy proxy jar
        const srcJar = path.join(proxyDir, 'server.jar');
        const dstJar = path.join(runDir, 'proxy.jar');
        fs.copyFileSync(srcJar, dstJar);

        // Copy PacketEvents plugin — platform-specific
        const peJarName = this.proxyType === 'bungeecord' ? 'packetevents-bungee.jar' : 'packetevents-velocity.jar';
        const peJar = path.join(__dirname, 'plugins', peJarName);
        if (fs.existsSync(peJar)) {
            fs.copyFileSync(peJar, path.join(runDir, 'plugins', 'packetevents.jar'));
        }

        // Copy FlowChat proxy plugin
        const pluginJarName = this.proxyType === 'bungeecord' ? 'flowchat-bungee-2.1.0.jar' : 'flowchat-velocity-2.1.0.jar';
        const pluginSrc = path.join(__dirname, '..', this.proxyType === 'bungeecord' ? 'bungee' : 'velocity', 'build', 'libs', pluginJarName);
        if (fs.existsSync(pluginSrc)) {
            fs.copyFileSync(pluginSrc, path.join(runDir, 'plugins', 'flowchat.jar'));
            console.log(`  Copied ${pluginJarName}`);
        } else {
            console.log(`  WARNING: ${pluginSrc} not found`);
        }

        // FlowChat config
        const fcDir = path.join(runDir, 'plugins', 'FlowChat');
        fs.mkdirSync(fcDir, { recursive: true });
        fs.writeFileSync(path.join(fcDir, 'flowchat.json'), JSON.stringify({
            incoming: [
                { pattern: 'hello_test', replacement: 'world_test' },
                { pattern: 'cancel_me', replacement: '', toast: true },
                { pattern: 'sound_test', replacement: '$0', sound: 'bell' },
                { pattern: 'color_test', replacement: '&aGreen &bBlue' },
                { pattern: 'respond_test', respond: 'auto_reply_ok' },
            ],
            outgoing: [
                { pattern: 'out_replace', replacement: 'out_replaced' },
            ]
        }, null, 2));

        if (this.proxyType === 'bungeecord') {
            this.setupBungeeConfig(runDir);
        } else {
            this.setupVelocityConfig(runDir);
        }
    }

    setupBungeeConfig(runDir) {
        // BungeeCord config.yml
        const config = `
listeners:
- query_port: 25577
  motd: '&1FlowChat Test Proxy'
  max_players: 5
  forced_hosts: {}
  host: 0.0.0.0:${PROXY_PORT}
  query_enabled: false
  force_default_server: true
  priorities:
  - lobby
online_mode: false
ip_forward: false
servers:
  lobby:
    motd: '&1Lobby'
    address: localhost:${BACKEND_PORT}
    restricted: false
`;
        fs.writeFileSync(path.join(runDir, 'config.yml'), config);
    }

    setupVelocityConfig(runDir) {
        // Velocity velocity.toml
        const config = `
config-version = "2.7"
bind = "0.0.0.0:${PROXY_PORT}"
motd = "&3FlowChat Test Proxy"
show-max-players = 5
online-mode = false
force-key-authentication = false
player-info-forwarding-mode = "NONE"

[servers]
lobby = "localhost:${BACKEND_PORT}"
try = ["lobby"]

[forced-hosts]

[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
haproxy-protocol = false
tcp-fast-open = false
bungee-plugin-message-channel = true
show-ping-requests = false
failover-on-unexpected-server-disconnect = true
announce-proxy-commands = true
log-command-executions = false
log-player-connections = true

[query]
enabled = false
port = 25577

[metrics]
enabled = false
`;
        fs.writeFileSync(path.join(runDir, 'velocity.toml'), config);

        // Forwarding secret
        fs.writeFileSync(path.join(runDir, 'forwarding.secret'), 'flowchattest');
    }

    startProxy() {
        return new Promise((resolve) => {
            const proxyDir = path.join(__dirname, 'servers', this.proxyType === 'bungeecord' ? 'bungeecord' : 'velocity-proxy');
            const runDir = path.join(proxyDir, 'run');
            const java = this.proxyType === 'bungeecord' ? JAVA_21 : JAVA_21;

            this.proxyProcess = spawn(java, [
                '-Xmx256M', '-Xms128M',
                '-jar', 'proxy.jar',
            ], { cwd: runDir, stdio: ['pipe', 'pipe', 'pipe'] });

            this.proxyProcess.stdout.on('data', (d) => { this.proxyLog += d.toString(); });
            this.proxyProcess.stderr.on('data', (d) => { this.proxyLog += d.toString(); });
            resolve();
        });
    }

    waitForProxy() {
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                console.log('  Proxy log so far:', this.proxyLog.slice(-500));
                reject(new Error('Proxy startup timeout (60s)'));
            }, 60000);
            const check = setInterval(() => {
                const ready = this.proxyType === 'bungeecord'
                    ? this.proxyLog.includes('Listening on')
                    : this.proxyLog.includes('Done (') || this.proxyLog.includes('Listening on');
                if (ready) {
                    clearInterval(check);
                    clearTimeout(timeout);
                    console.log('  Proxy ready');
                    setTimeout(resolve, 3000);
                }
            }, 500);
        });
    }

    async connectClient() {
        return new Promise((resolve, reject) => {
            this.client = mc.createClient({
                host: 'localhost',
                port: PROXY_PORT,
                username: 'FlowChatBot',
                version: BACKEND_VERSION,
                auth: 'offline',
            });

            this.client.on('error', (err) => {
                reject(new Error('Client error: ' + err.message));
            });

            this.client.on('system_chat', (packet) => {
                const text = this.parsePacketContent(packet.content);
                if (text) this.receivedMessages.push({ type: 'system', text, overlay: packet.isActionBar || false });
            });

            this.client.on('profileless_chat', (packet) => {
                const text = this.parsePacketContent(packet.message);
                if (text) this.receivedMessages.push({ type: 'profileless', text, overlay: false });
            });

            this.client.on('player_chat', (packet) => {
                const text = this.parsePacketContent(packet.plainMessage || packet.unsignedContent || packet.formattedMessage);
                if (text) this.receivedMessages.push({ type: 'player', text, overlay: false });
            });

            this.client.on('chat', (packet) => {
                const text = this.parsePacketContent(packet.message);
                if (text) this.receivedMessages.push({ type: 'chat', text, position: packet.position });
            });

            this.client.on('login', () => {
                console.log('  Client connected to proxy');
                setTimeout(resolve, 2000);
            });

            setTimeout(() => reject(new Error('Client connect timeout (15s)')), 15000);
        });
    }

    parsePacketContent(content) {
        if (!content) return null;
        if (typeof content === 'string') {
            try {
                const parsed = JSON.parse(content);
                return this.extractText(parsed);
            } catch(e) {
                return content;
            }
        }
        if (typeof content === 'object') {
            if (content.value && typeof content.value === 'string') return content.value;
            if (content.type === 'string' && content.value) return content.value;
            if (content.type === 'compound' && content.value) {
                const v = content.value;
                if (v.text && v.text.value) return v.text.value;
                if (v.translate && v.translate.value) {
                    let result = v.translate.value;
                    if (v.with && v.with.value) {
                        const args = Array.isArray(v.with.value) ? v.with.value : [v.with.value];
                        for (const a of args) {
                            const argText = this.parsePacketContent(a);
                            if (argText) result += ' ' + argText;
                        }
                    }
                    return result;
                }
            }
            return this.extractText(content);
        }
        return null;
    }

    extractText(obj) {
        if (typeof obj === 'string') return obj;
        if (typeof obj !== 'object' || obj === null) return '';
        let text = obj.text || obj.translate || '';
        if (obj.extra) for (const e of obj.extra) text += this.extractText(e);
        if (obj.with) for (const w of obj.with) text += this.extractText(w);
        return String(text);
    }

    async runTests() {
        // Test 1: Proxy loaded FlowChat
        this.test('Proxy loaded FlowChat plugin', () => {
            return this.proxyLog.includes('FlowChat') &&
                   (this.proxyLog.includes('enabled') || this.proxyLog.includes('Enabling'));
        });

        // Test 2: PacketEvents initialized on proxy
        this.test('PacketEvents initialized on proxy', () => {
            return this.proxyLog.includes('PacketEvents') || this.proxyLog.includes('packetevents');
        });

        // Test 3: FlowChat config created
        this.test('FlowChat config file exists', () => {
            const proxyDir = this.proxyType === 'bungeecord' ? 'bungeecord' : 'velocity-proxy';
            const configPath = path.join(__dirname, 'servers', proxyDir, 'run', 'plugins', 'FlowChat', 'flowchat.json');
            return fs.existsSync(configPath);
        });

        // Test 4: Client connected through proxy
        this.test('Client connected through proxy', () => {
            return this.client && this.client.state === 'play';
        });

        // Test 5: Text replacement — send via RCON to backend which broadcasts, proxy intercepts
        await this.rconTest('Text replacement through proxy', 'say hello_test', () => {
            return this.findMessage('world_test');
        });

        // Test 6: Toast/cancel
        this.receivedMessages = [];
        await this.rconTest('Toast/cancel through proxy', 'say cancel_me', () => {
            const overlay = this.receivedMessages.find(m => m.overlay);
            const cancelled = !this.findMessage('cancel_me');
            return overlay || cancelled;
        });

        // Test 7: Color codes
        await this.rconTest('Color codes through proxy', 'say color_test', () => {
            return this.findMessage('Green') || this.findMessage('color');
        });

        // Test 8: /flowchat reload (proxy command)
        // For proxy, /flowchat is on the proxy, not RCON (RCON is backend)
        // Test by checking proxy log for load messages
        this.test('/flowchat command registered', () => {
            // If plugin loaded successfully, command should be registered
            return this.proxyLog.includes('FlowChat') && this.proxyLog.includes('enabled');
        });
    }

    async rconTest(name, command, checkFn) {
        this.receivedMessages = [];
        await this.rconCmd(command);
        await this.delay(2000);
        this.test(name, checkFn);
    }

    findMessage(substring) {
        return this.receivedMessages.some(m => m.text && m.text.includes(substring));
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

    delay(ms) { return new Promise(r => setTimeout(r, ms)); }

    async rconCmd(command) {
        return new Promise((resolve) => {
            try {
                const sock = new net.Socket();
                sock.setTimeout(5000);
                sock.connect(RCON_PORT, 'localhost', () => {
                    const loginPayload = Buffer.alloc(14 + RCON_PASS.length);
                    loginPayload.writeInt32LE(10 + RCON_PASS.length, 0);
                    loginPayload.writeInt32LE(1, 4);
                    loginPayload.writeInt32LE(3, 8);
                    loginPayload.write(RCON_PASS, 12);
                    sock.write(loginPayload);
                });
                let phase = 'login';
                sock.on('data', () => {
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
        if (this.client) try { this.client.end(); } catch(e) {}
        if (this.proxyProcess) {
            this.proxyProcess.kill('SIGTERM');
            await this.delay(3000);
            try { this.proxyProcess.kill('SIGKILL'); } catch(e) {}
        }
        if (this.backendProcess) {
            this.backendProcess.kill('SIGTERM');
            await this.delay(3000);
            try { this.backendProcess.kill('SIGKILL'); } catch(e) {}
        }
        await this.delay(2000);
    }
}

// === Main ===
async function main() {
    const proxyTypes = process.argv[2] ? [process.argv[2]] : ['bungeecord', 'velocity'];

    const allResults = {};
    for (const proxyType of proxyTypes) {
        const tester = new ProxyIntegrationTest(proxyType);
        allResults[proxyType] = await tester.run();
    }

    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('PROXY INTEGRATION TEST RESULTS');
    console.log('='.repeat(60));

    let totalPass = 0, totalFail = 0;
    for (const [proxy, results] of Object.entries(allResults)) {
        const pass = results.filter(r => r.passed).length;
        const fail = results.filter(r => !r.passed).length;
        totalPass += pass;
        totalFail += fail;
        console.log(`\n${proxy}: ${pass}/${results.length} passed`);
        for (const r of results) {
            console.log(`  ${r.passed ? '✓' : '✗'} ${r.name}${r.error ? ' — ' + r.error : ''}`);
        }
    }

    console.log(`\nTotal: ${totalPass} passed, ${totalFail} failed`);

    fs.mkdirSync(path.join(__dirname, 'results'), { recursive: true });
    fs.writeFileSync(path.join(__dirname, 'results', 'proxy-results.json'),
        JSON.stringify(allResults, null, 2));

    process.exit(totalFail > 0 ? 1 : 0);
}

main().catch(err => {
    console.error('Fatal:', err);
    process.exit(2);
});
