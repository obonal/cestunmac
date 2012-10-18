package com.cestunmac.android.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cestunmac.android.Constants;
import com.cestunmac.android.R;
import com.cestunmac.android.data.Post;
import com.cestunmac.android.utils.ImageDownloader;

public class PostDetailsFragment extends SherlockFragment {

    private static final int MENU_SHARE = 0;

    /**
     * Create a new instance of DetailsFragment, initialized to
     * show the text at 'index'.
     * 
     * @param post_id
     */
    public static PostDetailsFragment newInstance(int index, int post_id) {
        PostDetailsFragment f = new PostDetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        args.putInt("postId", post_id);
        f.setArguments(args);

        return f;
    }

    private Post mDisplayedPost;

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    public int getShownPostId() {
        return getArguments().getInt("postId", -1);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);
        super.onActivityCreated(savedInstanceState);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        
//    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDisplayedPost != null) {
          // Share
          MenuItem options_menu_item = menu.add(Menu.NONE, MENU_SHARE, 0, R.string.menu_share);
          options_menu_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
          options_menu_item.setIcon(R.drawable.ic_action_share);
      }
      super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist. The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed. Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }

        View v = inflater.inflate(R.layout.post_details, container, false);

        int postId = getShownPostId();
        // Log.d(Constants.LOG_TAG, "index = " + getShownIndex() + "; postId = " + postId);
        String selection = Post.FIELD_POST_ID + "=?";
        String[] selectionArgs = new String[] { postId + "" };
        Cursor c = getActivity().getContentResolver().query(Constants.POSTS_CONTENT_URI, null, selection, selectionArgs, null);
        mDisplayedPost = null;
        if (c != null) {
            if (c.moveToFirst()) {
                mDisplayedPost = new Post(c, getActivity());
            }
            c.close();
        }

        WebView wv = (WebView) v.findViewById(R.id.content_webview);
        if (mDisplayedPost != null) {
            // Log.d(Constants.LOG_TAG, "p = " + p.getTitle());

            // Set Title if needed
            View detailsFrame = getActivity().findViewById(R.id.details);
            boolean dual_pane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
            if (dual_pane == false) {
                // Log.d(Constants.LOG_TAG, " ******* SETTING TITLE TO: " + p.getTitle());
                getActivity().setTitle(mDisplayedPost.getTitle());
            }

            // Log.d(Constants.LOG_TAG, "p.getFullURLString() = " + p.getFullURLString());

            new ImageDownloadAndDisplayTask().execute(mDisplayedPost.getFullURLString());
        } else {
            wv.loadData("", "text/html", "UTF-8");
        }

        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_SHARE && mDisplayedPost != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_title));
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mDisplayedPost.getUrl());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private class ImageDownloadAndDisplayTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... urls) {
            String fileCachePath = null;
            try {
                File cacheDir = getActivity().getExternalCacheDir();
                int count = urls.length;
                for (int i = 0; i < count; i++) {
                    String img_url = urls[i];
                    File img = new File(cacheDir, ImageDownloader.md5(img_url));
                    fileCachePath = img.getAbsolutePath();

                    if (img.exists()) {
                        continue;
                    }

                    Bitmap bitmap = ImageDownloader.downloadBitmap(img_url);
                    // save image to SD
                    if (img.createNewFile()) {
                        OutputStream out = new FileOutputStream(img);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
                        out.flush();
                        out.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileCachePath;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(String result) {
            if (getView() != null) {
                WebView wv = (WebView) getView().findViewById(R.id.content_webview);
                wv.getSettings().setBuiltInZoomControls(true);
                final String mimeType = "text/html";
                final String encoding = "utf-8";
                String html_content = "<html><body><img border=\"0\" src=\"file://" + result + "\" width=\"100%\"  /></body></html>";
                wv.loadDataWithBaseURL("fake://not/needed", html_content, mimeType, encoding, "");
            }
        }
    }
}
