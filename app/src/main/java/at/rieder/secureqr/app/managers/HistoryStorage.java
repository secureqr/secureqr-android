package at.rieder.secureqr.app.managers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import at.rieder.secureqr.app.model.HistoryItem;

/**
 * Created by Thomas on 25.03.14.
 */
public class HistoryStorage extends SQLiteOpenHelper {

    private static final String TAG = HistoryStorage.class.getSimpleName();

    public static final String DATABASE_NAME = "secure_qr_history.db";
    public static final String TABLE_NAME = "history_items";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_HISTORY_ITEM = "item";

    private static final Integer DATABASE_VERSION = 1;

    private static final String CREATE_TABLE = "CREATE TABLE " +
            TABLE_NAME + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_HISTORY_ITEM + " text not null);";

    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME;

    private SQLiteDatabase database;

    public HistoryStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.database = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.i(TAG, "creating sqlitedatabase");
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        Log.w(TAG, "Upgrading database from version " + i + " to version " + i2);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public Collection<HistoryItem> getAllHistoryItems() {
        Cursor cursor = database.rawQuery(SELECT_ALL, null);
        Set<HistoryItem> itemSet = new HashSet<HistoryItem>();

        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = (HistoryItem) fromString(cursor.getString(1));
                itemSet.add(item);
            } while (cursor.moveToNext());
        }

        return itemSet;
    }

    public void addScanResultToDatabase(HistoryItem historyItem) {
        String base64Object = toString(historyItem);

        if (base64Object != null) {
            ContentValues values = new ContentValues();
            values.put(HistoryStorage.COLUMN_HISTORY_ITEM, toString(historyItem));
            database.insert(HistoryStorage.TABLE_NAME, null, values);
        } else {
            Log.w(TAG, "error serializing object");
        }
    }

    /**
     * Read the object from Base64 string.
     */
    private static Object fromString(String s) {
        try {
            byte[] data = Base64.decode(s, Base64.DEFAULT);
            ObjectInputStream ois = null;
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write the object to a Base64 string.
     */
    private static String toString(Serializable o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clear() {
        Log.w(TAG, "Deleting SQLiteDatabase");
        this.getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        this.getWritableDatabase().execSQL(CREATE_TABLE);
    }
}
