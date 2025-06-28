# In any Cursor project:
import sys
sys.path.append('/Users/Darius/cursor_mcp_bridge')
from cursor_mcp_tools import *

# Use all your tools:
save_code("my_function", "def test(): pass")
remember("project_status", "Almost done")
add_context("Working on authentication")