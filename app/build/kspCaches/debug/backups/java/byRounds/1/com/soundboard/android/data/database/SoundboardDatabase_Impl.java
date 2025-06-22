package com.soundboard.android.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.soundboard.android.data.dao.ConnectionHistoryDao;
import com.soundboard.android.data.dao.ConnectionHistoryDao_Impl;
import com.soundboard.android.data.dao.SoundButtonDao;
import com.soundboard.android.data.dao.SoundButtonDao_Impl;
import com.soundboard.android.data.dao.SoundboardLayoutDao;
import com.soundboard.android.data.dao.SoundboardLayoutDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SoundboardDatabase_Impl extends SoundboardDatabase {
  private volatile SoundButtonDao _soundButtonDao;

  private volatile SoundboardLayoutDao _soundboardLayoutDao;

  private volatile ConnectionHistoryDao _connectionHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sound_buttons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `file_path` TEXT NOT NULL, `is_local_file` INTEGER NOT NULL, `position_x` INTEGER NOT NULL, `position_y` INTEGER NOT NULL, `color` TEXT NOT NULL, `icon_name` TEXT, `volume` REAL NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `soundboard_layouts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `is_active` INTEGER NOT NULL, `grid_columns` INTEGER NOT NULL, `grid_rows` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `is_template` INTEGER NOT NULL, `template_category` TEXT, `background_color` TEXT NOT NULL, `accent_color` TEXT NOT NULL, `button_spacing` REAL NOT NULL, `corner_radius` REAL NOT NULL, `enable_glow_effect` INTEGER NOT NULL, `max_buttons` INTEGER NOT NULL, `layout_preset` TEXT NOT NULL, `export_version` INTEGER NOT NULL, `original_author` TEXT, `download_url` TEXT, `tags` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `connection_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `computer_name` TEXT NOT NULL, `ip_address` TEXT NOT NULL, `port` INTEGER NOT NULL, `last_connected` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '23641611c885526fcf4b0fe4325f48fb')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `sound_buttons`");
        db.execSQL("DROP TABLE IF EXISTS `soundboard_layouts`");
        db.execSQL("DROP TABLE IF EXISTS `connection_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSoundButtons = new HashMap<String, TableInfo.Column>(11);
        _columnsSoundButtons.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("file_path", new TableInfo.Column("file_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("is_local_file", new TableInfo.Column("is_local_file", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("position_x", new TableInfo.Column("position_x", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("position_y", new TableInfo.Column("position_y", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("icon_name", new TableInfo.Column("icon_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("volume", new TableInfo.Column("volume", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundButtons.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSoundButtons = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSoundButtons = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSoundButtons = new TableInfo("sound_buttons", _columnsSoundButtons, _foreignKeysSoundButtons, _indicesSoundButtons);
        final TableInfo _existingSoundButtons = TableInfo.read(db, "sound_buttons");
        if (!_infoSoundButtons.equals(_existingSoundButtons)) {
          return new RoomOpenHelper.ValidationResult(false, "sound_buttons(com.soundboard.android.data.model.SoundButton).\n"
                  + " Expected:\n" + _infoSoundButtons + "\n"
                  + " Found:\n" + _existingSoundButtons);
        }
        final HashMap<String, TableInfo.Column> _columnsSoundboardLayouts = new HashMap<String, TableInfo.Column>(21);
        _columnsSoundboardLayouts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("grid_columns", new TableInfo.Column("grid_columns", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("grid_rows", new TableInfo.Column("grid_rows", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("is_template", new TableInfo.Column("is_template", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("template_category", new TableInfo.Column("template_category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("background_color", new TableInfo.Column("background_color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("accent_color", new TableInfo.Column("accent_color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("button_spacing", new TableInfo.Column("button_spacing", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("corner_radius", new TableInfo.Column("corner_radius", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("enable_glow_effect", new TableInfo.Column("enable_glow_effect", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("max_buttons", new TableInfo.Column("max_buttons", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("layout_preset", new TableInfo.Column("layout_preset", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("export_version", new TableInfo.Column("export_version", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("original_author", new TableInfo.Column("original_author", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("download_url", new TableInfo.Column("download_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSoundboardLayouts.put("tags", new TableInfo.Column("tags", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSoundboardLayouts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSoundboardLayouts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSoundboardLayouts = new TableInfo("soundboard_layouts", _columnsSoundboardLayouts, _foreignKeysSoundboardLayouts, _indicesSoundboardLayouts);
        final TableInfo _existingSoundboardLayouts = TableInfo.read(db, "soundboard_layouts");
        if (!_infoSoundboardLayouts.equals(_existingSoundboardLayouts)) {
          return new RoomOpenHelper.ValidationResult(false, "soundboard_layouts(com.soundboard.android.data.model.SoundboardLayout).\n"
                  + " Expected:\n" + _infoSoundboardLayouts + "\n"
                  + " Found:\n" + _existingSoundboardLayouts);
        }
        final HashMap<String, TableInfo.Column> _columnsConnectionHistory = new HashMap<String, TableInfo.Column>(6);
        _columnsConnectionHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionHistory.put("computer_name", new TableInfo.Column("computer_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionHistory.put("ip_address", new TableInfo.Column("ip_address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionHistory.put("port", new TableInfo.Column("port", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionHistory.put("last_connected", new TableInfo.Column("last_connected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionHistory.put("is_favorite", new TableInfo.Column("is_favorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConnectionHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConnectionHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoConnectionHistory = new TableInfo("connection_history", _columnsConnectionHistory, _foreignKeysConnectionHistory, _indicesConnectionHistory);
        final TableInfo _existingConnectionHistory = TableInfo.read(db, "connection_history");
        if (!_infoConnectionHistory.equals(_existingConnectionHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "connection_history(com.soundboard.android.data.model.ConnectionHistory).\n"
                  + " Expected:\n" + _infoConnectionHistory + "\n"
                  + " Found:\n" + _existingConnectionHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "23641611c885526fcf4b0fe4325f48fb", "cace9af1b19a46fab2d10be2b6c00849");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "sound_buttons","soundboard_layouts","connection_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `sound_buttons`");
      _db.execSQL("DELETE FROM `soundboard_layouts`");
      _db.execSQL("DELETE FROM `connection_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SoundButtonDao.class, SoundButtonDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SoundboardLayoutDao.class, SoundboardLayoutDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ConnectionHistoryDao.class, ConnectionHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SoundButtonDao soundButtonDao() {
    if (_soundButtonDao != null) {
      return _soundButtonDao;
    } else {
      synchronized(this) {
        if(_soundButtonDao == null) {
          _soundButtonDao = new SoundButtonDao_Impl(this);
        }
        return _soundButtonDao;
      }
    }
  }

  @Override
  public SoundboardLayoutDao soundboardLayoutDao() {
    if (_soundboardLayoutDao != null) {
      return _soundboardLayoutDao;
    } else {
      synchronized(this) {
        if(_soundboardLayoutDao == null) {
          _soundboardLayoutDao = new SoundboardLayoutDao_Impl(this);
        }
        return _soundboardLayoutDao;
      }
    }
  }

  @Override
  public ConnectionHistoryDao connectionHistoryDao() {
    if (_connectionHistoryDao != null) {
      return _connectionHistoryDao;
    } else {
      synchronized(this) {
        if(_connectionHistoryDao == null) {
          _connectionHistoryDao = new ConnectionHistoryDao_Impl(this);
        }
        return _connectionHistoryDao;
      }
    }
  }
}
