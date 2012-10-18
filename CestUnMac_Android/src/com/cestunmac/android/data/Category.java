package com.cestunmac.android.data;

import org.codehaus.jackson.JsonNode;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.cestunmac.android.Constants;
import com.cestunmac.android.utils.ServerExchangeUtils;

public class Category {

    public static final String DB_TABLE_NAME    = "categories";
    public static final String FIELD_TITLE      = "title";
    public static final String FIELD_CATEGORYID = "categoryId";

    private int                storedDBId       = Constants.DB_STORAGE_ID_NOT_STORED;
    private String             title;
    private long               categoryId;

    public Category(Cursor _cursor, Context _context) {
        updateFromCursor(_cursor, _context);
    }

    public Category(JsonNode category_node, Context _context) {
        updateFromJson(category_node, _context);
    }

    private void updateFromJson(JsonNode category_node, Context _context) {
        if (category_node != null) {
            JsonNode id = ServerExchangeUtils.safeJSonGet(category_node, Constants.JSON_ID_KEYWORD);
            if (id != null) {
                this.categoryId = id.getLongValue();

                Category existing_category_for_id = getCategoryFromId(categoryId, _context);
                if (existing_category_for_id != null) {
                    this.storedDBId = existing_category_for_id.getStoredDBId();
                }
            }

            JsonNode cat_title = ServerExchangeUtils.safeJSonGet(category_node, Constants.JSON_TITLE_KEYWORD);
            if (cat_title != null) {
                this.title = cat_title.getTextValue();
            }
        }
    }

    protected void updateFromCursor(Cursor _cursor, Context _context) {
        int id = _cursor.getInt(_cursor.getColumnIndex(CestUnMacDatabaseHelper.ID));
        this.storedDBId = id;

        int _categoryId = _cursor.getInt(_cursor.getColumnIndex(FIELD_CATEGORYID));
        this.categoryId = _categoryId;

        String _title = _cursor.getString(_cursor.getColumnIndex(FIELD_TITLE));
        this.title = _title;
    }

    public void persist(Context _context) {
        ContentValues values = getDBContentValues();
        if (getStoredDBId() == Constants.DB_STORAGE_ID_NOT_STORED) {
            // Create
            _context.getContentResolver().insert(Constants.CATEGORIES_CONTENT_URI, values);
            // C4MLog.d(Constants.LOG_TAG, " -------> Create " + getTitle());
        } else {

            String where = CestUnMacDatabaseHelper.ID + "=" + getStoredDBId();
            // Update
            _context.getContentResolver().update(Constants.CATEGORIES_CONTENT_URI, values, where, null);
            // C4MLog.d(Constants.LOG_TAG, " -------> Update for id " + getStoredDBId() + ": " + getTitle());
        }
    }

    public ContentValues getDBContentValues() {
        ContentValues values = new ContentValues();
        values.put(FIELD_TITLE, getTitle());
        values.put(Category.FIELD_CATEGORYID, getCategoryId());
        return values;
    }

    public String getTitle() {
        return title;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public int getStoredDBId() {
        return storedDBId;
    }

    public static Category getCategoryFromId(long id, Context _context) {
        String selection = Category.FIELD_CATEGORYID + " = ?";
        String[] selectionArgs = new String[] { id + "" };
        Cursor c = _context.getContentResolver().query(Constants.CATEGORIES_CONTENT_URI, null, selection, selectionArgs, null);
        if (c != null) {
            if (c.moveToFirst()) {
                Category cat = getCategoryFromCursor(c, _context);
                c.close();
                return cat;
            } else {
                c.close();
            }
        }
        return null;
    }

    public static Category getCategoryFromCursor(Cursor c, Context _context) {
        return new Category(c, _context);
    }
}
