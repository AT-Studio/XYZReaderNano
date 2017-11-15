package com.example.xyzreader.data;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

/**
 * Helper for loading a list of articles or a single article.
 */
public class ArticleLoader extends CursorLoader {
    public static ArticleLoader newAllArticlesInstance(Context context) {
        return new ArticleLoader(context, ItemsContract.Items.buildDirUri());
    }

    public static ArticleLoader newInstanceForItemId(Context context, long itemId) {
        return new ArticleLoader(context, ItemsContract.Items.buildDirUri(), itemId);
    }

    private ArticleLoader(Context context, Uri uri) {
        super(context, uri, Query.PROJECTION, null, null, ItemsContract.Items.DEFAULT_SORT);
    }

    private ArticleLoader(Context context, Uri uri, long itemId) {
        super(
                context,
                uri,
                Query.PROJECTION,
                ItemsContract.Items.SERVER_ID + "=?",
                new String[]{Long.toString(itemId)},
                ItemsContract.Items.DEFAULT_SORT);
    }

    public interface Query {
        String[] PROJECTION = {
                ItemsContract.Items._ID,
                ItemsContract.Items.SERVER_ID,
                ItemsContract.Items.TITLE,
                ItemsContract.Items.PUBLISHED_DATE,
                ItemsContract.Items.AUTHOR,
                ItemsContract.Items.THUMB_URL,
                ItemsContract.Items.PHOTO_URL,
                ItemsContract.Items.ASPECT_RATIO,
                ItemsContract.Items.BODY,
        };

        int _ID = 0;
        int SERVER_ID = 1;
        int TITLE = 2;
        int PUBLISHED_DATE = 3;
        int AUTHOR = 4;
        int THUMB_URL = 5;
        int PHOTO_URL = 6;
        int ASPECT_RATIO = 7;
        int BODY = 8;
    }
}
