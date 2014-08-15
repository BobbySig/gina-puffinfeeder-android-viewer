package edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;

import java.util.ArrayList;
import java.util.Collections;

import edu.alaska.gina.feeder.android.core.data.Entry;
import edu.alaska.gina.feeder.android.core.data.Feed;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.FullscreenViewerActivity;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.R;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.adapters.EntriesAdapter;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.network.FeederSpiceManager;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.network.JSONRequest;
import edu.alaska.gina.feeder.gina_puffinfeeder_android_viewer.network.JsonSpiceService;

/**
 * Fragment used to display the list of feed images in a GridView.
 * Created by bobby on 6/14/13.
 */
public class EntriesFragment extends Fragment {
    private final FeederSpiceManager mSpiceManager = new FeederSpiceManager(JsonSpiceService.class);

    private int fadeAnimationDuration;
    private View loadingView;
    private GridView contentView;

    private ContentDataFragment data;
    private Feed currentFeed;
    private EntriesAdapter mImageAdapter;

    /* Variable for keeping track of what to load next */
    private long leastRecentId = -1;

    /* Overridden Methods. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getArguments();
        this.currentFeed = (Feed) extras.getSerializable("feed");
        this.fadeAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_entries, container, false);
        setHasOptionsMenu(true);

        this.loadingView = v.findViewById(R.id.grid_progressBar);
        this.contentView = (GridView) v.findViewById(R.id.image_grid);

        Log.d(getResources().getString(R.string.app_tag) + "-flow", "---- Loading " + currentFeed.title + " ----");
        this.data = (ContentDataFragment) getActivity().getFragmentManager().findFragmentByTag(getString(R.string.content_retained_tag));
        if (this.data == null || this.data.entries.size() < 1) {
            this.data = new ContentDataFragment();
            this.data.firstVisible = 0;
            getActivity().getFragmentManager().beginTransaction().add(this.data, getString(R.string.content_retained_tag)).commit();
            this.data.retainedFeed = this.currentFeed;
            this.data.entries = new ArrayList<Entry>(12);
            this.mImageAdapter = new EntriesAdapter(this.getActivity(), data.entries);
            Log.d(getResources().getString(R.string.app_tag) + "-flow", "Retained data created.");
            initialEntriesNetworkRequest();
        } else if (!this.data.retainedFeed.equals(this.currentFeed)) {
            Log.d(getResources().getString(R.string.app_tag) + "-flow", "Incorrect retained data found.");
            this.data.firstVisible = 0;
            this.data.retainedFeed = this.currentFeed;
            this.data.entries.clear();
            this.mImageAdapter = new EntriesAdapter(this.getActivity(), data.entries);
            initialEntriesNetworkRequest();
        } else {
            Log.d(getResources().getString(R.string.app_tag) + "-flow", "Retained data found.");
            this.mImageAdapter = new EntriesAdapter(this.getActivity(), data.entries);
            this.leastRecentId = this.data.entries.get(this.data.entries.size() - 1).uid;
            this.mImageAdapter.notifyDataSetChanged();
            this.contentView.setAlpha(0f);
            this.contentView.setVisibility(View.VISIBLE);
            this.contentView.animate().alpha(1f).setDuration(fadeAnimationDuration).setListener(null);
            this.loadingView.animate().alpha(0f).setDuration(fadeAnimationDuration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadingView.setVisibility(View.GONE);
                }
            });
        }
        Log.d(getResources().getString(R.string.app_tag) + "-flow", "---- " + currentFeed.title + " Loaded ----");

        if (this.contentView == null)
            Log.d(getString(R.string.app_tag), "contentView is null!");
        this.contentView.setAdapter(mImageAdapter);
        this.contentView.setGravity(Gravity.CENTER_HORIZONTAL);

        this.contentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent photoView = new Intent(getActivity(), FullscreenViewerActivity.class);

                Bundle args = new Bundle();
                args.putSerializable("entry", data.entries.get(position));
                args.putString("feed-title", currentFeed.title);
                args.putString("feed-type", currentFeed.category);
                photoView.putExtras(args);

                getActivity().startActivity(photoView);
            }
        });

        this.contentView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean loading = false;
            private int previousTotalItems = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                /* Do noting. */
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (leastRecentId < 0)
                    return;

                if (!this.loading && (firstVisibleItem + visibleItemCount) >= totalItemCount) {
                    moreEntriesNetworkRequest(leastRecentId);
                    this.loading = true;
                }

                if (this.loading && this.previousTotalItems < totalItemCount) {
                    this.loading = false;
                    mImageAdapter.notifyDataSetChanged();
                }

                this.previousTotalItems = totalItemCount;
            }
        });

        if (getActivity().getActionBar() != null)
            getActivity().getActionBar().setTitle(currentFeed.title);
        this.mImageAdapter.notifyDataSetChanged();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.contentView.setSelection(this.data.firstVisible);
        if (!this.mSpiceManager.isStarted()) {
            Log.d(getString(R.string.app_tag), "Attempting to recover spice request...");
            this.mSpiceManager.addListenerIfPending(Entry[].class, getString(R.string.entries_cache), new ImageFeedRequestListener());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.data.firstVisible = contentView.getFirstVisiblePosition();
    }

    @Override
    public void onDetach() {
        if (mSpiceManager.isStarted())
            mSpiceManager.shouldStop();
        super.onDetach();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                this.data.retainedFeed = new Feed();
                ((FeederActivity)getActivity()).openEntriesFragment(this.currentFeed);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public Feed getCurrentFeed() {
        return this.currentFeed;
    }

    /**
     * Method that starts the initial network request for entries JSON file.
     */
    private void initialEntriesNetworkRequest() {
        Log.d(getString(R.string.app_tag) + "-network", "Requesting newest entries.");
        if (!mSpiceManager.isStarted())
            mSpiceManager.start(getActivity().getBaseContext());

        this.data.loadingMore = true;
        this.data.loadingNew = true;

        mSpiceManager.execute(new JSONRequest<Entry[]>(Entry[].class, currentFeed.entries_url + "?count=24"), getString(R.string.entries_cache), DurationInMillis.ALWAYS_EXPIRED, new ImageFeedRequestListener());
    }

    private void moreEntriesNetworkRequest(long maxId) {
        Log.d(getString(R.string.app_tag) + "-network", "Requesting entries from entry " + this.leastRecentId + ".");
        if (!mSpiceManager.isStarted())
            mSpiceManager.start(getActivity().getBaseContext());

        this.data.loadingMore = true;
        this.data.loadingNew = false;

        getActivity().setProgressBarIndeterminateVisibility(true);
        mSpiceManager.execute(new JSONRequest<Entry[]>(Entry[].class, currentFeed.entries_url + "?count=24&max_id=" + maxId), getString(R.string.entries_cache), DurationInMillis.ALWAYS_EXPIRED, new ImageFeedRequestListener());
    }

    /* Class to run after RoboSpice task completion. */
    public class ImageFeedRequestListener implements PendingRequestListener<Entry[]> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            data.loadingMore = false;
            data.loadingNew = false;

            getActivity().setProgressBarIndeterminateVisibility(false);
            Log.d(getString(R.string.app_tag), "Failed to load entries " + spiceException.getMessage());
            Toast.makeText(getActivity(), "Failed to load entries", Toast.LENGTH_SHORT).show();
            loadingView.setVisibility(View.GONE);
        }

        @Override
        public void onRequestSuccess(Entry[] entries) {
            data.loadingMore = false;
            data.loadingNew = false;

            Log.d(getString(R.string.app_tag), "Request Success");
            Collections.addAll(data.entries, entries);
            mImageAdapter.notifyDataSetChanged();

            if (contentView.getVisibility() == View.GONE) {
                contentView.setAlpha(0f);
                contentView.setVisibility(View.VISIBLE);
                contentView.animate().alpha(1f).setDuration(fadeAnimationDuration).setListener(null);
                loadingView.animate().alpha(0f).setDuration(fadeAnimationDuration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (getActivity() != null) {
                            loadingView.setVisibility(View.GONE);
                            getActivity().setProgressBarIndeterminateVisibility(false);
                        }
                    }
                });
            } else {
                getActivity().setProgressBarIndeterminateVisibility(false);
            }

            if (data.entries.size() >= 1)
                leastRecentId = data.entries.get(data.entries.size() - 1).uid;
        }

        @Override
        public void onRequestNotFound() {
            if (data.loadingMore || data.loadingNew) {
                Log.d(getString(R.string.app_tag) + "-network", "Request Lost, retrying.");
                if (data.loadingNew)
                    initialEntriesNetworkRequest();
                if (data.loadingMore)
                    moreEntriesNetworkRequest(data.firstVisible);
            }
        }
    }

    public static class ContentDataFragment extends Fragment {
        Feed retainedFeed;
        ArrayList<Entry> entries;
        int firstVisible;
        boolean loadingMore = false;
        boolean loadingNew = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }
}
