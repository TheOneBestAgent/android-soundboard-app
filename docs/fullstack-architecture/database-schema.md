# Database Schema

## SQLite Schema (Local Desktop Server Database)
```sql
-- Users and Authentication (Local Desktop)
CREATE TABLE users (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    email TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    avatar_url TEXT,
    subscription_tier TEXT DEFAULT 'free',
    preferences TEXT DEFAULT '{}', -- JSON string
    email_verified INTEGER DEFAULT 0, -- SQLite boolean
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Soundboards
CREATE TABLE soundboards (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    layout TEXT NOT NULL DEFAULT '{"rows": 4, "columns": 4}', -- JSON string
    is_default INTEGER DEFAULT 0, -- SQLite boolean
    is_shared INTEGER DEFAULT 0, -- SQLite boolean
    share_code TEXT UNIQUE,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Sound Buttons
CREATE TABLE sound_buttons (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    soundboard_id TEXT NOT NULL REFERENCES soundboards(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    audio_file_url TEXT NOT NULL,
    local_file_path TEXT,
    duration REAL NOT NULL,
    waveform_data TEXT, -- JSON string
    tags TEXT DEFAULT '[]', -- JSON array as string
    category TEXT,
    volume REAL DEFAULT 1.0 CHECK (volume >= 0 AND volume <= 2.0),
    pitch REAL DEFAULT 1.0 CHECK (pitch >= 0.5 AND pitch <= 2.0),
    effects TEXT DEFAULT '[]', -- JSON string
    hotkey TEXT,
    color TEXT, -- Hex color code
    icon TEXT,
    position_row INTEGER,
    position_col INTEGER,
    is_custom INTEGER DEFAULT 1, -- SQLite boolean
    play_count INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Audio Libraries
CREATE TABLE audio_libraries (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    is_public INTEGER DEFAULT 0, -- SQLite boolean
    download_count INTEGER DEFAULT 0,
    rating REAL DEFAULT 0.0 CHECK (rating >= 0 AND rating <= 5.0),
    rating_count INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Library Sounds (many-to-many relationship)
CREATE TABLE library_sounds (
    library_id TEXT NOT NULL REFERENCES audio_libraries(id) ON DELETE CASCADE,
    sound_button_id TEXT NOT NULL REFERENCES sound_buttons(id) ON DELETE CASCADE,
    added_at INTEGER DEFAULT (strftime('%s', 'now')),
    PRIMARY KEY (library_id, sound_button_id)
);

-- Categories
CREATE TABLE categories (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    name TEXT UNIQUE NOT NULL,
    color TEXT NOT NULL,
    icon TEXT NOT NULL,
    sound_count INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Streaming Sessions
CREATE TABLE streaming_sessions (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform TEXT NOT NULL,
    platform_user_id TEXT,
    status TEXT DEFAULT 'active',
    start_time INTEGER DEFAULT (strftime('%s', 'now')),
    end_time INTEGER,
    sounds_played INTEGER DEFAULT 0,
    total_duration INTEGER, -- Duration in seconds
    metadata TEXT DEFAULT '{}' -- JSON string
);

-- AI Processing Jobs
CREATE TABLE ai_processing_jobs (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    status TEXT DEFAULT 'pending',
    input_file_url TEXT NOT NULL,
    output_file_url TEXT,
    parameters TEXT DEFAULT '{}', -- JSON string
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    error_message TEXT,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    started_at INTEGER,
    completed_at INTEGER
);

-- VoiceMeeter Configurations
CREATE TABLE voicemeeter_configs (
    id TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    config_data TEXT NOT NULL, -- JSON string
    is_active INTEGER DEFAULT 0, -- SQLite boolean
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Indexes for performance
CREATE INDEX idx_soundboards_user_id ON soundboards(user_id);
CREATE INDEX idx_sound_buttons_soundboard_id ON sound_buttons(soundboard_id);
CREATE INDEX idx_sound_buttons_category ON sound_buttons(category);
CREATE INDEX idx_audio_libraries_public ON audio_libraries(is_public) WHERE is_public = 1;
CREATE INDEX idx_streaming_sessions_user_platform ON streaming_sessions(user_id, platform);
CREATE INDEX idx_ai_jobs_user_status ON ai_processing_jobs(user_id, status);

-- Triggers for updated_at (SQLite syntax)
CREATE TRIGGER update_users_updated_at 
    AFTER UPDATE ON users
    FOR EACH ROW
    BEGIN
        UPDATE users SET updated_at = strftime('%s', 'now') WHERE id = NEW.id;
    END;

CREATE TRIGGER update_soundboards_updated_at 
    AFTER UPDATE ON soundboards
    FOR EACH ROW
    BEGIN
        UPDATE soundboards SET updated_at = strftime('%s', 'now') WHERE id = NEW.id;
    END;

CREATE TRIGGER update_sound_buttons_updated_at 
    AFTER UPDATE ON sound_buttons
    FOR EACH ROW
    BEGIN
        UPDATE sound_buttons SET updated_at = strftime('%s', 'now') WHERE id = NEW.id;
    END;

CREATE TRIGGER update_audio_libraries_updated_at 
    AFTER UPDATE ON audio_libraries
    FOR EACH ROW
    BEGIN
        UPDATE audio_libraries SET updated_at = strftime('%s', 'now') WHERE id = NEW.id;
    END;

CREATE TRIGGER update_voicemeeter_configs_updated_at 
    AFTER UPDATE ON voicemeeter_configs
    FOR EACH ROW
    BEGIN
        UPDATE voicemeeter_configs SET updated_at = strftime('%s', 'now') WHERE id = NEW.id;
    END;
```

## SQLite Schema (Local Database)
```sql
-- Local cache of user data for offline functionality
CREATE TABLE local_soundboards (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    layout TEXT NOT NULL, -- JSON string
    is_default INTEGER DEFAULT 0,
    sync_status TEXT DEFAULT 'synced', -- synced, pending, conflict
    last_synced INTEGER, -- Unix timestamp
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE local_sound_buttons (
    id TEXT PRIMARY KEY,
    soundboard_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    local_file_path TEXT NOT NULL,
    backup_file_path TEXT,
    duration REAL NOT NULL,
    waveform_data TEXT, -- JSON string
    tags TEXT, -- JSON array as string
    category TEXT,
    volume REAL DEFAULT 1.0,
    pitch REAL DEFAULT 1.0,
    effects TEXT DEFAULT '[]', -- JSON string
    hotkey TEXT,
    color TEXT,
    icon TEXT,
    position_row INTEGER,
    position_col INTEGER,
    play_count INTEGER DEFAULT 0,
    sync_status TEXT DEFAULT 'synced',
    last_synced INTEGER,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (soundboard_id) REFERENCES local_soundboards(id) ON DELETE CASCADE
);

CREATE TABLE local_user_preferences (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operation TEXT NOT NULL, -- create, update, delete
    entity_type TEXT NOT NULL, -- soundboard, sound_button
    entity_id TEXT NOT NULL,
    data TEXT, -- JSON string of the data to sync
    created_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- Indexes
CREATE INDEX idx_local_sound_buttons_soundboard_id ON local_sound_buttons(soundboard_id);
CREATE INDEX idx_local_sound_buttons_category ON local_sound_buttons(category);
CREATE INDEX idx_sync_queue_entity ON sync_queue(entity_type, entity_id);
```

---
