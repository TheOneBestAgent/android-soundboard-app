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
import com.soundboard.android.data.model.ConnectionHistory;
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
public final class ConnectionHistoryDao_Impl implements ConnectionHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConnectionHistory> __insertionAdapterOfConnectionHistory;

  private final EntityDeletionOrUpdateAdapter<ConnectionHistory> __deletionAdapterOfConnectionHistory;

  private final EntityDeletionOrUpdateAdapter<ConnectionHistory> __updateAdapterOfConnectionHistory;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastConnected;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFavoriteStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldConnections;

  public ConnectionHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConnectionHistory = new EntityInsertionAdapter<ConnectionHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `connection_history` (`id`,`computer_name`,`ip_address`,`port`,`last_connected`,`is_favorite`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConnectionHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getComputerName());
        statement.bindString(3, entity.getIpAddress());
        statement.bindLong(4, entity.getPort());
        statement.bindLong(5, entity.getLastConnected());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__deletionAdapterOfConnectionHistory = new EntityDeletionOrUpdateAdapter<ConnectionHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `connection_history` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConnectionHistory entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfConnectionHistory = new EntityDeletionOrUpdateAdapter<ConnectionHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `connection_history` SET `id` = ?,`computer_name` = ?,`ip_address` = ?,`port` = ?,`last_connected` = ?,`is_favorite` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConnectionHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getComputerName());
        statement.bindString(3, entity.getIpAddress());
        statement.bindLong(4, entity.getPort());
        statement.bindLong(5, entity.getLastConnected());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateLastConnected = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE connection_history SET last_connected = ? WHERE ip_address = ? AND port = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFavoriteStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE connection_history SET is_favorite = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldConnections = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM connection_history WHERE last_connected < ? AND is_favorite = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insertConnection(final ConnectionHistory connection,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfConnectionHistory.insertAndReturnId(connection);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteConnection(final ConnectionHistory connection,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfConnectionHistory.handle(connection);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateConnection(final ConnectionHistory connection,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfConnectionHistory.handle(connection);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastConnected(final String ipAddress, final int port, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastConnected.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, ipAddress);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, port);
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
          __preparedStmtOfUpdateLastConnected.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFavoriteStatus(final int id, final boolean isFavorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFavoriteStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isFavorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
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
          __preparedStmtOfUpdateFavoriteStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldConnections(final long cutoffTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldConnections.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
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
          __preparedStmtOfDeleteOldConnections.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ConnectionHistory>> getAllConnections() {
    final String _sql = "SELECT * FROM connection_history ORDER BY last_connected DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"connection_history"}, new Callable<List<ConnectionHistory>>() {
      @Override
      @NonNull
      public List<ConnectionHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfComputerName = CursorUtil.getColumnIndexOrThrow(_cursor, "computer_name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ip_address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfLastConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "last_connected");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final List<ConnectionHistory> _result = new ArrayList<ConnectionHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConnectionHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpComputerName;
            _tmpComputerName = _cursor.getString(_cursorIndexOfComputerName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final long _tmpLastConnected;
            _tmpLastConnected = _cursor.getLong(_cursorIndexOfLastConnected);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            _item = new ConnectionHistory(_tmpId,_tmpComputerName,_tmpIpAddress,_tmpPort,_tmpLastConnected,_tmpIsFavorite);
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
  public Flow<List<ConnectionHistory>> getFavoriteConnections() {
    final String _sql = "SELECT * FROM connection_history WHERE is_favorite = 1 ORDER BY last_connected DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"connection_history"}, new Callable<List<ConnectionHistory>>() {
      @Override
      @NonNull
      public List<ConnectionHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfComputerName = CursorUtil.getColumnIndexOrThrow(_cursor, "computer_name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ip_address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfLastConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "last_connected");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final List<ConnectionHistory> _result = new ArrayList<ConnectionHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConnectionHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpComputerName;
            _tmpComputerName = _cursor.getString(_cursorIndexOfComputerName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final long _tmpLastConnected;
            _tmpLastConnected = _cursor.getLong(_cursorIndexOfLastConnected);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            _item = new ConnectionHistory(_tmpId,_tmpComputerName,_tmpIpAddress,_tmpPort,_tmpLastConnected,_tmpIsFavorite);
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
  public Object getConnectionById(final int id,
      final Continuation<? super ConnectionHistory> $completion) {
    final String _sql = "SELECT * FROM connection_history WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ConnectionHistory>() {
      @Override
      @Nullable
      public ConnectionHistory call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfComputerName = CursorUtil.getColumnIndexOrThrow(_cursor, "computer_name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ip_address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfLastConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "last_connected");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final ConnectionHistory _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpComputerName;
            _tmpComputerName = _cursor.getString(_cursorIndexOfComputerName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final long _tmpLastConnected;
            _tmpLastConnected = _cursor.getLong(_cursorIndexOfLastConnected);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            _result = new ConnectionHistory(_tmpId,_tmpComputerName,_tmpIpAddress,_tmpPort,_tmpLastConnected,_tmpIsFavorite);
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
  public Object getConnectionByAddress(final String ipAddress, final int port,
      final Continuation<? super ConnectionHistory> $completion) {
    final String _sql = "SELECT * FROM connection_history WHERE ip_address = ? AND port = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, ipAddress);
    _argIndex = 2;
    _statement.bindLong(_argIndex, port);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ConnectionHistory>() {
      @Override
      @Nullable
      public ConnectionHistory call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfComputerName = CursorUtil.getColumnIndexOrThrow(_cursor, "computer_name");
          final int _cursorIndexOfIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "ip_address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfLastConnected = CursorUtil.getColumnIndexOrThrow(_cursor, "last_connected");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final ConnectionHistory _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpComputerName;
            _tmpComputerName = _cursor.getString(_cursorIndexOfComputerName);
            final String _tmpIpAddress;
            _tmpIpAddress = _cursor.getString(_cursorIndexOfIpAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final long _tmpLastConnected;
            _tmpLastConnected = _cursor.getLong(_cursorIndexOfLastConnected);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            _result = new ConnectionHistory(_tmpId,_tmpComputerName,_tmpIpAddress,_tmpPort,_tmpLastConnected,_tmpIsFavorite);
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
