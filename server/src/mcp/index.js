/**
 * MCP (Model Context Protocol) Index
 * 
 * Centralized export and management of all MCP components
 * for the Android Soundboard application.
 */

import SequentialThinkingMCP from './SequentialThinkingMCP.js';

// MCP Registry
class MCPRegistry {
  constructor() {
    this.mcps = new Map();
    this.initialized = false;
  }

  /**
   * Register an MCP instance
   */
  register(name, mcpInstance) {
    this.mcps.set(name, mcpInstance);
    console.log(`MCP registered: ${name}`);
  }

  /**
   * Get an MCP instance by name
   */
  get(name) {
    return this.mcps.get(name);
  }

  /**
   * Get all registered MCPs
   */
  getAll() {
    return Array.from(this.mcps.entries()).map(([name, mcp]) => ({
      name,
      type: mcp.constructor.name,
      status: mcp.initialized ? 'active' : 'inactive'
    }));
  }

  /**
   * Initialize all registered MCPs
   */
  async initializeAll(config = {}) {
    const results = [];
    
    for (const [name, mcp] of this.mcps) {
      try {
        const result = await mcp.initialize(config[name] || {});
        results.push({ name, success: true, result });
      } catch (error) {
        results.push({ name, success: false, error: error.message });
      }
    }

    this.initialized = true;
    return results;
  }

  /**
   * Cleanup all MCPs
   */
  async cleanupAll() {
    const results = [];
    
    for (const [name, mcp] of this.mcps) {
      try {
        if (typeof mcp.cleanup === 'function') {
          await mcp.cleanup();
        }
        results.push({ name, success: true });
      } catch (error) {
        results.push({ name, success: false, error: error.message });
      }
    }

    return results;
  }
}

// Create and configure registry
const mcpRegistry = new MCPRegistry();

// Register MCPs
mcpRegistry.register('sequentialThinking', new SequentialThinkingMCP());

// Export
export { SequentialThinkingMCP, mcpRegistry };
export default mcpRegistry; 