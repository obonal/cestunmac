package com.cestunmac.android;

import android.net.Uri;

public interface Constants {

    // Database
    public static final String DATABASE_NAME                                    = "cestunmac.db";
    public static final int    DATABASE_VERSION                                 = 1;
    public static final String DB_CONTENTPROVIDER_AUTHORITY                     = "com.cestunmac.android.dataprovider";
    public static final String POSTS_URI_SUFFIX                                 = "posts";
    public static final String CATEGORIES_URI_SUFFIX                            = "categories";
    public static final String ATTACHMENTS_URI_SUFFIX                           = "attachments";
    public static final int    DB_STORAGE_ID_NOT_STORED                         = -1;

    // Content provider MIME TYPE prefixes
    public static final String CONTENT_PROVIDER_SINGLE_ITEM_MIME_TYPE_PREFIX    = "vnd.android.cursor.item/" + DB_CONTENTPROVIDER_AUTHORITY;
    public static final String CONTENT_PROVIDER_MULTIPLE_ITEMS_MIME_TYPE_PREFIX = "vnd.android.cursor.dir/" + DB_CONTENTPROVIDER_AUTHORITY;

    // Content provider URIs
    public static final String CONTENT_URI_PREFIX                               = "content://" + DB_CONTENTPROVIDER_AUTHORITY + "/";
    public static final Uri    POSTS_CONTENT_URI                                = Uri.parse(CONTENT_URI_PREFIX + POSTS_URI_SUFFIX);
    public static final Uri    CATEGORIES_CONTENT_URI                           = Uri.parse(CONTENT_URI_PREFIX + CATEGORIES_URI_SUFFIX);
    public static final Uri    ATTACHMENTS_CONTENT_URI                          = Uri.parse(CONTENT_URI_PREFIX + ATTACHMENTS_URI_SUFFIX);

    /*
     * Récupérer les catégories :
     * http://www.cestunmac.com/?json=get_category_index&dev=1
     * Récupérer tous les posts :
     * http://www.cestunmac.com/?json=get_list_posts&dev=1&limit=[limit]
     * Exemple : http://www.cestunmac.com/?json=get_list_posts&dev=1&limit=30
     * Récupérer les posts depuis une date t
     * http://www.cestunmac.com/?json=get_date_posts&dev=1&date=[timestamp]&limit=[limit]
     */

    public static final int    MAX_NB_POSTS_TO_GET                              = 30;

    public static final String JSON_POST_URL                                    = "http://www.cestunmac.com/";
    public static final String REQUEST_BASE_URL                                 = "http://www.cestunmac.com/?json=";
    public static final String REQUEST_URL_CATEGORIES                           = REQUEST_BASE_URL + "get_category_index&dev=1";
    public static final String REQUEST_URL_POSTS                                = REQUEST_BASE_URL + "get_date_posts&dev=1&limit=" + MAX_NB_POSTS_TO_GET;
    public static final String REQUEST_URL_POSTS_FIRST_TIME                     = REQUEST_BASE_URL + "get_list_posts&dev=1&limit=" + MAX_NB_POSTS_TO_GET;

    public static final String JSON_STATUSCODE_KEYWORD                          = "status";
    public static final String JSON_TIMESTAMP_KEYWORD                           = "timestamp";
    public static final String JSON_POSTS_KEYWORD                               = "posts";
    public static final String JSON_ATTACHMENTS_KEYWORD                         = "attachments";
    public static final String JSON_IMAGES_KEYWORD                              = "images";
    public static final String JSON_TITLE_KEYWORD                               = "title";
    public static final String JSON_DATE_KEYWORD                                = "date";
    public static final String JSON_THUMBNAIL_KEYWORD                           = "thumbnail";
    public static final String JSON_URL_KEYWORD                                 = "url";
    public static final String JSON_CATEGORIES_KEYWORD                          = "categories";
    public static final String JSON_ID_KEYWORD                                  = "id";
    public static final String JSON_FULL_KEYWORD                                = "full";

    public static final String EXTRA_CATEGORY_ID                                = "category_id";

    public static final int    CONNECTION_TIMEOUT                               = 10;
    public static final String LOG_TAG                                          = "C'est Un Mac!";

    public static final String OK_STATUS                                        = "ok";

    public static final String POSTS_REFRESH_KEYWORD                            = "POSTS_REFRESH_KEYWORD";
    public static final String CATEGORIES_REFRESH_KEYWORD                       = "CATEGORIES_REFRESH_KEYWORD";
}
