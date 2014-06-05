package edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Arrays;

/**
 * Fragment containing WebView that displays a full-sized image.
 * Created by bobby on 7/1/13.
 */
class ImageViewerFragment extends Fragment {
    private String image_url_small;
    private String image_url_med;
    private String image_url_large;
    private WebView image_frame;
    private SharedPreferences sharedPreferences;
    private ConnectivityManager connectivityManager;

    /** Overridden Methods. */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_image_viewer, container, false);
        setHasOptionsMenu(true);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle extra = getArguments();

        image_frame = (WebView) getActivity().findViewById(R.id.fragment_feed_image_webview);
        image_frame.getSettings().setBuiltInZoomControls(true);
        image_frame.getSettings().setLoadWithOverviewMode(true);
        image_frame.setBackgroundColor(Color.parseColor(extra.getString("bg_color", "#000000")));

        connectivityManager = getConnectivityManager();

        image_frame.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                try {
                    getActivity().setProgress(newProgress * 100);
                    if (newProgress >= 0)
                        getActivity().setProgressBarIndeterminate(false);
                    if (newProgress >= 100) {
                        getActivity().setProgressBarVisibility(false);
                        getActivity().setProgressBarIndeterminateVisibility(false);
                    }
                } catch (NullPointerException e) {
                    Log.d(getString(R.string.app_tag), "ProgressBar NullPointer!\n" + Arrays.toString(e.getStackTrace()));
                }
            }
        });

        image_url_small = extra.getString("image_url_small");
        image_url_med = extra.getString("image_url_med");
        image_url_large = extra.getString("image_url_large");
        String title = extra.getString("bar_title");

        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().setTitle(title);

        getActivity().setProgressBarVisibility(true);
        getActivity().setProgressBarIndeterminate(true);
        getActivity().setProgressBarIndeterminateVisibility(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (isAdded())
                loadme(pickLoadSize());
            }
        });

        if (isAdded())
            loadme(pickLoadSize());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        image_frame.destroy();
    }

    /**
     * Determines if the network being used is a mobile network.
     * @param net1 The NetworkInfo object representing the network to be tested.
     * @return "true" if network is mobile (3/4G). "false" if it is not (wifi).
     */
    private boolean isMetered(NetworkInfo net1) {
        int type = net1.getType();
        switch (type) {
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Determines what sized image to load in the viewer.
     * @return URL of image to be loaded.
     */
    private String pickLoadSize() {
        connectivityManager = getConnectivityManager();
        NetworkInfo nf = connectivityManager.getActiveNetworkInfo();

        if (nf != null) {
            if (isMetered(nf))
                return sharedPreferences.getString("pref_smart_sizing_size", "small");
            else
                return sharedPreferences.getString("pref_viewer_image_size", "med");
        }
        else {
            Log.d(getString(R.string.app_tag), "NetworkInfo null!");
            return sharedPreferences.getString("pref_viewer_image_size", "med");
        }
    }

    /**
     * Loads the image.
     * @param size Size string. Can be "large", "med", or "small".
     */
    private void loadme(String size) {
        if (size.equals("small") && isTablet())
            image_frame.getSettings().setUseWideViewPort(false);
        else
            image_frame.getSettings().setUseWideViewPort(true);

        image_frame.stopLoading();
        image_frame.loadUrl(getImageUrl(size));
    }

    /**
     * Determines which url to return based on size choice.
     * @param sizeString Size string. Can be "large", "med", or "small".
     * @return URL of correct image size.
     */
    private String getImageUrl(String sizeString) {
        if (sizeString.equals("small"))
            return image_url_small;
        if (sizeString.equals("med"))
            return image_url_med;
        if (sizeString.equals("large"))
            return image_url_large;

        return null;
    }

    /**
     * Grabs the ConnectivityManager object representing the device's networks.
     * @return ConnectivityManager of the device.
     */
    private ConnectivityManager getConnectivityManager() {
        if (isAdded())
            return (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return this.connectivityManager;
    }

    /**
     * Determines if device is a tablet.
     * @return "true" if active device screen is greater than 7" in diagonal.
     */
    boolean isTablet() {
        DisplayMetrics d = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(d);

        float hIn = d.heightPixels / d.ydpi;
        float wIn = d.widthPixels / d.xdpi;
        double diag = Math.sqrt((wIn * wIn) + (hIn * hIn));

        return (diag >= 7);
    }
}
