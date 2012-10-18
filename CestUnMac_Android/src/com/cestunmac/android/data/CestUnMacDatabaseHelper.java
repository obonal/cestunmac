package com.cestunmac.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.cestunmac.android.Constants;

/**
 * Database Helper which supports the creation and the upgrade of the Qobuz database
 */
public class CestUnMacDatabaseHelper extends SQLiteOpenHelper implements Constants {

    public static final String  ID                                = "_id";
@Override
public String toString() {
    // TODO Auto-generated method stub
    return super.toString();
}@Override
public int hashCode() {
    // TODO Auto-generated method stub
    return super.hashCode();
}public static String getId() {
    return ID;
}
    // Posts Table
    private static final String POSTS_TABLE_CREATION_FIELDS       = ID + " INTEGER PRIMARY KEY," + Post.FIELD_POST_ID + " INTEGER," + Post.FIELD_DATE
                                                                    + " INTEGER," + Post.FIELD_TITLE + " TEXT," + Category.FIELD_CATEGORYID + " INTEGER,"
                                                                    + Post.FIELD_URL + " TEXT,"
                                                                    + Attachment.FIELD_ATTACHMENTID + " INTEGER";

    private static final String POSTS_TABLE_CREATION_PARAMS       = " (" + POSTS_TABLE_CREATION_FIELDS + ");";

    // Categories Table
    private static final String CATEGORIES_TABLE_CREATION_FIELDS  = ID + " INTEGER PRIMARY KEY," + Category.FIELD_CATEGORYID + " INTEGER,"
                                                                    + Category.FIELD_TITLE + " TEXT";

    private static final String CATEGORIES_TABLE_CREATION_PARAMS  = " (" + CATEGORIES_TABLE_CREATION_FIELDS + ");";

    // Attachments Table
    private static final String ATTACHMENTS_TABLE_CREATION_FIELDS = ID + " INTEGER PRIMARY KEY," + Attachment.FIELD_ATTACHMENTID + " INTEGER,"
                                                                    + Attachment.FIELD_FULLURLSTRING + " TEXT," + Attachment.FIELD_THUMBNAILURLSTRING + " TEXT";

    private static final String ATTACHMENTS_TABLE_CREATION_PARAMS = " (" + ATTACHMENTS_TABLE_CREATION_FIELDS + ");";

    public CestUnMacDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(Constants.LOG_TAG, "*** Database Helper: Create tables");
        // Posts
        createTable(db, Post.DB_TABLE_NAME, POSTS_TABLE_CREATION_PARAMS, new String[] { Post.FIELD_POST_ID });
        // Categories
        createTable(db, Category.DB_TABLE_NAME, CATEGORIES_TABLE_CREATION_PARAMS, new String[] { Category.FIELD_CATEGORYID });
        // Attachments
        createTable(db, Attachment.DB_TABLE_NAME, ATTACHMENTS_TABLE_CREATION_PARAMS, new String[] { Attachment.FIELD_ATTACHMENTID });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "*** Database Helper: Upgrade database from version " + oldVersion + " to version " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + Post.DB_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Category.DB_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Attachment.DB_TABLE_NAME);
        onCreate(db);
    }

    public void createTable(SQLiteDatabase db, String tableName, String tableCreationParams, String[] indexColumns) {
        Log.i(LOG_TAG, "*** Database Helper: createTable " + tableName + " ***");
        db.execSQL("CREATE TABLE " + tableName + tableCreationParams);
        for (int i = 0; i < indexColumns.length; i++) {
            String indexColumn = indexColumns[i];
            Log.i(LOG_TAG, "      -> create index for " + indexColumn);
            db.execSQL("CREATE INDEX " + tableName + "_index ON " + tableName + " (" + indexColumn + ");");
        }

    }
}
