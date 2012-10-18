package com.cestunmac.android.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.cestunmac.android.Constants;

/**
 * Content provider for data used by the Qobuz application
 */
public class CestUnMacContentProvider extends ContentProvider implements Constants {
    protected SQLiteDatabase    mDataBase;

    protected static final int  POSTS_ID_ONLY             = 0;
    protected static final int  POSTS_FULL_TABLE          = 1;
    protected static final int  CATEGORIES_ID_ONLY        = 3;
    protected static final int  CATEGORIES_FULL_TABLE     = 4;
    protected static final int  ATTACHMENTS_ID_ONLY       = 5;
    protected static final int  ATTACHMENTS_FULL_TABLE    = 6;

    private static final String DEFAULT_SORT_ORDER        = CestUnMacDatabaseHelper.ID;

    private static final String URI_INFO_MATCH            = "URI_INFO_MATCH";
    private static final String URI_INFO_ID_ONLY          = "URI_INFO_ID_ONLY";
    private static final String URI_INFO_TABLE_NAME       = "URI_INFO_TABLE_NAME";
    private static final String URI_INFO_DEFAULT_ORDER_BY = "URI_INFO_DEFAULT_ORDER_BY";
    private static final String URI_INFO_NULL_COLUMN_HACK = "URI_INFO_NULL_COLUMN_HACK";
    private static final String URI_INFO_ID_FIELD         = "URI_INFO_ID_FIELD";

    protected static UriMatcher mUriMatcher               = new UriMatcher(UriMatcher.NO_MATCH);

    private static boolean      warnListeners             = true;

    static {
        // ------------- Posts
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, POSTS_URI_SUFFIX + "/#", POSTS_ID_ONLY);
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, POSTS_URI_SUFFIX, POSTS_FULL_TABLE);

        // ------------- Categories
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, CATEGORIES_URI_SUFFIX + "/#", CATEGORIES_ID_ONLY);
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, CATEGORIES_URI_SUFFIX, CATEGORIES_FULL_TABLE);

        // ------------- Attachments
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, ATTACHMENTS_URI_SUFFIX + "/#", ATTACHMENTS_ID_ONLY);
        mUriMatcher.addURI(DB_CONTENTPROVIDER_AUTHORITY, ATTACHMENTS_URI_SUFFIX, ATTACHMENTS_FULL_TABLE);
    }

    /**
     * Initializes the parameters corresponding to the given Uri (e.g. the corresponding table name, sort order, ...)
     * 
     * @param uri
     *            Uri
     */
    private Bundle getInfoFromUri(Uri uri) {
        Bundle bundle = getInfoFromMatch(mUriMatcher.match(uri), uri);
        if (bundle != null) {
            return bundle;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /**
     * Initializes the parameters corresponding to the given Uri match (e.g. the corresponding table name, sort order, ...)
     * 
     * @param match
     *            match returned by {@link UriMatcher.match(Uri)}
     * @param uri
     *            The uri if available
     */
    private Bundle getInfoFromMatch(int match, Uri uri) {
        Bundle result = new Bundle();
        boolean id_only = false;
        String table_name = null;
        String default_order_by = DEFAULT_SORT_ORDER;
        String null_column_hack = null;
        String id_field = CestUnMacDatabaseHelper.ID;

        switch (match) {
            case POSTS_ID_ONLY:
                id_only = true;
            case POSTS_FULL_TABLE:
                table_name = Post.DB_TABLE_NAME;
                default_order_by = Post.FIELD_DATE + " DESC";
                null_column_hack = Post.FIELD_TITLE;
                break;
            case CATEGORIES_ID_ONLY:
                id_only = true;
            case CATEGORIES_FULL_TABLE:
                table_name = Category.DB_TABLE_NAME;
                default_order_by = Category.FIELD_TITLE;
                null_column_hack = Category.FIELD_TITLE;
                break;
            case ATTACHMENTS_ID_ONLY:
                id_only = true;
            case ATTACHMENTS_FULL_TABLE:
                table_name = Attachment.DB_TABLE_NAME;
                null_column_hack = Attachment.FIELD_ATTACHMENTID;
                break;
            default:
                return null;
        }

        result.putInt(URI_INFO_MATCH, match);
        result.putBoolean(URI_INFO_ID_ONLY, id_only);
        result.putString(URI_INFO_TABLE_NAME, table_name);
        result.putString(URI_INFO_DEFAULT_ORDER_BY, default_order_by);
        result.putString(URI_INFO_NULL_COLUMN_HACK, null_column_hack);
        result.putString(URI_INFO_ID_FIELD, id_field);

        return result;
    }

    @Override
    public boolean onCreate() {
        CestUnMacDatabaseHelper databaseHelper = new CestUnMacDatabaseHelper(getContext());
        mDataBase = databaseHelper.getWritableDatabase();
        return (mDataBase != null);
    }

    public HashMap<String, String> getProjectionMapByMatch(int match) {
        switch (match) {
            case POSTS_ID_ONLY:
            case POSTS_FULL_TABLE:
            case CATEGORIES_ID_ONLY:
            case CATEGORIES_FULL_TABLE:
            case ATTACHMENTS_ID_ONLY:
            case ATTACHMENTS_FULL_TABLE:
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortOrder) {
        // C4MLog.i(Constants.LOG_TAG, "Query on uri: " + uri + ((where != null) ? " WHERE " + where : ""));
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Bundle info = getInfoFromUri(uri);
        int match = info.getInt(URI_INFO_MATCH);
        String orderBy;

        boolean id_only = info.getBoolean(URI_INFO_ID_ONLY);
        String table_name = info.getString(URI_INFO_TABLE_NAME);
        String default_order_by = info.getString(URI_INFO_DEFAULT_ORDER_BY);
        String id_field = info.getString(URI_INFO_ID_FIELD);

        queryBuilder.setTables(table_name);
        // Set the correct projection map
        queryBuilder.setProjectionMap(getProjectionMapByMatch(match));

        if (id_only) {
            queryBuilder.appendWhere(id_field + "=?");
            if (whereArgs == null) {
                whereArgs = new String[] { uri.getPathSegments().get(1) };
            } else {
                String[] new_args = new String[whereArgs.length + 1];
                System.arraycopy(whereArgs, 0, new_args, 0, whereArgs.length);
                new_args[whereArgs.length] = uri.getPathSegments().get(1);
            }
        }

        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = default_order_by;
        } else {
            orderBy = sortOrder;
        }
        
        //Log.w(Constants.LOG_TAG, "Query on uri: " + uri + ((where != null) ? " WHERE " + where : "") + " ORDER BY: " + orderBy);
        Cursor cursor = queryBuilder.query(mDataBase, projection, where, whereArgs, null, null, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // C4MLog.i(Constants.LOG_TAG, "Insert on uri: " + uri);
        Bundle info = getInfoFromUri(uri);
        boolean id_only = info.getBoolean(URI_INFO_ID_ONLY);
        String table_name = info.getString(URI_INFO_TABLE_NAME);
        String null_column_hack = info.getString(URI_INFO_NULL_COLUMN_HACK);

        if (id_only) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        long rowId = mDataBase.insert(table_name, null_column_hack, values);

        if (rowId != -1) {
            Uri insertedUri = ContentUris.withAppendedId(uri, rowId);
            if (isWarnListeners()) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return insertedUri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        // C4MLog.i(Constants.LOG_TAG, "Update on uri: " + uri + " WHERE " + where);
        Bundle info = getInfoFromUri(uri);
        boolean id_only = info.getBoolean(URI_INFO_ID_ONLY);
        String table_name = info.getString(URI_INFO_TABLE_NAME);
        String id_field = info.getString(URI_INFO_ID_FIELD);

        int count;

        if (id_only) {
            String id = uri.getPathSegments().get(1);
            count = mDataBase.update(table_name, values, id_field + "=" + id + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
        } else {
            count = mDataBase.update(table_name, values, where, whereArgs);
        }

        if (count > 0 && isWarnListeners()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Bundle info = getInfoFromUri(uri);
        boolean id_only = info.getBoolean(URI_INFO_ID_ONLY);
        String table_name = info.getString(URI_INFO_TABLE_NAME);
        String id_field = info.getString(URI_INFO_ID_FIELD);

        int count;
        if (id_only) {
            String id = uri.getPathSegments().get(1);
            count = mDataBase.delete(table_name, id_field + "=" + id + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
        } else {
            count = mDataBase.delete(table_name, where, whereArgs);
        }

        if (count > 0 && isWarnListeners()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // C4MLog.i(Constants.LOG_TAG, "Get type of uri: " + uri);
        Bundle info = getInfoFromUri(uri);
        boolean id_only = info.getBoolean(URI_INFO_ID_ONLY);
        String table_name = info.getString(URI_INFO_TABLE_NAME);

        if (id_only) {
            return CONTENT_PROVIDER_SINGLE_ITEM_MIME_TYPE_PREFIX + "." + table_name;
        } else {
            return CONTENT_PROVIDER_MULTIPLE_ITEMS_MIME_TYPE_PREFIX + "." + table_name;
        }
    }

    /**
     * Returns true if the record specified by the given id exists in the given content provider
     * 
     * @param context
     *            Context
     * @param contentUri
     *            Content Uri
     * @param id
     *            Id of the record
     * @return
     */
    public static boolean recordExists(Context context, Uri contentUri, int id) {
        Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(contentUri, id), new String[] { CestUnMacDatabaseHelper.ID }, null, null,
                                                           null);
        if (cursor == null) {
            return false;
        } else {
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            } else {
                cursor.close();
                return true;
            }
        }
    }

    /**
     * Returns true if the record specified by the given id exists in the given content provider
     * 
     * @param context
     *            Context
     * @param contentUri
     *            Content Uri
     * @param id
     *            Id of the record
     * @return
     */
    public static boolean recordExists(Context context, Uri contentUri, String id) {
        Cursor cursor = context.getContentResolver().query(contentUri, new String[] { CestUnMacDatabaseHelper.ID }, CestUnMacDatabaseHelper.ID + "=?",
                                                           new String[] { id }, null);
        if (cursor == null) {
            return false;
        } else {
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            } else {
                cursor.close();
                return true;
            }
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Bundle info = getInfoFromUri(uri);
        if (info == null) {
            mDataBase.endTransaction();
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        int insert_count = 0;
        String table_name = info.getString(URI_INFO_TABLE_NAME);
        String nullColumnHack = info.getString(URI_INFO_NULL_COLUMN_HACK);
        
        mDataBase.beginTransaction();
        if (table_name != null && nullColumnHack != null && values != null && values.length > 0) {
            for (ContentValues value : values) {
                long inserted_row = mDataBase.insert(table_name, nullColumnHack, value);
                if (inserted_row > -1) {
                    insert_count++;
                }
            }
            mDataBase.setTransactionSuccessful();
        }
        mDataBase.endTransaction();
        
        if (isWarnListeners()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        
        return insert_count;
    }

    /**
     * @param warnListeners
     *            the warnListeners to set
     */
    public static void setWarnListeners(boolean warnListeners) {
        CestUnMacContentProvider.warnListeners = warnListeners;
    }

    /**
     * @return the warnListeners
     */
    public static boolean isWarnListeners() {
        return warnListeners;
    }

}
