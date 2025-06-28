/**
 * Sequential Thinking MCP Test Script
 * 
 * Demonstrates and validates the Sequential Thinking MCP functionality
 * for the Android Soundboard application.
 */

import SequentialThinkingClient from './server/src/mcp/SequentialThinkingClient.js';

async function testSequentialThinkingMCP() {
  console.log('🧠 Testing Sequential Thinking MCP...\n');

  const client = new SequentialThinkingClient('http://localhost:3001');

  try {
    // Test 1: Initialize MCP
    console.log('1. Initializing MCP...');
    const initResult = await client.initialize({
      sequentialThinking: {
        enableMemoryPersistence: true,
        enableContextTracking: true
      }
    });
    console.log('✅ MCP initialized successfully\n');

    // Test 2: Get MCP status
    console.log('2. Checking MCP status...');
    const status = await client.getStatus();
    console.log('📊 MCP Status:', JSON.stringify(status, null, 2), '\n');

    // Test 3: Problem Decomposition
    console.log('3. Testing Problem Decomposition...');
    const sessionId = `test_${Date.now()}`;
    await client.startSession(sessionId, { 
      context: 'Android Soundboard performance optimization' 
    });

    const problem = {
      description: 'Android Soundboard app has slow response times when playing audio files',
      type: 'performance',
      scope: 'application',
      stakeholders: ['users', 'developers'],
      constraints: ['must maintain audio quality', 'must work on older devices']
    };

    const decomposition = await client.decomposeProblem(sessionId, problem);
    console.log('🔍 Problem Decomposition Result:');
    console.log('   Sub-problems:', decomposition.summary.subProblems);
    console.log('   Complexity:', decomposition.summary.complexity);
    console.log('   Estimated effort:', decomposition.summary.estimatedEffort);
    console.log('   Roadmap phases:', decomposition.details.roadmap.phases.length, '\n');

    // Test 4: Decision Analysis
    console.log('4. Testing Decision Analysis...');
    const decision = {
      description: 'Choose the best audio playback optimization strategy',
      expectedOutcome: 'Improved response time without quality loss'
    };

    const alternatives = [
      {
        name: 'Audio Caching',
        pros: ['Fast playback', 'Reduced loading time'],
        cons: ['Memory usage', 'Storage space']
      },
      {
        name: 'Streaming Optimization',
        pros: ['Low memory usage', 'Scalable'],
        cons: ['Network dependency', 'Initial delay']
      },
      {
        name: 'Hybrid Approach',
        pros: ['Best of both worlds', 'Flexible'],
        cons: ['Complex implementation', 'Higher development cost']
      }
    ];

    const decisionAnalysis = await client.analyzeDecision(sessionId, decision, alternatives);
    console.log('🤔 Decision Analysis Result:');
    console.log('   Recommended option:', decisionAnalysis.summary.recommendedOption);
    console.log('   Confidence:', decisionAnalysis.summary.confidence);
    console.log('   Key factors:', decisionAnalysis.summary.keyFactors.join(', '), '\n');

    // Test 5: Root Cause Analysis
    console.log('5. Testing Root Cause Analysis...');
    const issue = {
      description: 'Audio playback delays in Android Soundboard',
      symptoms: ['Slow response', 'Audio stuttering', 'High CPU usage'],
      type: 'performance',
      timeline: 'recent'
    };

    const rootCauseAnalysis = await client.analyzeRootCause(sessionId, issue);
    console.log('🔍 Root Cause Analysis Result:');
    console.log('   Root causes found:', rootCauseAnalysis.summary.rootCauses);
    console.log('   Primary cause:', rootCauseAnalysis.summary.primaryCause);
    console.log('   Recommended actions:', rootCauseAnalysis.summary.recommendedActions.join(', '), '\n');

    // Test 6: Optimization Analysis
    console.log('6. Testing Optimization Analysis...');
    const system = {
      name: 'Android Soundboard Audio System',
      components: ['Audio Player', 'File Manager', 'Cache System', 'Network Layer']
    };

    const optimizationAnalysis = await client.analyzeOptimization(sessionId, system);
    console.log('⚡ Optimization Analysis Result:');
    console.log('   Current performance:', optimizationAnalysis.summary.currentPerformance);
    console.log('   Bottlenecks found:', optimizationAnalysis.summary.bottlenecks);
    console.log('   Expected improvement:', optimizationAnalysis.summary.expectedImprovement + '%');
    console.log('   Implementation effort:', optimizationAnalysis.summary.implementationEffort, '\n');

    // Test 7: Complete Workflow
    console.log('7. Testing Complete Reasoning Workflow...');
    const workflowProblem = {
      description: 'Improve Android Soundboard user experience',
      type: 'optimization',
      scope: 'application-wide'
    };

    const workflowResult = await client.completeReasoningWorkflow(workflowProblem);
    console.log('🔄 Complete Workflow Result:');
    console.log('   Session ID:', workflowResult.sessionId);
    console.log('   Decomposition completed:', !!workflowResult.decomposition);
    console.log('   Optimization analysis completed:', !!workflowResult.optimizationAnalysis);
    console.log('   Session summary:', workflowResult.sessionSummary.status, '\n');

    // Test 8: Get History
    console.log('8. Testing History Retrieval...');
    const history = await client.getHistory(5);
    console.log('📚 Recent History:');
    console.log('   History entries:', history.length);
    if (history.length > 0) {
      console.log('   Latest session:', history[history.length - 1].sessionId);
    }
    console.log('');

    // Test 9: Get Active Sessions
    console.log('9. Testing Active Sessions...');
    const activeSessions = await client.getActiveSessions();
    console.log('🟢 Active Sessions:');
    console.log('   Active sessions:', activeSessions.length);
    activeSessions.forEach(session => {
      console.log(`   - ${session.id}: ${session.patterns} patterns`);
    });
    console.log('');

    // Test 10: End Session
    console.log('10. Ending test session...');
    const endResult = await client.endSession(sessionId);
    console.log('✅ Session ended successfully');
    console.log('   Duration:', endResult.summary.duration + 'ms');
    console.log('   Patterns executed:', endResult.summary.patterns.length, '\n');

    console.log('🎉 All Sequential Thinking MCP tests completed successfully!');
    console.log('📊 Summary:');
    console.log('   ✅ MCP initialization');
    console.log('   ✅ Problem decomposition');
    console.log('   ✅ Decision analysis');
    console.log('   ✅ Root cause analysis');
    console.log('   ✅ Optimization analysis');
    console.log('   ✅ Complete workflow');
    console.log('   ✅ Session management');
    console.log('   ✅ History tracking');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
    console.error('Stack trace:', error.stack);
  } finally {
    // Cleanup
    try {
      await client.cleanup();
      console.log('🧹 Cleanup completed');
    } catch (cleanupError) {
      console.warn('⚠️ Cleanup warning:', cleanupError.message);
    }
  }
}

// Run the test if this file is executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  testSequentialThinkingMCP().catch(console.error);
}

export { testSequentialThinkingMCP }; 