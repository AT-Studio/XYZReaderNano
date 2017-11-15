package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.R.id.toolbar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = ArticleListActivity.class.toString();
    @BindView(toolbar) Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    Adapter adapter;

    @BindView(R.id.loadingWrapper)
    FrameLayout loadingWrapper;
    @BindView(R.id.mainProgressBar)
    ProgressBar mainProgressBar;
    @BindView(R.id.checkInternet)
    TextView checkInternet;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    int numColumns;

    DisplayMetrics displayMetrics;

    boolean calledOnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        calledOnCreate = true;

        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(0, null, this);

        numColumns = getResources().getInteger(R.integer.list_column_count);

        displayMetrics = getResources().getDisplayMetrics();

        if (savedInstanceState == null) {
            refresh();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadingWrapper.getVisibility() == View.VISIBLE) {
                    mainProgressBar.setVisibility(View.GONE);
                    checkInternet.setVisibility(View.VISIBLE);
                }
            }
        }, 10000);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_licenses) {
            Intent intent = new Intent(this, LicenseActivity.class);
            startActivity(intent);
        }

        return true;
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursor.getCount() <= 0) return;

        cursor.moveToFirst();

        if (loadingWrapper.getVisibility() == View.VISIBLE) {
            loadingWrapper.setVisibility(View.GONE);
            fadeIn(mSwipeRefreshLayout, 100);
        }

        if (adapter == null) {

            adapter = new Adapter(cursor);
            mRecyclerView.setAdapter(adapter);
            StaggeredGridLayoutManager sglm =
                    new StaggeredGridLayoutManager(numColumns, StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(sglm);
        }
        else {
            adapter.updateAdapter(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private ArrayList<StoryItem> storyItems;
        private LinkedList<Target> targets;

        public Adapter(Cursor cursor) {

            storyItems = new ArrayList<>();

            while (!cursor.isAfterLast()) {

                long DbId = cursor.getLong(ArticleLoader.Query._ID);
                long storyId = cursor.getLong(ArticleLoader.Query.SERVER_ID);
                String title = cursor.getString(ArticleLoader.Query.TITLE);
                String author = cursor.getString(ArticleLoader.Query.AUTHOR);
                String thumbnailURL = cursor.getString(ArticleLoader.Query.THUMB_URL);
                String publishedDate = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);

                storyItems.add(new StoryItem(DbId, storyId, title, author, thumbnailURL, publishedDate));

                cursor.moveToNext();

            }

            Collections.sort(storyItems);

            this.targets = new LinkedList<>();
        }

        public void updateAdapter(Cursor cursor) {
            while (!cursor.isAfterLast()) {

                long DbId = cursor.getLong(ArticleLoader.Query._ID);
                long storyId = cursor.getLong(ArticleLoader.Query.SERVER_ID);

                int position = binarySearchArray(storyId, storyItems);

                if (storyItems.get(position).storyId == storyId) {
                    storyItems.get(position).DbId = DbId;
                } else {
                    String title = cursor.getString(ArticleLoader.Query.TITLE);
                    String author = cursor.getString(ArticleLoader.Query.AUTHOR);
                    String thumbnailURL = cursor.getString(ArticleLoader.Query.THUMB_URL);
                    String publishedDate = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);

                    storyItems.add(position, new StoryItem(DbId, storyId, title, author, thumbnailURL, publishedDate));
                    notifyItemInserted(position);
                }

                cursor.moveToNext();

            }

        }

        public int binarySearchArray(long searchId, ArrayList<StoryItem> items) {

            int start = 0;
            int end = items.size() - 1;

            while (start <= end) {

                int middle = (start + end) / 2;

                if (searchId < items.get(middle).storyId) {
                    end = middle - 1;
                } else if (searchId > items.get(middle).storyId) {
                    start = middle + 1;
                } else if (searchId == items.get(middle).storyId) {
                    return middle;
                }
            }

            return start;

        }

        @Override
        public long getItemId(int position) {
            return storyItems.get(position).DbId;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    long[] ids = new long[storyItems.size()];
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = storyItems.get(i).storyId;
                    }
                    bundle.putInt(ArticleDetailActivity.START_POS_EXTRA, vh.getAdapterPosition());
                    bundle.putLongArray(ArticleDetailActivity.DBIDS_EXTRA, ids);
                    Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
            return vh;
        }

        private Date parsePublishedDate(int position) {
            try {
                String date = storyItems.get(position).publishedDate;
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {

            final StoryItem item = storyItems.get(position);
            holder.titleView.setText(item.title);
            Date publishedDate = parsePublishedDate(position);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + item.author));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + item.author));
            }

            String imagePath = item.thumbnailURL;

            if (getItemCount() - position <= numColumns) {
                FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) holder.recipeCard.getLayoutParams();
                params.bottomMargin = getPixels(20);
                holder.recipeCard.setLayoutParams(params);
            } else {
                FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) holder.recipeCard.getLayoutParams();
                params.bottomMargin = 0;
                holder.recipeCard.setLayoutParams(params);
            }

            final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.imageWrapper.getLayoutParams();
            params.height = getPixels(150);
            holder.imageWrapper.setLayoutParams(params);

            if (!TextUtils.isEmpty(imagePath)) {
                holder.thumbnailView.setVisibility(View.GONE);
                holder.noImageText.setVisibility(View.INVISIBLE);
                holder.imageProgressBar.setVisibility(View.VISIBLE);
                if (item.thumbnailHeight != null) {
                    final LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) holder.imageWrapper.getLayoutParams();
                    params.height = item.thumbnailHeight;
                    holder.imageWrapper.setLayoutParams(params1);
                }
                final Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                        holder.imageProgressBar.setVisibility(View.GONE);
                        holder.noImageText.setVisibility(View.GONE);
                        holder.thumbnailView.setVisibility(View.INVISIBLE);
                        holder.imageWrapper.post(new Runnable() {
                            @Override
                            public void run() {
                                float ratio = (float) bitmap.getHeight() / bitmap.getWidth();

                                final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.imageWrapper.getLayoutParams();

                                int width = holder.imageWrapper.getWidth();

                                FrameLayout.LayoutParams imageParams = (FrameLayout.LayoutParams) holder.thumbnailView.getLayoutParams();
                                imageParams.height = (int) (width * ratio);
                                holder.thumbnailView.setLayoutParams(imageParams);

                                if (item.thumbnailHeight == null) {
                                    item.thumbnailHeight = (int) (width * ratio);
                                    ValueAnimator animator = ValueAnimator.ofInt(holder.imageWrapper.getHeight(), item.thumbnailHeight).setDuration(300);
                                    animator.setInterpolator(new DecelerateInterpolator());
                                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            params.height = (int) valueAnimator.getAnimatedValue();
                                            holder.imageWrapper.setLayoutParams(params);
                                        }
                                    });
                                    animator.addListener(new Animator.AnimatorListener() {
                                        @Override
                                        public void onAnimationStart(Animator animator) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animator animator) {
                                        }

                                        @Override
                                        public void onAnimationCancel(Animator animator) {

                                        }

                                        @Override
                                        public void onAnimationRepeat(Animator animator) {

                                        }
                                    });
                                    animator.start();
                                }
                                holder.thumbnailView.setImageBitmap(bitmap);
                                fadeIn(holder.thumbnailView, 300);
                            }
                        });

                        targets.remove(this);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        holder.imageProgressBar.setVisibility(View.GONE);
                        holder.noImageText.setVisibility(View.VISIBLE);

                        targets.remove(this);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };
                targets.add(target);
                Picasso.with(ArticleListActivity.this).load(imagePath).into(target);
            } else {
                holder.thumbnailView.setVisibility(View.GONE);
                holder.noImageText.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return storyItems.size();
        }
    }

    public class StoryItem implements Comparable<StoryItem> {

        long DbId;
        long storyId;
        String title;
        String author;
        String thumbnailURL;
        String publishedDate;
        Integer thumbnailHeight;

        public StoryItem(long DbId, long storyId, String title, String author, String thumbnailURL, String publishedDate) {
            this.DbId = DbId;
            this.storyId = storyId;
            this.title = title;
            this.author = author;
            this.thumbnailURL = thumbnailURL;
            this.publishedDate = publishedDate;
        }

        @Override
        public int compareTo(@NonNull StoryItem storyItem) {
            return (int) (this.storyId - storyItem.storyId);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.recipeCard) CardView recipeCard;
        @BindView(R.id.imageProgressBar) ProgressBar imageProgressBar;
        @BindView(R.id.noImageText) TextView noImageText;
        @BindView(R.id.thumbnail) ImageView thumbnailView;
        @BindView(R.id.article_title) TextView titleView;
        @BindView(R.id.article_subtitle) TextView subtitleView;
        @BindView(R.id.imageWrapper) FrameLayout imageWrapper;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public int getPixels(int dp) {
        return (int) (dp * displayMetrics.density);
    }

    public void fadeIn(View layout, int time)    {
        layout.setAlpha(0f);
        layout.setVisibility(View.VISIBLE);
        layout.animate()
                .alpha(1f)
                .setDuration(time)
                .setListener(null);

    }
}
