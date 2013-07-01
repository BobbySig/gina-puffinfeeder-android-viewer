package edu.alaska.gina.feeder.puffinfeeder;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Created by bobby on 7/1/13.
 */
public class ImageViewerFragment extends SherlockFragment{
    protected String image_url;
    protected String title;
    protected WebView image_frame;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_image_view_frame, container, false);
        setHasOptionsMenu(true);

        image_frame = (WebView) getSherlockActivity().findViewById(R.id.feed_image_webView);
        image_frame.getSettings().setBuiltInZoomControls(true);
        image_frame.getSettings().setLoadWithOverviewMode(true);
        image_frame.getSettings().setUseWideViewPort(true);

        Bundle extra = getArguments();
        if (extra != null) {
            image_url = extra.getString("image_url");
            title = extra.getString("bar_title");
        }
        else {
            Log.d("Puffin Feeder", "No Image URL. Please Fix that...");
            Toast.makeText(getActivity(), "No Image URL. Please fix that...", Toast.LENGTH_SHORT).show();
            return v;
        }

        getSherlockActivity().getSupportActionBar().setTitle(title);
        image_frame.loadUrl(image_url);

        return v;
    }
}
