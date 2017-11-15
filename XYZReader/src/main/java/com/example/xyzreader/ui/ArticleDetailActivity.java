package com.example.xyzreader.ui;

import android.animation.ValueAnimator;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String START_POS_EXTRA = "startPosition";
    public static final String DBIDS_EXTRA = "DbIds";

    long[] DbIds;
    int startPosition;

    @BindView(R.id.mainLayout)
    CoordinatorLayout mainLayout;

    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.storyThumbnail)
    ImageView storyThumbnail;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout toolbarLayout;

    @BindView(R.id.app_bar)
    AppBarLayout appBarLayout;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.storyName)
    TextView storyName;

    @BindView(R.id.author)
    TextView author;

    @BindView(R.id.nestedScrollView)
    NestedScrollView nestedScrollView;

    @BindView(R.id.scrollFAB)
    FloatingActionButton scrollFAB;

    @BindView(R.id.cursorProgressBar)
    ProgressBar cursorProgressBar;

    DisplayMetrics displayMetrics;

    String textTitle;

    boolean cursorLoading;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        ButterKnife.bind(this);

        Bundle bundle = getIntent().getExtras();

        if (savedInstanceState != null) {
            startPosition = savedInstanceState.getInt(START_POS_EXTRA);
            DbIds = savedInstanceState.getLongArray(DBIDS_EXTRA);
        } else if (bundle != null) {
            startPosition = bundle.getInt(START_POS_EXTRA);
            DbIds = bundle.getLongArray(DBIDS_EXTRA);
        } else {
            finish();
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayMetrics = getResources().getDisplayMetrics();

        getSupportLoaderManager().initLoader(0, null, this);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBar, int verticalOffset) {

                if (Math.abs(verticalOffset) == appBar.getTotalScrollRange() && textTitle != null) {
                    toolbarLayout.setTitle(textTitle);
                    ValueAnimator anim = ValueAnimator.ofFloat(scrollFAB.getScaleX(), 0f).setDuration(50);
                    anim.setInterpolator(new DecelerateInterpolator());
                    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            float value = (float) valueAnimator.getAnimatedValue();
                            scrollFAB.setScaleX(value);
                            scrollFAB.setScaleY(value);
                        }
                    });
                    anim.start();
                } else {
                    toolbarLayout.setTitle(" ");
                    ValueAnimator anim = ValueAnimator.ofFloat(scrollFAB.getScaleX(), 1f).setDuration(50);
                    anim.setInterpolator(new DecelerateInterpolator());
                    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            float value = (float) valueAnimator.getAnimatedValue();
                            scrollFAB.setScaleX(value);
                            scrollFAB.setScaleY(value);
                        }
                    });
                    anim.start();
                }

            }
        });

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

            }
        });

        scrollFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appBarLayout.setExpanded(false, true);
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (cursorLoading) {
                    cursorProgressBar.setVisibility(View.VISIBLE);
                }
            }
        }, 1000);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        cursorLoading = true;
        return ArticleLoader.newInstanceForItemId(this, DbIds[startPosition]);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        cursor.moveToFirst();

        textTitle = cursor.getString(ArticleLoader.Query.TITLE);

        Picasso.with(this).load(cursor.getString(ArticleLoader.Query.PHOTO_URL)).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap poster, Picasso.LoadedFrom from) {

                float ratio = (float) poster.getHeight() / poster.getWidth();
                int height = (int) (displayMetrics.widthPixels * ratio);

                final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
                params.height = height + getStatusBarHeight();
                appBarLayout.setLayoutParams(params);

                FrameLayout.LayoutParams params1 = (FrameLayout.LayoutParams) storyThumbnail.getLayoutParams();
                params1.height = height;
                storyThumbnail.setLayoutParams(params1);
                storyThumbnail.setImageBitmap(poster);

            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Toast.makeText(ArticleDetailActivity.this, "Failed to load thumbnail.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) viewPager.getLayoutParams();
        params.height = displayMetrics.heightPixels - (getPixels(56) + getStatusBarHeight());
        viewPager.setLayoutParams(params);

        storyName.setText(textTitle);

        Date publishedDate = parsePublishedDate(cursor);

        String dateAndAuthor = DateUtils.getRelativeTimeSpanString(
                publishedDate.getTime(),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString() +
                "\n" +
                cursor.getString(ArticleLoader.Query.AUTHOR);

        author.setText(dateAndAuthor);

        String text = cursor.getString(ArticleLoader.Query.BODY);

        final String body = text.replaceAll("([a-zA-Z0-9])\\r\\n", "$1 ");

        final int height = params.height - getPixels(60);
        final int width = displayMetrics.widthPixels - getPixels(40);

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {

                PageSplitter pageSplitter = new PageSplitter(width, height, 1, 0);

                TextPaint textPaint = new TextPaint();
                textPaint.setTextSize(getResources().getDimension(R.dimen.story_text_height));

                pageSplitter.append(body, textPaint);

                TextPagerAdater adapter = new TextPagerAdater(getSupportFragmentManager(), pageSplitter.getPages());

                return adapter;
            }

            @Override
            protected void onPostExecute(Object adapter) {
                super.onPostExecute(adapter);

                cursorLoading = false;
                cursorProgressBar.setVisibility(View.GONE);

                viewPager.setAdapter((TextPagerAdater) adapter);

                fadeIn(viewPager, 200);
            }
        };

        task.execute();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    public int getPixels(int dp) {
        return (int) (dp * displayMetrics.density);
    }

    private Date parsePublishedDate(Cursor cursor) {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class TextPagerAdater extends FragmentPagerAdapter {

        List<CharSequence> pages;

        public TextPagerAdater(FragmentManager fragmentManager, List<CharSequence> pages) {
            super(fragmentManager);
            this.pages = pages;
        }

        @Override
        public Fragment getItem(int position) {
            return TextFragment.create(pages.get(position).toString(), position + 1, getCount());
        }

        @Override
        public int getCount() {
            return pages.size();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(START_POS_EXTRA, startPosition);
        outState.putLongArray(DBIDS_EXTRA, DbIds);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void fadeIn(View layout, int time) {
        layout.setAlpha(0f);
        layout.setVisibility(View.VISIBLE);
        layout.animate()
                .alpha(1f)
                .setDuration(time)
                .setListener(null);

    }
}
