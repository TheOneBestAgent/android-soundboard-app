import { promisify } from 'util';
import { spawn } from 'child_process';

/**
 * Async Utilities Library
 * Modern async/await patterns for common operations
 * Phase 4.3: Async/await modernization utilities
 */

export class AsyncUtils {
    /**
     * Create an async delay
     * @param {number} ms - Milliseconds to delay
     * @param {AbortSignal} signal - Optional abort signal for cancellation
     * @returns {Promise<void>}
     */
    static async delay(ms, signal = null) {
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(resolve, ms);
            
            if (signal) {
                const onAbort = () => {
                    clearTimeout(timeout);
                    reject(new Error('Delay aborted'));
                };
                
                if (signal.aborted) {
                    clearTimeout(timeout);
                    reject(new Error('Delay aborted'));
                    return;
                }
                
                signal.addEventListener('abort', onAbort, { once: true });
            }
        });
    }

    /**
     * Create an async interval that can be cancelled
     * @param {Function} callback - Function to call on each interval
     * @param {number} ms - Interval in milliseconds
     * @param {AbortSignal} signal - Abort signal for cancellation
     * @returns {Promise<void>}
     */
    static async asyncInterval(callback, ms, signal = null) {
        while (!signal?.aborted) {
            try {
                await callback();
                await this.delay(ms, signal);
            } catch (error) {
                if (signal?.aborted) break;
                throw error;
            }
        }
    }

    /**
     * Execute a process with async/await pattern
     * @param {string} command - Command to execute
     * @param {string[]} args - Command arguments
     * @param {Object} options - Spawn options
     * @param {AbortSignal} signal - Optional abort signal
     * @returns {Promise<{code: number, stdout: string, stderr: string}>}
     */
    static async executeProcess(command, args = [], options = {}, signal = null) {
        return new Promise((resolve, reject) => {
            const process = spawn(command, args, options);
            let stdout = '';
            let stderr = '';

            process.stdout?.on('data', (data) => {
                stdout += data.toString();
            });

            process.stderr?.on('data', (data) => {
                stderr += data.toString();
            });

            process.on('close', (code) => {
                resolve({ code, stdout, stderr });
            });

            process.on('error', (error) => {
                reject(new Error(`Process error: ${error.message}`));
            });

            if (signal) {
                const onAbort = () => {
                    process.kill('SIGTERM');
                    reject(new Error('Process aborted'));
                };

                if (signal.aborted) {
                    process.kill('SIGTERM');
                    reject(new Error('Process aborted'));
                    return;
                }

                signal.addEventListener('abort', onAbort, { once: true });
            }
        });
    }

    /**
     * Retry an async operation with exponential backoff
     * @param {Function} operation - Async function to retry
     * @param {Object} options - Retry options
     * @returns {Promise<any>}
     */
    static async retry(operation, options = {}) {
        const {
            maxAttempts = 3,
            baseDelay = 1000,
            maxDelay = 30000,
            backoffFactor = 2,
            signal = null
        } = options;

        let lastError;
        
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            if (signal?.aborted) {
                throw new Error('Retry operation aborted');
            }

            try {
                return await operation(attempt);
            } catch (error) {
                lastError = error;
                
                if (attempt === maxAttempts) {
                    throw error;
                }

                const delay = Math.min(
                    baseDelay * Math.pow(backoffFactor, attempt - 1),
                    maxDelay
                );

                console.warn(`Attempt ${attempt} failed, retrying in ${delay}ms:`, error.message);
                await this.delay(delay, signal);
            }
        }

        throw lastError;
    }

    /**
     * Create a timeout wrapper for async operations
     * @param {Function} operation - Async operation
     * @param {number} timeoutMs - Timeout in milliseconds
     * @param {string} timeoutMessage - Custom timeout message
     * @returns {Promise<any>}
     */
    static async withTimeout(operation, timeoutMs, timeoutMessage = 'Operation timed out') {
        const controller = new AbortController();
        
        const timeoutPromise = this.delay(timeoutMs).then(() => {
            controller.abort();
            throw new Error(timeoutMessage);
        });

        const operationPromise = operation(controller.signal);

        try {
            return await Promise.race([operationPromise, timeoutPromise]);
        } finally {
            controller.abort();
        }
    }

    /**
     * Create a promisified version of a callback-based function
     * @param {Function} fn - Function to promisify
     * @param {Object} thisArg - Context for the function
     * @returns {Function}
     */
    static promisify(fn, thisArg = null) {
        return promisify(fn.bind(thisArg));
    }

    /**
     * Handle process signals with async cleanup
     * @param {Object} handlers - Signal handlers {SIGTERM: async function, SIGINT: async function}
     * @returns {void}
     */
    static setupAsyncSignalHandlers(handlers) {
        const signalHandler = (signal) => {
            return async () => {
                console.log(`Received ${signal}, starting graceful shutdown...`);
                try {
                    if (handlers[signal]) {
                        await handlers[signal]();
                    }
                    console.log(`${signal} handler completed successfully`);
                    process.exit(0);
                } catch (error) {
                    console.error(`Error during ${signal} handling:`, error);
                    process.exit(1);
                }
            };
        };

        Object.keys(handlers).forEach(signal => {
            process.on(signal, signalHandler(signal));
        });
    }

    /**
     * Create an async task scheduler with cancellation support
     * @param {Function} task - Task to schedule
     * @param {number} intervalMs - Interval in milliseconds
     * @param {Object} options - Scheduler options
     * @returns {Object} - Scheduler with start/stop methods
     */
    static createAsyncScheduler(task, intervalMs, options = {}) {
        const { immediate = false, maxIterations = null } = options;
        let controller = null;
        let iterations = 0;

        const start = async () => {
            if (controller && !controller.signal.aborted) {
                throw new Error('Scheduler is already running');
            }

            controller = new AbortController();
            iterations = 0;

            try {
                if (immediate) {
                    await task();
                    iterations++;
                }

                while (!controller.signal.aborted) {
                    if (maxIterations && iterations >= maxIterations) {
                        break;
                    }

                    await this.delay(intervalMs, controller.signal);
                    
                    if (!controller.signal.aborted) {
                        await task();
                        iterations++;
                    }
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    throw error;
                }
            }
        };

        const stop = () => {
            if (controller) {
                controller.abort();
            }
        };

        const isRunning = () => {
            return controller && !controller.signal.aborted;
        };

        return { start, stop, isRunning, getIterations: () => iterations };
    }

    /**
     * Create async stream processor for handling process streams
     * @param {Stream} stream - Readable stream
     * @param {Function} processor - Data processor function
     * @param {AbortSignal} signal - Optional abort signal
     * @returns {Promise<void>}
     */
    static async processStream(stream, processor, signal = null) {
        return new Promise((resolve, reject) => {
            const chunks = [];

            stream.on('data', (chunk) => {
                if (signal?.aborted) return;
                
                try {
                    processor(chunk);
                    chunks.push(chunk);
                } catch (error) {
                    reject(error);
                }
            });

            stream.on('end', () => {
                resolve(chunks);
            });

            stream.on('error', (error) => {
                reject(error);
            });

            if (signal) {
                const onAbort = () => {
                    stream.destroy();
                    reject(new Error('Stream processing aborted'));
                };

                if (signal.aborted) {
                    stream.destroy();
                    reject(new Error('Stream processing aborted'));
                    return;
                }

                signal.addEventListener('abort', onAbort, { once: true });
            }
        });
    }
}

export default AsyncUtils;