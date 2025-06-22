package com.soundboard.android.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.soundboard.android.data.model.SoundButton;
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
public final class SoundButtonDao_Impl implements SoundButtonDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SoundButton> __insertionAdapterOfSoundButton;

  private final EntityDeletionOrUpdateAdapter<SoundButton> __deletionAdapterOfSoundButton;

  private final EntityDeletionOrUpdateAdapter<SoundButton> __updateAdapterOfSoundButton;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSoundButtonById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSoundButtons;

  public SoundButtonDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSoundButton = new EntityInsertionAdapter<SoundButton>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sound_buttons` (`id`,`name`,`file_path`,`is_local_file`,`position_x`,`position_y`,`color`,`icon_name`,`volume`,`created_at`,`updated_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundButton entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFilePath());
        final int _tmp = entity.isLocalFile() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getPositionX());
        statement.bindLong(6, entity.getPositionY());
        statement.bindString(7, entity.getColor());
        if (entity.getIconName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getIconName());
        }
        statement.bindDouble(9, entity.getVolume());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfSoundButton = new EntityDeletionOrUpdateAdapter<SoundButton>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sound_buttons` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundButton entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSoundButton = new EntityDeletionOrUpdateAdapter<SoundButton>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sound_buttons` SET `id` = ?,`name` = ?,`file_path` = ?,`is_local_file` = ?,`position_x` = ?,`position_y` = ?,`color` = ?,`icon_name` = ?,`volume` = ?,`created_at` = ?,`updated_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SoundButton entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFilePath());
        final int _tmp = entity.isLocalFile() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getPositionX());
        statement.bindLong(6, entity.getPositionY());
        statement.bindString(7, entity.getColor());
        if (entity.getIconName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getIconName());
        }
        statement.bindDouble(9, entity.getVolume());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSoundButtonById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sound_buttons WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllSoundButtons = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sound_buttons";
        return _query;
      }
    };
  }

  @Override
  public Object insertSoundButton(final SoundButton soundButton,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSoundButton.insertAndReturnId(soundButton);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSoundButton(final SoundButton soundButton,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSoundButton.handle(soundButton);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSoundButton(final SoundButton soundButton,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSoundButton.handle(soundButton);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSoundButtonById(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSoundButtonById.acquire();
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
          __preparedStmtOfDeleteSoundButtonById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllSoundButtons(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSoundButtons.acquire();
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
          __preparedStmtOfDeleteAllSoundButtons.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SoundButton>> getAllSoundButtons() {
    final String _sql = "SELECT * FROM sound_buttons ORDER BY position_y, position_x";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sound_buttons"}, new Callable<List<SoundButton>>() {
      @Override
      @NonNull
      public List<SoundButton> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "file_path");
          final int _cursorIndexOfIsLocalFile = CursorUtil.getColumnIndexOrThrow(_cursor, "is_local_file");
          final int _cursorIndexOfPositionX = CursorUtil.getColumnIndexOrThrow(_cursor, "position_x");
          final int _cursorIndexOfPositionY = CursorUtil.getColumnIndexOrThrow(_cursor, "position_y");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIconName = CursorUtil.getColumnIndexOrThrow(_cursor, "icon_name");
          final int _cursorIndexOfVolume = CursorUtil.getColumnIndexOrThrow(_cursor, "volume");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<SoundButton> _result = new ArrayList<SoundButton>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SoundButton _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final boolean _tmpIsLocalFile;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocalFile);
            _tmpIsLocalFile = _tmp != 0;
            final int _tmpPositionX;
            _tmpPositionX = _cursor.getInt(_cursorIndexOfPositionX);
            final int _tmpPositionY;
            _tmpPositionY = _cursor.getInt(_cursorIndexOfPositionY);
            final String _tmpColor;
            _tmpColor = _cursor.getString(_cursorIndexOfColor);
            final String _tmpIconName;
            if (_cursor.isNull(_cursorIndexOfIconName)) {
              _tmpIconName = null;
            } else {
              _tmpIconName = _cursor.getString(_cursorIndexOfIconName);
            }
            final float _tmpVolume;
            _tmpVolume = _cursor.getFloat(_cursorIndexOfVolume);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new SoundButton(_tmpId,_tmpName,_tmpFilePath,_tmpIsLocalFile,_tmpPositionX,_tmpPositionY,_tmpColor,_tmpIconName,_tmpVolume,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getSoundButtonById(final int id,
      final Continuation<? super SoundButton> $completion) {
    final String _sql = "SELECT * FROM sound_buttons WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SoundButton>() {
      @Override
      @Nullable
      public SoundButton call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "file_path");
          final int _cursorIndexOfIsLocalFile = CursorUtil.getColumnIndexOrThrow(_cursor, "is_local_file");
          final int _cursorIndexOfPositionX = CursorUtil.getColumnIndexOrThrow(_cursor, "position_x");
          final int _cursorIndexOfPositionY = CursorUtil.getColumnIndexOrThrow(_cursor, "position_y");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIconName = CursorUtil.getColumnIndexOrThrow(_cursor, "icon_name");
          final int _cursorIndexOfVolume = CursorUtil.getColumnIndexOrThrow(_cursor, "volume");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final SoundButton _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final boolean _tmpIsLocalFile;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocalFile);
            _tmpIsLocalFile = _tmp != 0;
            final int _tmpPositionX;
            _tmpPositionX = _cursor.getInt(_cursorIndexOfPositionX);
            final int _tmpPositionY;
            _tmpPositionY = _cursor.getInt(_cursorIndexOfPositionY);
            final String _tmpColor;
            _tmpColor = _cursor.getString(_cursorIndexOfColor);
            final String _tmpIconName;
            if (_cursor.isNull(_cursorIndexOfIconName)) {
              _tmpIconName = null;
            } else {
              _tmpIconName = _cursor.getString(_cursorIndexOfIconName);
            }
            final float _tmpVolume;
            _tmpVolume = _cursor.getFloat(_cursorIndexOfVolume);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SoundButton(_tmpId,_tmpName,_tmpFilePath,_tmpIsLocalFile,_tmpPositionX,_tmpPositionY,_tmpColor,_tmpIconName,_tmpVolume,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getSoundButtonByPosition(final int x, final int y,
      final Continuation<? super SoundButton> $completion) {
    final String _sql = "SELECT * FROM sound_buttons WHERE position_x = ? AND position_y = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, x);
    _argIndex = 2;
    _statement.bindLong(_argIndex, y);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SoundButton>() {
      @Override
      @Nullable
      public SoundButton call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "file_path");
          final int _cursorIndexOfIsLocalFile = CursorUtil.getColumnIndexOrThrow(_cursor, "is_local_file");
          final int _cursorIndexOfPositionX = CursorUtil.getColumnIndexOrThrow(_cursor, "position_x");
          final int _cursorIndexOfPositionY = CursorUtil.getColumnIndexOrThrow(_cursor, "position_y");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIconName = CursorUtil.getColumnIndexOrThrow(_cursor, "icon_name");
          final int _cursorIndexOfVolume = CursorUtil.getColumnIndexOrThrow(_cursor, "volume");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final SoundButton _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final boolean _tmpIsLocalFile;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLocalFile);
            _tmpIsLocalFile = _tmp != 0;
            final int _tmpPositionX;
            _tmpPositionX = _cursor.getInt(_cursorIndexOfPositionX);
            final int _tmpPositionY;
            _tmpPositionY = _cursor.getInt(_cursorIndexOfPositionY);
            final String _tmpColor;
            _tmpColor = _cursor.getString(_cursorIndexOfColor);
            final String _tmpIconName;
            if (_cursor.isNull(_cursorIndexOfIconName)) {
              _tmpIconName = null;
            } else {
              _tmpIconName = _cursor.getString(_cursorIndexOfIconName);
            }
            final float _tmpVolume;
            _tmpVolume = _cursor.getFloat(_cursorIndexOfVolume);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new SoundButton(_tmpId,_tmpName,_tmpFilePath,_tmpIsLocalFile,_tmpPositionX,_tmpPositionY,_tmpColor,_tmpIconName,_tmpVolume,_tmpCreatedAt,_tmpUpdatedAt);
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
