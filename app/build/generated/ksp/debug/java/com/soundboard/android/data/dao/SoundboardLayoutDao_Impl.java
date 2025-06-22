package com.soundboard.android.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.soundboard.android.data.database.SoundboardTypeConverters;
import com.soundboard.android.data.model.LayoutPreset;
import com.soundboard.android.data.model.SoundboardLayout;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SoundboardLayoutDao_Impl implements SoundboardLayoutDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SoundboardLayout> __insertionAdapterOfSoundboardLayout;

  private final SoundboardTypeConverters __soundboardTypeConverters = new SoundboardTypeConverters();

  private final EntityDeletionOrUpdateAdapter<SoundboardLayout> __deletionAdapterOfSoundboardLayout;

  private final EntityDeletionOrUpdateAdapter<SoundboardLayout> __updateAdapterOfSoundboardLayout;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllLayouts;

  private final SharedSQLiteStatement __preparedStmtOfDeactivateAllLayouts;

  private final SharedSQLiteStatement __preparedStmtOfSetActiveLayout;

  public SoundboardLayoutDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSoundboardLayout = new EntityInsertionAdapter<SoundboardLayout>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `soundboard_layouts` (`id`,`name`,`description`,`is_active`,`grid_columns`,`grid_rows`,`created_at`,`updated_at`,`is_template`,`template_category`,`background_color`,`accent_color`,`button_spacing`,`corner_radius`,`enable_glow_effect`,`max_buttons`,`layout_preset`,`export_version`,`original_author`,`download_url`,`tags`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundboardLayout entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getGridColumns());
        statement.bindLong(6, entity.getGridRows());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getUpdatedAt());
        final int _tmp_1 = entity.isTemplate() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        if (entity.getTemplateCategory() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTemplateCategory());
        }
        statement.bindString(11, entity.getBackgroundColor());
        statement.bindString(12, entity.getAccentColor());
        statement.bindDouble(13, entity.getButtonSpacing());
        statement.bindDouble(14, entity.getCornerRadius());
        final int _tmp_2 = entity.getEnableGlowEffect() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        statement.bindLong(16, entity.getMaxButtons());
        final String _tmp_3 = __soundboardTypeConverters.fromLayoutPreset(entity.getLayoutPreset());
        statement.bindString(17, _tmp_3);
        statement.bindLong(18, entity.getExportVersion());
        if (entity.getOriginalAuthor() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getOriginalAuthor());
        }
        if (entity.getDownloadUrl() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getDownloadUrl());
        }
        if (entity.getTags() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getTags());
        }
      }
    };
    this.__deletionAdapterOfSoundboardLayout = new EntityDeletionOrUpdateAdapter<SoundboardLayout>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `soundboard_layouts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundboardLayout entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSoundboardLayout = new EntityDeletionOrUpdateAdapter<SoundboardLayout>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `soundboard_layouts` SET `id` = ?,`name` = ?,`description` = ?,`is_active` = ?,`grid_columns` = ?,`grid_rows` = ?,`created_at` = ?,`updated_at` = ?,`is_template` = ?,`template_category` = ?,`background_color` = ?,`accent_color` = ?,`button_spacing` = ?,`corner_radius` = ?,`enable_glow_effect` = ?,`max_buttons` = ?,`layout_preset` = ?,`export_version` = ?,`original_author` = ?,`download_url` = ?,`tags` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundboardLayout entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getGridColumns());
        statement.bindLong(6, entity.getGridRows());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getUpdatedAt());
        final int _tmp_1 = entity.isTemplate() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        if (entity.getTemplateCategory() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTemplateCategory());
        }
        statement.bindString(11, entity.getBackgroundColor());
        statement.bindString(12, entity.getAccentColor());
        statement.bindDouble(13, entity.getButtonSpacing());
        statement.bindDouble(14, entity.getCornerRadius());
        final int _tmp_2 = entity.getEnableGlowEffect() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        statement.bindLong(16, entity.getMaxButtons());
        final String _tmp_3 = __soundboardTypeConverters.fromLayoutPreset(entity.getLayoutPreset());
        statement.bindString(17, _tmp_3);
        statement.bindLong(18, entity.getExportVersion());
        if (entity.getOriginalAuthor() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getOriginalAuthor());
        }
        if (entity.getDownloadUrl() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getDownloadUrl());
        }
        if (entity.getTags() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getTags());
        }
        statement.bindLong(22, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllLayouts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM soundboard_layouts";
        return _query;
      }
    };
    this.__preparedStmtOfDeactivateAllLayouts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE soundboard_layouts SET is_active = 0";
        return _query;
      }
    };
    this.__preparedStmtOfSetActiveLayout = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE soundboard_layouts SET is_active = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLayout(final SoundboardLayout layout,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSoundboardLayout.insertAndReturnId(layout);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLayout(final SoundboardLayout layout,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSoundboardLayout.handle(layout);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLayout(final SoundboardLayout layout,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSoundboardLayout.handle(layout);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object switchActiveLayout(final long id, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> SoundboardLayoutDao.DefaultImpls.switchActiveLayout(SoundboardLayoutDao_Impl.this, id, __cont), $completion);
  }

  @Override
  public Object deleteAllLayouts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllLayouts.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllLayouts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deactivateAllLayouts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeactivateAllLayouts.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeactivateAllLayouts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setActiveLayout(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetActiveLayout.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetActiveLayout.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SoundboardLayout>> getAllLayouts() {
    final String _sql = "SELECT * FROM soundboard_layouts ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"soundboard_layouts"}, new Callable<List<SoundboardLayout>>() {
      @Override
      @NonNull
      public List<SoundboardLayout> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfGridColumns = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_columns");
          final int _cursorIndexOfGridRows = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_rows");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsTemplate = CursorUtil.getColumnIndexOrThrow(_cursor, "is_template");
          final int _cursorIndexOfTemplateCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "template_category");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accent_color");
          final int _cursorIndexOfButtonSpacing = CursorUtil.getColumnIndexOrThrow(_cursor, "button_spacing");
          final int _cursorIndexOfCornerRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "corner_radius");
          final int _cursorIndexOfEnableGlowEffect = CursorUtil.getColumnIndexOrThrow(_cursor, "enable_glow_effect");
          final int _cursorIndexOfMaxButtons = CursorUtil.getColumnIndexOrThrow(_cursor, "max_buttons");
          final int _cursorIndexOfLayoutPreset = CursorUtil.getColumnIndexOrThrow(_cursor, "layout_preset");
          final int _cursorIndexOfExportVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "export_version");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "original_author");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "download_url");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<SoundboardLayout> _result = new ArrayList<SoundboardLayout>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SoundboardLayout _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpGridColumns;
            _tmpGridColumns = _cursor.getInt(_cursorIndexOfGridColumns);
            final int _tmpGridRows;
            _tmpGridRows = _cursor.getInt(_cursorIndexOfGridRows);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsTemplate;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTemplate);
            _tmpIsTemplate = _tmp_1 != 0;
            final String _tmpTemplateCategory;
            if (_cursor.isNull(_cursorIndexOfTemplateCategory)) {
              _tmpTemplateCategory = null;
            } else {
              _tmpTemplateCategory = _cursor.getString(_cursorIndexOfTemplateCategory);
            }
            final String _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            final String _tmpAccentColor;
            _tmpAccentColor = _cursor.getString(_cursorIndexOfAccentColor);
            final float _tmpButtonSpacing;
            _tmpButtonSpacing = _cursor.getFloat(_cursorIndexOfButtonSpacing);
            final float _tmpCornerRadius;
            _tmpCornerRadius = _cursor.getFloat(_cursorIndexOfCornerRadius);
            final boolean _tmpEnableGlowEffect;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfEnableGlowEffect);
            _tmpEnableGlowEffect = _tmp_2 != 0;
            final int _tmpMaxButtons;
            _tmpMaxButtons = _cursor.getInt(_cursorIndexOfMaxButtons);
            final LayoutPreset _tmpLayoutPreset;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfLayoutPreset);
            _tmpLayoutPreset = __soundboardTypeConverters.toLayoutPreset(_tmp_3);
            final int _tmpExportVersion;
            _tmpExportVersion = _cursor.getInt(_cursorIndexOfExportVersion);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final String _tmpDownloadUrl;
            if (_cursor.isNull(_cursorIndexOfDownloadUrl)) {
              _tmpDownloadUrl = null;
            } else {
              _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _item = new SoundboardLayout(_tmpId,_tmpName,_tmpDescription,_tmpIsActive,_tmpGridColumns,_tmpGridRows,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsTemplate,_tmpTemplateCategory,_tmpBackgroundColor,_tmpAccentColor,_tmpButtonSpacing,_tmpCornerRadius,_tmpEnableGlowEffect,_tmpMaxButtons,_tmpLayoutPreset,_tmpExportVersion,_tmpOriginalAuthor,_tmpDownloadUrl,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getActiveLayout(final Continuation<? super SoundboardLayout> $completion) {
    final String _sql = "SELECT * FROM soundboard_layouts WHERE is_active = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SoundboardLayout>() {
      @Override
      @Nullable
      public SoundboardLayout call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfGridColumns = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_columns");
          final int _cursorIndexOfGridRows = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_rows");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsTemplate = CursorUtil.getColumnIndexOrThrow(_cursor, "is_template");
          final int _cursorIndexOfTemplateCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "template_category");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accent_color");
          final int _cursorIndexOfButtonSpacing = CursorUtil.getColumnIndexOrThrow(_cursor, "button_spacing");
          final int _cursorIndexOfCornerRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "corner_radius");
          final int _cursorIndexOfEnableGlowEffect = CursorUtil.getColumnIndexOrThrow(_cursor, "enable_glow_effect");
          final int _cursorIndexOfMaxButtons = CursorUtil.getColumnIndexOrThrow(_cursor, "max_buttons");
          final int _cursorIndexOfLayoutPreset = CursorUtil.getColumnIndexOrThrow(_cursor, "layout_preset");
          final int _cursorIndexOfExportVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "export_version");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "original_author");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "download_url");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final SoundboardLayout _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpGridColumns;
            _tmpGridColumns = _cursor.getInt(_cursorIndexOfGridColumns);
            final int _tmpGridRows;
            _tmpGridRows = _cursor.getInt(_cursorIndexOfGridRows);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsTemplate;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTemplate);
            _tmpIsTemplate = _tmp_1 != 0;
            final String _tmpTemplateCategory;
            if (_cursor.isNull(_cursorIndexOfTemplateCategory)) {
              _tmpTemplateCategory = null;
            } else {
              _tmpTemplateCategory = _cursor.getString(_cursorIndexOfTemplateCategory);
            }
            final String _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            final String _tmpAccentColor;
            _tmpAccentColor = _cursor.getString(_cursorIndexOfAccentColor);
            final float _tmpButtonSpacing;
            _tmpButtonSpacing = _cursor.getFloat(_cursorIndexOfButtonSpacing);
            final float _tmpCornerRadius;
            _tmpCornerRadius = _cursor.getFloat(_cursorIndexOfCornerRadius);
            final boolean _tmpEnableGlowEffect;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfEnableGlowEffect);
            _tmpEnableGlowEffect = _tmp_2 != 0;
            final int _tmpMaxButtons;
            _tmpMaxButtons = _cursor.getInt(_cursorIndexOfMaxButtons);
            final LayoutPreset _tmpLayoutPreset;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfLayoutPreset);
            _tmpLayoutPreset = __soundboardTypeConverters.toLayoutPreset(_tmp_3);
            final int _tmpExportVersion;
            _tmpExportVersion = _cursor.getInt(_cursorIndexOfExportVersion);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final String _tmpDownloadUrl;
            if (_cursor.isNull(_cursorIndexOfDownloadUrl)) {
              _tmpDownloadUrl = null;
            } else {
              _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _result = new SoundboardLayout(_tmpId,_tmpName,_tmpDescription,_tmpIsActive,_tmpGridColumns,_tmpGridRows,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsTemplate,_tmpTemplateCategory,_tmpBackgroundColor,_tmpAccentColor,_tmpButtonSpacing,_tmpCornerRadius,_tmpEnableGlowEffect,_tmpMaxButtons,_tmpLayoutPreset,_tmpExportVersion,_tmpOriginalAuthor,_tmpDownloadUrl,_tmpTags);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLayoutById(final int id,
      final Continuation<? super SoundboardLayout> $completion) {
    final String _sql = "SELECT * FROM soundboard_layouts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SoundboardLayout>() {
      @Override
      @Nullable
      public SoundboardLayout call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfGridColumns = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_columns");
          final int _cursorIndexOfGridRows = CursorUtil.getColumnIndexOrThrow(_cursor, "grid_rows");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsTemplate = CursorUtil.getColumnIndexOrThrow(_cursor, "is_template");
          final int _cursorIndexOfTemplateCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "template_category");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "background_color");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accent_color");
          final int _cursorIndexOfButtonSpacing = CursorUtil.getColumnIndexOrThrow(_cursor, "button_spacing");
          final int _cursorIndexOfCornerRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "corner_radius");
          final int _cursorIndexOfEnableGlowEffect = CursorUtil.getColumnIndexOrThrow(_cursor, "enable_glow_effect");
          final int _cursorIndexOfMaxButtons = CursorUtil.getColumnIndexOrThrow(_cursor, "max_buttons");
          final int _cursorIndexOfLayoutPreset = CursorUtil.getColumnIndexOrThrow(_cursor, "layout_preset");
          final int _cursorIndexOfExportVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "export_version");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "original_author");
          final int _cursorIndexOfDownloadUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "download_url");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final SoundboardLayout _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpGridColumns;
            _tmpGridColumns = _cursor.getInt(_cursorIndexOfGridColumns);
            final int _tmpGridRows;
            _tmpGridRows = _cursor.getInt(_cursorIndexOfGridRows);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpIsTemplate;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTemplate);
            _tmpIsTemplate = _tmp_1 != 0;
            final String _tmpTemplateCategory;
            if (_cursor.isNull(_cursorIndexOfTemplateCategory)) {
              _tmpTemplateCategory = null;
            } else {
              _tmpTemplateCategory = _cursor.getString(_cursorIndexOfTemplateCategory);
            }
            final String _tmpBackgroundColor;
            _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            final String _tmpAccentColor;
            _tmpAccentColor = _cursor.getString(_cursorIndexOfAccentColor);
            final float _tmpButtonSpacing;
            _tmpButtonSpacing = _cursor.getFloat(_cursorIndexOfButtonSpacing);
            final float _tmpCornerRadius;
            _tmpCornerRadius = _cursor.getFloat(_cursorIndexOfCornerRadius);
            final boolean _tmpEnableGlowEffect;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfEnableGlowEffect);
            _tmpEnableGlowEffect = _tmp_2 != 0;
            final int _tmpMaxButtons;
            _tmpMaxButtons = _cursor.getInt(_cursorIndexOfMaxButtons);
            final LayoutPreset _tmpLayoutPreset;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfLayoutPreset);
            _tmpLayoutPreset = __soundboardTypeConverters.toLayoutPreset(_tmp_3);
            final int _tmpExportVersion;
            _tmpExportVersion = _cursor.getInt(_cursorIndexOfExportVersion);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final String _tmpDownloadUrl;
            if (_cursor.isNull(_cursorIndexOfDownloadUrl)) {
              _tmpDownloadUrl = null;
            } else {
              _tmpDownloadUrl = _cursor.getString(_cursorIndexOfDownloadUrl);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            _result = new SoundboardLayout(_tmpId,_tmpName,_tmpDescription,_tmpIsActive,_tmpGridColumns,_tmpGridRows,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsTemplate,_tmpTemplateCategory,_tmpBackgroundColor,_tmpAccentColor,_tmpButtonSpacing,_tmpCornerRadius,_tmpEnableGlowEffect,_tmpMaxButtons,_tmpLayoutPreset,_tmpExportVersion,_tmpOriginalAuthor,_tmpDownloadUrl,_tmpTags);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
