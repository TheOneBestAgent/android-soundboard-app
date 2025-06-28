# Sequential Thinking MCP (Model Context Protocol)

## Overview

The Sequential Thinking MCP provides structured reasoning capabilities for complex decision-making and problem-solving in the Android Soundboard application. This implementation offers step-by-step reasoning chains, multi-step problem decomposition, decision tree analysis, hypothesis testing, and context-aware reasoning.

## Features

### 🧠 Core Reasoning Patterns

1. **Problem Decomposition** - Breaks down complex problems into manageable sub-problems
2. **Decision Analysis** - Analyzes decisions with pros/cons and impact assessment
3. **Hypothesis Testing** - Tests hypotheses through systematic validation
4. **Root Cause Analysis** - Identifies underlying causes of problems
5. **Optimization Analysis** - Analyzes and optimizes system performance
6. **Troubleshooting** - Systematic approach to problem resolution

### 🔧 Technical Features

- **Session Management** - Track reasoning sessions with context and history
- **Memory Persistence** - Save and load reasoning history and context
- **Context Tracking** - Maintain context across reasoning chains
- **Performance Monitoring** - Monitor reasoning performance and patterns
- **API Integration** - RESTful API endpoints for all functionality
- **Client Library** - Easy-to-use client wrapper for integration

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Sequential Thinking MCP                  │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   MCP Registry  │  │  Session Mgmt   │  │   Memory     │ │
│  │                 │  │                 │  │   Persistence│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Problem       │  │   Decision      │  │  Hypothesis  │ │
│  │ Decomposition   │  │   Analysis      │  │   Testing    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Root Cause    │  │  Optimization   │  │Troubleshooting│ │
│  │   Analysis      │  │   Analysis      │  │               │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    REST API Layer                           │
├─────────────────────────────────────────────────────────────┤
│                    Client Library                           │
└─────────────────────────────────────────────────────────────┘
```

## Installation & Setup

### Prerequisites

- Node.js 18.0.0 or higher
- Android Soundboard server running on port 3001

### Installation

1. **Server Integration** (Already included)
   ```bash
   # The MCP is already integrated into the server
   cd server
   npm install
   ```

2. **Client Library** (Optional)
   ```javascript
   import SequentialThinkingClient from './server/src/mcp/SequentialThinkingClient.js';
   ```

### Configuration

The MCP can be configured through the initialization API:

```javascript
const config = {
  enableMemoryPersistence: true,
  enableContextTracking: true,
  enableChainValidation: true,
  enablePerformanceMonitoring: true,
  memoryFilePath: './data/sequential-thinking-memory.json'
};
```

## API Reference

### Core Endpoints

#### Initialize MCP
```http
POST /api/mcp/initialize
Content-Type: application/json

{
  "config": {
    "sequentialThinking": {
      "enableMemoryPersistence": true,
      "enableContextTracking": true
    }
  }
}
```

#### Start Session
```http
POST /api/mcp/sequential-thinking/session
Content-Type: application/json

{
  "sessionId": "unique_session_id",
  "context": {
    "project": "Android Soundboard",
    "domain": "audio_optimization"
  }
}
```

#### Execute Pattern
```http
POST /api/mcp/sequential-thinking/execute
Content-Type: application/json

{
  "sessionId": "session_id",
  "pattern": "problemDecomposition",
  "input": {
    "description": "Problem description",
    "type": "performance"
  },
  "options": {}
}
```

### Pattern-Specific Endpoints

#### Problem Decomposition
```http
POST /api/mcp/sequential-thinking/problem-decomposition
Content-Type: application/json

{
  "sessionId": "session_id",
  "problem": {
    "description": "Android Soundboard has slow response times",
    "type": "performance",
    "scope": "application"
  }
}
```

#### Decision Analysis
```http
POST /api/mcp/sequential-thinking/decision-analysis
Content-Type: application/json

{
  "sessionId": "session_id",
  "decision": {
    "description": "Choose audio optimization strategy"
  },
  "alternatives": [
    {
      "name": "Audio Caching",
      "pros": ["Fast playback"],
      "cons": ["Memory usage"]
    }
  ]
}
```

#### Root Cause Analysis
```http
POST /api/mcp/sequential-thinking/root-cause-analysis
Content-Type: application/json

{
  "sessionId": "session_id",
  "problem": {
    "description": "Audio playback delays",
    "symptoms": ["Slow response", "Stuttering"],
    "type": "performance"
  }
}
```

#### Optimization Analysis
```http
POST /api/mcp/sequential-thinking/optimization-analysis
Content-Type: application/json

{
  "sessionId": "session_id",
  "system": {
    "name": "Audio System",
    "components": ["Player", "Cache", "Network"]
  }
}
```

#### Hypothesis Testing
```http
POST /api/mcp/sequential-thinking/hypothesis-testing
Content-Type: application/json

{
  "sessionId": "session_id",
  "hypothesis": {
    "statement": "Audio caching will improve performance",
    "expectedOutcome": "Faster playback times"
  }
}
```

#### Troubleshooting
```http
POST /api/mcp/sequential-thinking/troubleshooting
Content-Type: application/json

{
  "sessionId": "session_id",
  "issue": {
    "description": "Audio not playing",
    "symptoms": ["No sound", "App crashes"],
    "type": "functional"
  }
}
```

### Management Endpoints

#### Get Session Summary
```http
GET /api/mcp/sequential-thinking/session/{sessionId}
```

#### End Session
```http
DELETE /api/mcp/sequential-thinking/session/{sessionId}
```

#### Get Active Sessions
```http
GET /api/mcp/sequential-thinking/sessions
```

#### Get History
```http
GET /api/mcp/sequential-thinking/history?limit=10
```

#### Get MCP Status
```http
GET /api/mcp/status
```

#### Cleanup
```http
DELETE /api/mcp/cleanup
```

## Client Library Usage

### Basic Usage

```javascript
import SequentialThinkingClient from './server/src/mcp/SequentialThinkingClient.js';

const client = new SequentialThinkingClient('http://localhost:3001');

// Initialize
await client.initialize();

// Start session
const sessionId = 'my_session';
await client.startSession(sessionId, { context: 'audio_optimization' });

// Decompose problem
const problem = {
  description: 'Slow audio playback in Android app',
  type: 'performance'
};

const result = await client.decomposeProblem(sessionId, problem);
console.log('Sub-problems:', result.summary.subProblems);

// End session
await client.endSession(sessionId);
```

### Advanced Workflows

#### Complete Reasoning Workflow

```javascript
const problem = {
  description: 'Improve Android Soundboard performance',
  type: 'optimization'
};

const workflow = await client.completeReasoningWorkflow(problem);
console.log('Decomposition:', workflow.decomposition);
console.log('Optimization:', workflow.optimizationAnalysis);
```

#### Decision Making Workflow

```javascript
const decision = {
  description: 'Choose audio optimization strategy',
  expectedOutcome: 'Improved performance'
};

const alternatives = [
  { name: 'Caching', pros: ['Fast'], cons: ['Memory'] },
  { name: 'Streaming', pros: ['Efficient'], cons: ['Network'] }
];

const analysis = await client.decisionWorkflow(decision, alternatives);
console.log('Recommended:', analysis.analysis.summary.recommendedOption);
```

## Use Cases

### 1. Performance Optimization

```javascript
// Analyze audio system performance
const system = {
  name: 'Android Soundboard Audio System',
  components: ['Audio Player', 'File Manager', 'Cache']
};

const optimization = await client.analyzeOptimization(sessionId, system);
console.log('Bottlenecks:', optimization.summary.bottlenecks);
console.log('Improvement:', optimization.summary.expectedImprovement + '%');
```

### 2. Bug Investigation

```javascript
// Root cause analysis for audio issues
const issue = {
  description: 'Audio playback fails on certain devices',
  symptoms: ['No sound', 'App crashes', 'High CPU usage'],
  type: 'functional'
};

const analysis = await client.analyzeRootCause(sessionId, issue);
console.log('Root cause:', analysis.summary.primaryCause);
console.log('Actions:', analysis.summary.recommendedActions);
```

### 3. Feature Planning

```javascript
// Decompose complex feature requirements
const feature = {
  description: 'Implement real-time audio streaming',
  type: 'feature',
  scope: 'application-wide'
};

const decomposition = await client.decomposeProblem(sessionId, feature);
console.log('Components:', decomposition.details.subProblems);
console.log('Timeline:', decomposition.details.roadmap.totalDuration);
```

### 4. Architecture Decisions

```javascript
// Analyze architectural choices
const decision = {
  description: 'Choose audio processing architecture',
  expectedOutcome: 'Scalable, maintainable solution'
};

const alternatives = [
  { name: 'Native Android', pros: ['Performance'], cons: ['Platform lock-in'] },
  { name: 'Cross-platform', pros: ['Portability'], cons: ['Performance overhead'] }
];

const analysis = await client.analyzeDecision(sessionId, decision, alternatives);
console.log('Recommendation:', analysis.summary.recommendedOption);
```

## Testing

### Run Test Suite

```bash
# Start the server first
cd server
npm start

# In another terminal, run the test
node test_sequential_thinking.js
```

### Test Output Example

```
🧠 Testing Sequential Thinking MCP...

1. Initializing MCP...
✅ MCP initialized successfully

2. Checking MCP status...
📊 MCP Status: {
  "success": true,
  "mcps": [
    {
      "name": "sequentialThinking",
      "type": "SequentialThinkingMCP",
      "status": "active"
    }
  ],
  "initialized": true
}

3. Testing Problem Decomposition...
🔍 Problem Decomposition Result:
   Sub-problems: 1
   Complexity: medium
   Estimated effort: 4 weeks
   Roadmap phases: 1

4. Testing Decision Analysis...
🤔 Decision Analysis Result:
   Recommended option: Audio Caching
   Confidence: 0.85
   Key factors: performance, cost, maintainability

🎉 All Sequential Thinking MCP tests completed successfully!
```

## Integration with Android App

### Kotlin Integration Example

```kotlin
// In your Android app
class SequentialThinkingService {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.1.100:3001" // Your server IP
    
    suspend fun analyzePerformanceIssue(issue: String): String {
        val request = Request.Builder()
            .url("$baseUrl/api/mcp/sequential-thinking/root-cause-analysis")
            .post(
                JSONObject().apply {
                    put("sessionId", "android_session_${System.currentTimeMillis()}")
                    put("problem", JSONObject().apply {
                        put("description", issue)
                        put("type", "performance")
                    })
                }.toString().toRequestBody("application/json".toMediaType())
            )
            .build()
            
        val response = client.newCall(request).execute()
        return response.body?.string() ?: "{}"
    }
}
```

## Performance Considerations

### Memory Management

- Sessions are automatically cleaned up after completion
- Memory persistence is configurable and can be disabled
- Context memory has configurable size limits

### Scalability

- Each session is independent and can run concurrently
- No shared state between sessions
- Stateless API design for horizontal scaling

### Monitoring

- Performance metrics are tracked per pattern execution
- Session duration and pattern usage statistics
- Error tracking and logging

## Troubleshooting

### Common Issues

1. **Server Connection Failed**
   - Ensure the Android Soundboard server is running
   - Check the server URL in client configuration
   - Verify network connectivity

2. **Session Not Found**
   - Sessions expire after completion
   - Use unique session IDs
   - Check session status before operations

3. **Pattern Execution Failed**
   - Verify input format matches pattern requirements
   - Check server logs for detailed error messages
   - Ensure session is active

### Debug Mode

Enable debug logging by setting the environment variable:

```bash
export DEBUG_MCP=true
```

## Future Enhancements

### Planned Features

1. **Machine Learning Integration**
   - Pattern learning from historical data
   - Automated pattern selection
   - Predictive reasoning

2. **Visualization**
   - Reasoning chain visualization
   - Decision tree diagrams
   - Performance analytics dashboard

3. **Advanced Patterns**
   - Risk assessment patterns
   - Cost-benefit analysis
   - Stakeholder impact analysis

4. **Integration Enhancements**
   - WebSocket support for real-time updates
   - GraphQL API for complex queries
   - Plugin system for custom patterns

## Contributing

### Development Setup

1. Clone the repository
2. Install dependencies: `npm install`
3. Start the server: `npm start`
4. Run tests: `node test_sequential_thinking.js`

### Adding New Patterns

1. Extend the `SequentialThinkingMCP` class
2. Add pattern method to `reasoningPatterns` object
3. Create corresponding API endpoint
4. Add tests and documentation

### Code Style

- Follow existing code patterns
- Add comprehensive JSDoc comments
- Include error handling
- Write unit tests for new features

## License

This Sequential Thinking MCP implementation is part of the Android Soundboard project and follows the same licensing terms.

## Support

For issues and questions:

1. Check the troubleshooting section
2. Review server logs for error details
3. Run the test suite to validate functionality
4. Create an issue with detailed reproduction steps

---

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Compatibility:** Android Soundboard v8.0.0+ 