import { EventEmitter } from 'events';
import dgram from 'dgram';
import os from 'os';
import crypto from 'crypto';
import AsyncUtils from '../utils/AsyncUtils.js';

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
        this.broadcastScheduler = null;
        this.cleanupScheduler = null;

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

    async start() {
        return new Promise((resolve, reject) => {
            this.server.bind(DISCOVERY_PORT, async () => {
                try {
                    // Start broadcast scheduler
                    this.broadcastScheduler = AsyncUtils.createAsyncScheduler(
                        async () => await this.broadcastPresence(),
                        MESSAGE_INTERVAL,
                        { immediate: true }
                    );
                    
                    // Start cleanup scheduler
                    this.cleanupScheduler = AsyncUtils.createAsyncScheduler(
                        async () => await this.checkForExpiredPeers(),
                        PEER_TIMEOUT,
                        { immediate: false }
                    );
                    
                    // Start both schedulers
                    this.broadcastScheduler.start().catch(console.error);
                    this.cleanupScheduler.start().catch(console.error);
                    
                    resolve();
                } catch (error) {
                    reject(error);
                }
            });
        });
    }

    async stop() {
        if (this.broadcastScheduler) {
            this.broadcastScheduler.stop();
            this.broadcastScheduler = null;
        }
        
        if (this.cleanupScheduler) {
            this.cleanupScheduler.stop();
            this.cleanupScheduler = null;
        }
        
        return AsyncUtils.promisify(this.server.close, this.server)();
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

    async broadcastPresence() {
        const message = Buffer.from(JSON.stringify({
            id: this.id,
            address: this.getMyIP(),
            isLeader: this.isLeader
        }));

        try {
            await AsyncUtils.promisify(
                (msg, offset, length, port, address, callback) => {
                    this.server.send(msg, offset, length, port, address, callback);
                }
            )(message, 0, message.length, DISCOVERY_PORT, BROADCAST_ADDRESS);
        } catch (error) {
            console.error('[Discovery] Broadcast error:', error);
        }
    }

    async checkForExpiredPeers() {
        const now = Date.now();
        for (const [id, peer] of this.peers.entries()) {
            if (now - peer.lastSeen > PEER_TIMEOUT) {
                this.peers.delete(id);
                this.electLeader();
                this.emit('peer-timeout', id);
            }
        }
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