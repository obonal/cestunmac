package com.cestunmac.android.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonNode;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;

import com.cestunmac.android.Constants;
import com.cestunmac.android.utils.ServerExchangeUtils;

public class Post {

    public static final String DB_TABLE_NAME     = "posts";
    public static final String FIELD_POST_ID     = "postId";
    public static final String FIELD_DATE        = "date";
    public static final String FIELD_TITLE       = "title";
    public static final String FIELD_URL         = "url";
    public static final String FIELD_CATEGORY_ID = "categoryId";

    private int                storedDBId        = Constants.DB_STORAGE_ID_NOT_STORED;
    private String             title;
    private String             url;
    private int                postId;
    private long               timestamp;
    private Category           category;
    private Attachment         attachment;

    public Post(Cursor _cursor, Context _context) {
        updateFromCursor(_cursor, _context);
    }

    public Post(JsonNode post, Context _context) {
        updateFromPost(post, _context);
    }

    private void updateFromPost(JsonNode post, Context _context) {
        // Log.i(Constants.LOG_TAG, "post json: \n" + post);
        JsonNode post_id = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_ID_KEYWORD);
        if (post_id != null) {
            String selection = FIELD_POST_ID + "=?";
            String[] selectionArgs = new String[] { post_id.getIntValue() + "" };
            Cursor c = _context.getContentResolver().query(Constants.POSTS_CONTENT_URI, null, selection, selectionArgs, null);
            if (c != null && c.moveToFirst()) {
                updateFromCursor(c, _context);
            } else {
                this.postId = post_id.getIntValue();
            }

            if (c != null) {
                c.close();
            }
        }

        JsonNode title_node = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_TITLE_KEYWORD);
        if (title_node != null) {
            setTitle(title_node.getTextValue());
        }
        
        JsonNode url_node = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_URL_KEYWORD);
        if (url_node != null) {
            setUrl(url_node.getTextValue());
        }

        JsonNode date_node = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_DATE_KEYWORD);
        if (date_node != null) {
            // 2011-10-31 09:28:21
            /*
             * Examples for April 6, 1970 at 3:23am:
             * "MM/dd/yy h:mmaa" -> "04/06/70 3:23am"
             * "MMM dd, yyyy h:mmaa" -> "Apr 6, 1970 3:23am"
             * "MMMM dd, yyyy h:mmaa" -> "April 6, 1970 3:23am"
             * "E, MMMM dd, yyyy h:mmaa" -> "Mon, April 6, 1970 3:23am&
             * "EEEE, MMMM dd, yyyy h:mmaa" -> "Monday, April 6, 1970 3:23am"
             * "'Noteworthy day: 'M/d/yy" -> "Noteworthy day: 4/6/70"
             */
            try {
                String date_string = date_node.getTextValue();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date d = df.parse(date_string);
                // Log.d("DATE", date_string + " => " + d);
                this.timestamp = d.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        JsonNode categories_array = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_CATEGORIES_KEYWORD);
        if (categories_array != null) {
            JsonNode category_node = categories_array.get(0);
            category = new Category(category_node, _context);
            if ((category.getStoredDBId() == Constants.DB_STORAGE_ID_NOT_STORED)) {
                // category.persist(_context);
            }
        }

        JsonNode attachments_array = ServerExchangeUtils.safeJSonGet(post, Constants.JSON_ATTACHMENTS_KEYWORD);
        if (attachments_array != null) {
            JsonNode attachment_node = attachments_array.get(0);
            attachment = new Attachment(attachment_node);
            if ((attachment.getStoredDBId() == Constants.DB_STORAGE_ID_NOT_STORED)) {
                attachment.persist(_context);
            }
        }

    }

    protected void updateFromCursor(Cursor _cursor, Context _context) {
        int id = _cursor.getInt(_cursor.getColumnIndex(CestUnMacDatabaseHelper.ID));
        this.storedDBId = id;

        int _postId = _cursor.getInt(_cursor.getColumnIndex(FIELD_POST_ID));
        this.postId = _postId;

        String _title = _cursor.getString(_cursor.getColumnIndex(FIELD_TITLE));
        setTitle(_title);
        
        String _url = _cursor.getString(_cursor.getColumnIndex(FIELD_URL));
        setUrl(_url);

        long _timestamp = _cursor.getLong(_cursor.getColumnIndex(FIELD_DATE));
        this.timestamp = _timestamp;

        int _categoryId = _cursor.getInt(_cursor.getColumnIndex(Category.FIELD_CATEGORYID));
        String selection = Category.FIELD_CATEGORYID + "=?";
        String[] selectionArgs = new String[] { "" + _categoryId };
        Cursor cat_cursor = _context.getContentResolver().query(Constants.CATEGORIES_CONTENT_URI, null, selection, selectionArgs, null);
        if (cat_cursor != null && cat_cursor.moveToFirst()) {
            Category cat = new Category(cat_cursor, _context);
            cat_cursor.close();
            this.category = cat;
        }

        int _attachmentId = _cursor.getInt(_cursor.getColumnIndex(Attachment.FIELD_ATTACHMENTID));
        selection = Attachment.FIELD_ATTACHMENTID + "=?";
        selectionArgs = new String[] { "" + _attachmentId };
        Cursor att_cursor = _context.getContentResolver().query(Constants.ATTACHMENTS_CONTENT_URI, null, selection, selectionArgs, null);
        if (att_cursor != null && att_cursor.moveToFirst()) {
            Attachment att = new Attachment(att_cursor, _context);
            att_cursor.close();
            this.attachment = att;
        }
    }

    public void persist(Context _context) {
        ContentValues values = getDBContentValues();
        if (isStoredInDb() == false) {
            // Create
            _context.getContentResolver().insert(Constants.POSTS_CONTENT_URI, values);
            // C4MLog.d(Constants.LOG_TAG, " -------> Create " + getTitle());
        } else {

            String where = CestUnMacDatabaseHelper.ID + "=" + getStoredDBId();
            // Update
            _context.getContentResolver().update(Constants.POSTS_CONTENT_URI, values, where, null);
            // C4MLog.d(Constants.LOG_TAG, " -------> Update for id " + getStoredDBId() + ": " + getTitle());
        }
    }

    public ContentValues getDBContentValues() {
        ContentValues values = new ContentValues();
        values.put(FIELD_POST_ID, getPostId());
        values.put(FIELD_DATE, getTimestamp());
        values.put(FIELD_TITLE, getTitle());
        values.put(FIELD_URL, getUrl());
        values.put(Category.FIELD_CATEGORYID, category.getCategoryId());
        values.put(Attachment.FIELD_ATTACHMENTID, attachment.getAttachmentId());
        return values;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        // this.title = URLDecoder.decode(title);
        this.title = Html.fromHtml(title).toString();
        // this.title = title;
    }

    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Category getCategory() {
        return category;
    }

    public int getPostId() {
        return postId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThumbnailURLString() {
        return attachment.getThumbnailURLString();
    }

    public String getFullURLString() {
        return attachment.getFullURLString();
    }

    public int getStoredDBId() {
        return storedDBId;
    }

    public static Post getPostFromCursor(Cursor c, Context _context) {
        return new Post(c, _context);
    }

    public boolean isStoredInDb() {
        return (getStoredDBId() != Constants.DB_STORAGE_ID_NOT_STORED);
    }

}
