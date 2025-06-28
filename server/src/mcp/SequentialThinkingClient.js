/**
 * Sequential Thinking MCP Client
 * 
 * Client-side wrapper for the Sequential Thinking MCP API
 * Provides easy-to-use methods for structured reasoning
 */

class SequentialThinkingClient {
  constructor(baseUrl = 'http://localhost:3001') {
    this.baseUrl = baseUrl;
    this.activeSessions = new Map();
  }

  /**
   * Initialize the MCP system
   */
  async initialize(config = {}) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/initialize`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ config })
      });

      const result = await response.json();
      if (!result.success) {
        throw new Error(result.error);
      }

      return result;
    } catch (error) {
      throw new Error(`Failed to initialize MCP: ${error.message}`);
    }
  }

  /**
   * Start a new reasoning session
   */
  async startSession(sessionId, context = {}) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/session`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ sessionId, context })
      });

      const result = await response.json();
      if (!result.success) {
        throw new Error(result.error);
      }

      this.activeSessions.set(sessionId, result.result);
      return result.result;
    } catch (error) {
      throw new Error(`Failed to start session: ${error.message}`);
    }
  }

  /**
   * Execute a reasoning pattern
   */
  async executePattern(sessionId, pattern, input, options = {}) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/execute`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ sessionId, pattern, input, options })
      });

      const result = await response.json();
      if (!result.success) {
        throw new Error(result.error);
      }

      return result.result;
    } catch (error) {
      throw new Error(`Failed to execute pattern: ${error.message}`);
    }
  }

  /**
   * Problem decomposition
   */
  async decomposeProblem(sessionId, problem, options = {}) {
    return this.executePattern(sessionId, 'problemDecomposition', problem, options);
  }

  /**
   * Decision analysis
   */
  async analyzeDecision(sessionId, decision, alternatives = [], options = {}) {
    const input = { decision, alternatives };
    return this.executePattern(sessionId, 'decisionAnalysis', input, options);
  }

  /**
   * Hypothesis testing
   */
  async testHypothesis(sessionId, hypothesis, options = {}) {
    return this.executePattern(sessionId, 'hypothesisTesting', hypothesis, options);
  }

  /**
   * Root cause analysis
   */
  async analyzeRootCause(sessionId, problem, options = {}) {
    return this.executePattern(sessionId, 'rootCauseAnalysis', problem, options);
  }

  /**
   * Optimization analysis
   */
  async analyzeOptimization(sessionId, system, options = {}) {
    return this.executePattern(sessionId, 'optimizationAnalysis', system, options);
  }

  /**
   * Troubleshooting
   */
  async troubleshoot(sessionId, issue, options = {}) {
    return this.executePattern(sessionId, 'troubleshooting', issue, options);
  }

  /**
   * Get session summary
   */
  async getSessionSummary(sessionId) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/session/${sessionId}`);
      const result = await response.json();
      
      if (!result.success) {
        throw new Error(result.error);
      }

      return result.summary;
    } catch (error) {
      throw new Error(`Failed to get session summary: ${error.message}`);
    }
  }

  /**
   * End a session
   */
  async endSession(sessionId) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/session/${sessionId}`, {
        method: 'DELETE'
      });

      const result = await response.json();
      if (!result.success) {
        throw new Error(result.error);
      }

      this.activeSessions.delete(sessionId);
      return result.result;
    } catch (error) {
      throw new Error(`Failed to end session: ${error.message}`);
    }
  }

  /**
   * Get all active sessions
   */
  async getActiveSessions() {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/sessions`);
      const result = await response.json();
      
      if (!result.success) {
        throw new Error(result.error);
      }

      return result.sessions;
    } catch (error) {
      throw new Error(`Failed to get active sessions: ${error.message}`);
    }
  }

  /**
   * Get reasoning history
   */
  async getHistory(limit = 10) {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/sequential-thinking/history?limit=${limit}`);
      const result = await response.json();
      
      if (!result.success) {
        throw new Error(result.error);
      }

      return result.history;
    } catch (error) {
      throw new Error(`Failed to get history: ${error.message}`);
    }
  }

  /**
   * Get MCP status
   */
  async getStatus() {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/status`);
      const result = await response.json();
      
      if (!result.success) {
        throw new Error(result.error);
      }

      return result;
    } catch (error) {
      throw new Error(`Failed to get status: ${error.message}`);
    }
  }

  /**
   * Cleanup all MCPs
   */
  async cleanup() {
    try {
      const response = await fetch(`${this.baseUrl}/api/mcp/cleanup`, {
        method: 'DELETE'
      });

      const result = await response.json();
      if (!result.success) {
        throw new Error(result.error);
      }

      this.activeSessions.clear();
      return result.results;
    } catch (error) {
      throw new Error(`Failed to cleanup: ${error.message}`);
    }
  }

  /**
   * Convenience method for complete reasoning workflow
   */
  async completeReasoningWorkflow(problem, options = {}) {
    const sessionId = `workflow_${Date.now()}`;
    
    try {
      // Start session
      await this.startSession(sessionId, { problem: problem.description });

      // Decompose problem
      const decomposition = await this.decomposeProblem(sessionId, problem);
      
      // Analyze root causes if it's a problem
      let rootCauseAnalysis = null;
      if (problem.type === 'issue' || problem.type === 'problem') {
        rootCauseAnalysis = await this.analyzeRootCause(sessionId, problem);
      }

      // Generate optimization recommendations if it's a system
      let optimizationAnalysis = null;
      if (problem.type === 'system' || problem.type === 'optimization') {
        optimizationAnalysis = await this.analyzeOptimization(sessionId, problem);
      }

      // End session
      const sessionSummary = await this.endSession(sessionId);

      return {
        sessionId,
        decomposition,
        rootCauseAnalysis,
        optimizationAnalysis,
        sessionSummary
      };
    } catch (error) {
      // Clean up session on error
      try {
        await this.endSession(sessionId);
      } catch (cleanupError) {
        console.warn('Failed to cleanup session:', cleanupError.message);
      }
      throw error;
    }
  }

  /**
   * Decision making workflow
   */
  async decisionWorkflow(decision, alternatives, options = {}) {
    const sessionId = `decision_${Date.now()}`;
    
    try {
      // Start session
      await this.startSession(sessionId, { decision: decision.description });

      // Analyze decision
      const analysis = await this.analyzeDecision(sessionId, decision, alternatives);

      // Test hypotheses if needed
      let hypothesisResults = [];
      if (options.testHypotheses && analysis.details.recommendation) {
        const hypothesis = {
          statement: `The recommended option (${analysis.details.recommendation.option}) will achieve the desired outcome`,
          expectedOutcome: decision.expectedOutcome
        };
        const hypothesisResult = await this.testHypothesis(sessionId, hypothesis);
        hypothesisResults.push(hypothesisResult);
      }

      // End session
      const sessionSummary = await this.endSession(sessionId);

      return {
        sessionId,
        analysis,
        hypothesisResults,
        sessionSummary
      };
    } catch (error) {
      // Clean up session on error
      try {
        await this.endSession(sessionId);
      } catch (cleanupError) {
        console.warn('Failed to cleanup session:', cleanupError.message);
      }
      throw error;
    }
  }
}

// Export for different environments
if (typeof module !== 'undefined' && module.exports) {
  // Node.js
  module.exports = SequentialThinkingClient;
} else if (typeof window !== 'undefined') {
  // Browser
  window.SequentialThinkingClient = SequentialThinkingClient;
}

export default SequentialThinkingClient; 