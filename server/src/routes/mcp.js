/**
 * MCP API Routes
 * 
 * HTTP endpoints for accessing MCP (Model Context Protocol) functionality
 * including the Sequential Thinking MCP for structured reasoning.
 */

import express from 'express';
import { mcpRegistry } from '../mcp/index.js';

const router = express.Router();

/**
 * GET /api/mcp/status
 * Get status of all registered MCPs
 */
router.get('/status', async (req, res) => {
  try {
    const mcps = mcpRegistry.getAll();
    res.json({
      success: true,
      mcps,
      initialized: mcpRegistry.initialized,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/initialize
 * Initialize all MCPs with configuration
 */
router.post('/initialize', async (req, res) => {
  try {
    const config = req.body.config || {};
    const results = await mcpRegistry.initializeAll(config);
    
    res.json({
      success: true,
      results,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/session
 * Start a new Sequential Thinking session
 */
router.post('/sequential-thinking/session', async (req, res) => {
  try {
    const { sessionId, context } = req.body;
    
    if (!sessionId) {
      return res.status(400).json({
        success: false,
        error: 'sessionId is required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = mcp.startSession(sessionId, context);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/execute
 * Execute a reasoning pattern in a session
 */
router.post('/sequential-thinking/execute', async (req, res) => {
  try {
    const { sessionId, pattern, input, options } = req.body;
    
    if (!sessionId || !pattern) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and pattern are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern(pattern, sessionId, input, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * GET /api/mcp/sequential-thinking/session/:sessionId
 * Get session summary
 */
router.get('/sequential-thinking/session/:sessionId', async (req, res) => {
  try {
    const { sessionId } = req.params;

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const summary = mcp.getSessionSummary(sessionId);
    if (!summary) {
      return res.status(404).json({
        success: false,
        error: 'Session not found'
      });
    }
    
    res.json({
      success: true,
      summary,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * DELETE /api/mcp/sequential-thinking/session/:sessionId
 * End a session
 */
router.delete('/sequential-thinking/session/:sessionId', async (req, res) => {
  try {
    const { sessionId } = req.params;

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = mcp.endSession(sessionId);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * GET /api/mcp/sequential-thinking/sessions
 * Get all active sessions
 */
router.get('/sequential-thinking/sessions', async (req, res) => {
  try {
    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const sessions = mcp.getActiveSessions();
    
    res.json({
      success: true,
      sessions,
      count: sessions.length,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * GET /api/mcp/sequential-thinking/history
 * Get reasoning history
 */
router.get('/sequential-thinking/history', async (req, res) => {
  try {
    const { limit = 10 } = req.query;

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const history = mcp.getReasoningHistory(parseInt(limit));
    
    res.json({
      success: true,
      history,
      count: history.length,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/problem-decomposition
 * Execute problem decomposition pattern
 */
router.post('/sequential-thinking/problem-decomposition', async (req, res) => {
  try {
    const { sessionId, problem, options } = req.body;
    
    if (!sessionId || !problem) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and problem are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern('problemDecomposition', sessionId, problem, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/decision-analysis
 * Execute decision analysis pattern
 */
router.post('/sequential-thinking/decision-analysis', async (req, res) => {
  try {
    const { sessionId, decision, alternatives, options } = req.body;
    
    if (!sessionId || !decision) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and decision are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const input = { decision, alternatives: alternatives || [] };
    const result = await mcp.executePattern('decisionAnalysis', sessionId, input, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/hypothesis-testing
 * Execute hypothesis testing pattern
 */
router.post('/sequential-thinking/hypothesis-testing', async (req, res) => {
  try {
    const { sessionId, hypothesis, options } = req.body;
    
    if (!sessionId || !hypothesis) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and hypothesis are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern('hypothesisTesting', sessionId, hypothesis, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/root-cause-analysis
 * Execute root cause analysis pattern
 */
router.post('/sequential-thinking/root-cause-analysis', async (req, res) => {
  try {
    const { sessionId, problem, options } = req.body;
    
    if (!sessionId || !problem) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and problem are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern('rootCauseAnalysis', sessionId, problem, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/optimization-analysis
 * Execute optimization analysis pattern
 */
router.post('/sequential-thinking/optimization-analysis', async (req, res) => {
  try {
    const { sessionId, system, options } = req.body;
    
    if (!sessionId || !system) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and system are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern('optimizationAnalysis', sessionId, system, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * POST /api/mcp/sequential-thinking/troubleshooting
 * Execute troubleshooting pattern
 */
router.post('/sequential-thinking/troubleshooting', async (req, res) => {
  try {
    const { sessionId, issue, options } = req.body;
    
    if (!sessionId || !issue) {
      return res.status(400).json({
        success: false,
        error: 'sessionId and issue are required'
      });
    }

    const mcp = mcpRegistry.get('sequentialThinking');
    if (!mcp) {
      return res.status(404).json({
        success: false,
        error: 'Sequential Thinking MCP not found'
      });
    }

    const result = await mcp.executePattern('troubleshooting', sessionId, issue, options);
    
    res.json({
      success: true,
      result,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * DELETE /api/mcp/cleanup
 * Cleanup all MCPs
 */
router.delete('/cleanup', async (req, res) => {
  try {
    const results = await mcpRegistry.cleanupAll();
    
    res.json({
      success: true,
      results,
      timestamp: Date.now()
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

export default router; 