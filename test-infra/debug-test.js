// Quick debug — connect to a 1.21.4 server and log all received packets
const mc = require('minecraft-protocol');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const net = require('net');

const SERVER_PORT = 25599;
const RCON_PORT = 25598;
const RCON_PASS = 'flowchat_test';

async function debug() {
    const serverDir = path.join(__dirname, 'servers', 'paper-1.21.4', 'run');
    
    console.log('Starting server...');
    const server = spawn('/home/agent/java/jdk-21.0.11+10/bin/java', [
        '-Xmx512M', '-jar', 'server.jar', '--nogui'
    ], { cwd: serverDir, stdio: ['pipe', 'pipe', 'pipe'] });

    let log = '';
    server.stdout.on('data', d => log += d);
    server.stderr.on('data', d => log += d);

    // Wait for ready
    await new Promise((resolve) => {
        const check = setInterval(() => {
            if (log.includes('Done (')) { clearInterval(check); resolve(); }
        }, 500);
        setTimeout(() => { clearInterval(check); resolve(); }, 60000);
    });

    console.log('Server ready. Connecting client...');
    await new Promise(r => setTimeout(r, 2000));

    const client = mc.createClient({
        host: 'localhost', port: SERVER_PORT,
        username: 'DebugBot', version: '1.21.4', auth: 'offline',
    });

    const messages = [];

    // Log ALL packets
    client.on('system_chat', (p) => {
        console.log('SYSTEM_CHAT:', JSON.stringify(p).substring(0, 300));
        messages.push(p);
    });
    client.on('chat', (p) => {
        console.log('CHAT:', JSON.stringify(p).substring(0, 300));
    });
    client.on('profileless_chat', (p) => {
        console.log('PROFILELESS_CHAT:', JSON.stringify(p).substring(0, 300));
    });
    client.on('player_chat', (p) => {
        console.log('PLAYER_CHAT:', JSON.stringify(p).substring(0, 300));
    });
    client.on('action_bar', (p) => {
        console.log('ACTION_BAR:', JSON.stringify(p).substring(0, 300));
    });

    await new Promise((resolve) => {
        client.on('login', () => setTimeout(resolve, 2000));
        setTimeout(resolve, 10000);
    });

    console.log('\nSending RCON: say hello_test');
    await rconCmd('say hello_test');
    await new Promise(r => setTimeout(r, 3000));

    console.log('\nSending RCON: say cancel_me');
    await rconCmd('say cancel_me');
    await new Promise(r => setTimeout(r, 2000));

    console.log('\nSending RCON: flowchat reload');
    await rconCmd('flowchat reload');
    await new Promise(r => setTimeout(r, 2000));

    console.log('\nSending RCON: flowchat test');
    await rconCmd('flowchat test');
    await new Promise(r => setTimeout(r, 3000));

    console.log('\nTotal messages received:', messages.length);
    client.end();
    server.kill('SIGTERM');
    await new Promise(r => setTimeout(r, 3000));
    server.kill('SIGKILL');
}

function rconCmd(command) {
    return new Promise((resolve) => {
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
    });
}

debug().catch(e => { console.error(e); process.exit(1); });
