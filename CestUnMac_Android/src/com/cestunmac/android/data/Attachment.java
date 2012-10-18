package com.cestunmac.android.data;

import org.codehaus.jackson.JsonNode;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.cestunmac.android.Constants;
import com.cestunmac.android.utils.ServerExchangeUtils;

public class Attachment {

    public static final String DB_TABLE_NAME            = "attachments";
    public static final String FIELD_ATTACHMENTID       = "attachmentId";
    public static final String FIELD_THUMBNAILURLSTRING = "thumbnailURLString";
    public static final String FIELD_FULLURLSTRING      = "fullURLString";

    private int                storedDBId               = Constants.DB_STORAGE_ID_NOT_STORED;
    private int                attachmentId;
    private String             thumbnailURLString;
    private String             fullURLString;

    public Attachment(Cursor _cursor, Context _context) {
        updateFromCursor(_cursor, _context);
    }

    public Attachment(JsonNode attachment_node) {
        updateFromJson(attachment_node);
    }

    private void updateFromJson(JsonNode attachment_node) {
        Log.i(Constants.LOG_TAG, "attachment_node = " + attachment_node);
        JsonNode id = ServerExchangeUtils.safeJSonGet(attachment_node, Constants.JSON_ID_KEYWORD);
        if (id != null) {
            this.attachmentId = id.getIntValue();
        }
        
        JsonNode images = ServerExchangeUtils.safeJSonGet(attachment_node, Constants.JSON_IMAGES_KEYWORD);
        if (images != null) {
            JsonNode thumbnail = ServerExchangeUtils.safeJSonGet(images, Constants.JSON_THUMBNAIL_KEYWORD);
            if (thumbnail != null) {
                JsonNode thumbnail_url = ServerExchangeUtils.safeJSonGet(thumbnail, Constants.JSON_URL_KEYWORD);
                if (thumbnail_url != null) {
                    this.thumbnailURLString = thumbnail_url.getTextValue();
                }
            }
            JsonNode full = ServerExchangeUtils.safeJSonGet(images, Constants.JSON_FULL_KEYWORD);
            if (full != null) {
                JsonNode full_url = ServerExchangeUtils.safeJSonGet(full, Constants.JSON_URL_KEYWORD);
                if (full_url != null) {
                    this.fullURLString = full_url.getTextValue();
                }
            }
        }
    }

    protected void updateFromCursor(Cursor _cursor, Context _context) {
        int id = _cursor.getInt(_cursor.getColumnIndex(CestUnMacDatabaseHelper.ID));
        this.storedDBId = id;

        int _attachmentId = _cursor.getInt(_cursor.getColumnIndex(FIELD_ATTACHMENTID));
        this.attachmentId = _attachmentId;

        String _thumbnailURLString = _cursor.getString(_cursor.getColumnIndex(FIELD_THUMBNAILURLSTRING));
        this.thumbnailURLString = _thumbnailURLString;
        
        String _fullURLString = _cursor.getString(_cursor.getColumnIndex(FIELD_FULLURLSTRING));
        this.fullURLString = _fullURLString;
    }

    public void persist(Context _context) {
        ContentValues values = getDBContentValues();
        if (getStoredDBId() == Constants.DB_STORAGE_ID_NOT_STORED) {
            // Create
            _context.getContentResolver().insert(Constants.ATTACHMENTS_CONTENT_URI, values);
            // C4MLog.d(Constants.LOG_TAG, " -------> Create " + getTitle());
        } else {

            String where = CestUnMacDatabaseHelper.ID + "=" + getStoredDBId();
            // Update
            _context.getContentResolver().update(Constants.ATTACHMENTS_CONTENT_URI, values, where, null);
            // C4MLog.d(Constants.LOG_TAG, " -------> Update for id " + getStoredDBId() + ": " + getTitle());
        }
    }

    public ContentValues getDBContentValues() {
        ContentValues values = new ContentValues();
        values.put(FIELD_ATTACHMENTID, getAttachmentId());
        values.put(FIELD_THUMBNAILURLSTRING, getThumbnailURLString());
        values.put(FIELD_FULLURLSTRING, getFullURLString());
        return values;
    }

    public int getAttachmentId() {
        return attachmentId;
    }

    public String getThumbnailURLString() {
        return thumbnailURLString;
    }

    public String getFullURLString() {
        return fullURLString;
    }

    public int getStoredDBId() {
        return storedDBId;
    }
}
