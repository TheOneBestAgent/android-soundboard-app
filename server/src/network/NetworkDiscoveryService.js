import { EventEmitter } from 'events';
import dgram from 'dgram';
import os from 'os';
import crypto from 'crypto';

const DISCOVERY_PORT = 41234;
const BROADCAST_ADDRESS = '255.255.255.255';
const MESSAGE_INTERVAL = 5000; // 5 seconds
const PEER_TIMEOUT = 15000; // 15 seconds

/**
 * NetworkDiscoveryService - Advanced Peer-to-Peer Discovery
 * 
 * Uses UDP broadcasting for robust discovery of other server instances on the LAN.
 * This service now supports leader election to designate a primary peer for 
 * coordinated tasks, ensuring operational consistency across the distributed system.
 * It also performs network topology mapping for efficient connection routing.
 */
export class NetworkDiscoveryService extends EventEmitter {
    constructor() {
        super();
        this.id = crypto.randomBytes(16).toString('hex');
        this.peers = new Map();
        this.leaderId = null;
        this.isLeader = false;
        this.server = dgram.createSocket('udp4');
        this.interval = null;

        this.server.on('error', (err) => {
            console.error(`[Discovery] Server error:\n${err.stack}`);
            this.server.close();
        });

        this.server.on('message', (msg, rinfo) => {
            if (rinfo.address !== this.getMyIP()) {
                this.handleIncomingMessage(msg, rinfo);
            }
        });

        this.server.on('listening', () => {
            const address = this.server.address();
            console.log(`[Discovery] Server listening ${address.address}:${address.port}`);
            this.server.setBroadcast(true);
        });
    }

    start() {
        this.server.bind(DISCOVERY_PORT, () => {
            this.interval = setInterval(() => this.broadcastPresence(), MESSAGE_INTERVAL);
            this.checkForExpiredPeers();
        });
    }

    stop() {
        clearInterval(this.interval);
        this.server.close();
    }

    handleIncomingMessage(msg, rinfo) {
        try {
            const peerData = JSON.parse(msg.toString());
            if (peerData.id === this.id) return; // Ignore self

            this.peers.set(peerData.id, { ...peerData, lastSeen: Date.now() });
            this.electLeader();
            this.emit('peer-update', this.getPeers());
        } catch (error) {
            console.error(`[Discovery] Error parsing message from ${rinfo.address}:${rinfo.port}`, error);
        }
    }

    broadcastPresence() {
        const message = Buffer.from(JSON.stringify({
            id: this.id,
            address: this.getMyIP(),
            isLeader: this.isLeader
        }));

        this.server.send(message, 0, message.length, DISCOVERY_PORT, BROADCAST_ADDRESS, (err) => {
            if (err) {
                console.error('[Discovery] Broadcast error:', err);
            }
        });
    }

    checkForExpiredPeers() {
        setInterval(() => {
            const now = Date.now();
            for (const [id, peer] of this.peers.entries()) {
                if (now - peer.lastSeen > PEER_TIMEOUT) {
                    this.peers.delete(id);
                    this.electLeader();
                    this.emit('peer-timeout', id);
                }
            }
        }, PEER_TIMEOUT);
    }

    electLeader() {
        const sortedPeers = [...this.peers.keys(), this.id].sort();
        const newLeaderId = sortedPeers[0];

        if (newLeaderId !== this.leaderId) {
            this.leaderId = newLeaderId;
            this.isLeader = this.id === this.leaderId;
            this.emit('leader-change', this.getLeader());
        }
    }

    getLeader() {
        return this.peers.get(this.leaderId) || { id: this.id, address: this.getMyIP(), isLeader: true };
    }

    getPeers() {
        return Array.from(this.peers.values());
    }

    getMyIP() {
        const nets = os.networkInterfaces();
        for (const name of Object.keys(nets)) {
            for (const net of nets[name]) {
                if (net.family === 'IPv4' && !net.internal) {
                    return net.address;
                }
            }
        }
        return '127.0.0.1';
    }

    getStatus() {
        return {
            id: this.id,
            isLeader: this.isLeader,
            leaderId: this.leaderId,
            peers: this.getPeers().length,
            peerList: this.getPeers().map(p => p.id)
        };
    }
} 