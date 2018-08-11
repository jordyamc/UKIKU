package knf.kuma.animeinfo.viewholders;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityImgFull;
import knf.kuma.animeinfo.AnimePagerAdapter;
import knf.kuma.commons.PicassoSingle;
import xdroid.toaster.Toaster;

import static knf.kuma.commons.BypassUtil.clearCookies;
import static knf.kuma.commons.BypassUtil.isLoading;
import static knf.kuma.commons.BypassUtil.isNeeded;
import static knf.kuma.commons.BypassUtil.saveCookies;
import static knf.kuma.commons.BypassUtil.userAgent;

public class AnimeActivityHolder {
    @BindView(R.id.appBar)
    public AppBarLayout appBarLayout;
    @BindView(R.id.collapsingToolbar)
    public CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.img)
    public ImageView imageView;
    @BindView(R.id.toolbar)
    public Toolbar toolbar;
    @BindView(R.id.tabs)
    public TabLayout tabLayout;
    @BindView(R.id.pager)
    public ViewPager pager;
    @BindView(R.id.fab)
    public FloatingActionButton fab;
    @BindView(R.id.webview)
    WebView webView;

    private Intent intent;
    private AnimePagerAdapter animePagerAdapter;
    private Interface innerInterface;

    public AnimeActivityHolder(AppCompatActivity activity) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        fab.setVisibility(View.INVISIBLE);
        innerInterface = (Interface) activity;
        intent = activity.getIntent();
        populate(activity);
        pager.setOffscreenPageLimit(2);
        animePagerAdapter = new AnimePagerAdapter(activity.getSupportFragmentManager());
        pager.setAdapter(animePagerAdapter);
        tabLayout.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                appBarLayout.setExpanded(position == 0, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (activity.getIntent().getBooleanExtra("isRecord", false))
            pager.setCurrentItem(1, true);
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1 && animePagerAdapter != null)
                    animePagerAdapter.onChaptersReselect();
            }
        });
    }

    public void setTitle(final String title) {
        collapsingToolbarLayout.post(() -> collapsingToolbarLayout.setTitle(title));
    }

    public void loadImg(final String link, final View.OnClickListener listener) {
        imageView.post(() -> {
            PicassoSingle.get(imageView.getContext()).load(link).noPlaceholder().into(imageView);
            imageView.setOnClickListener(listener);
        });
    }

    @OnClick(R.id.fab)
    void onFabClick(FloatingActionButton actionButton) {
        innerInterface.onFabClicked(actionButton);
    }

    @OnLongClick(R.id.fab)
    boolean onFabLongClick(FloatingActionButton actionButton) {
        innerInterface.onFabLongClicked(actionButton);
        return true;
    }

    @OnClick(R.id.img)
    void onImgClick(ImageView imageView) {
        innerInterface.onImgClicked(imageView);
    }

    public void setFABState(final boolean isFav) {
        fab.post(() -> fab.setImageResource(isFav ? R.drawable.heart_full : R.drawable.heart_empty));
    }

    public void setFABSeeing() {
        fab.post(() -> fab.setImageResource(R.drawable.ic_seeing));
    }

    public void setFABState(final boolean isFav, final boolean isSeeing) {
        fab.post(() -> {
            if (isFav && isSeeing)
                fab.setImageResource(R.drawable.ic_star_heart);
            else if (isSeeing)
                fab.setImageResource(R.drawable.ic_seeing);
            else if (isFav)
                fab.setImageResource(R.drawable.heart_full);
            else
                fab.setImageResource(R.drawable.heart_empty);
        });
    }

    public void showFAB() {
        fab.post(() -> {
            fab.setEnabled(true);
            fab.setVisibility(View.VISIBLE);
            fab.startAnimation(AnimationUtils.loadAnimation(fab.getContext(), R.anim.scale_up));
        });
    }

    public void hideFAB() {
        fab.post(() -> {
            fab.setEnabled(false);
            fab.setVisibility(View.INVISIBLE);
            fab.startAnimation(AnimationUtils.loadAnimation(fab.getContext(), R.anim.scale_down));
        });
    }

    public void hideFABForce() {
        fab.post(() -> {
            fab.setEnabled(false);
            fab.setVisibility(View.INVISIBLE);
        });
    }

    private void populate(final AppCompatActivity activity) {
        final String title = intent.getStringExtra("title");
        if (title != null)
            collapsingToolbarLayout.setTitle(title);
        final String img = intent.getStringExtra("img");
        if (img != null) {
            PicassoSingle.get(activity).load(img).into(imageView);
            imageView.setOnClickListener(v -> activity.startActivity(new Intent(activity, ActivityImgFull.class).setData(Uri.parse(img)).putExtra("title", title), ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageView, "img").toBundle()));
        }
    }

    public void checkBypass(Context context) {
        AsyncTask.execute(() -> {
            if (isNeeded(context) && !isLoading) {
                isLoading = true;
                Log.e("CloudflareBypass", "is needed");
                clearCookies();
                new Handler(Looper.getMainLooper()).post(() -> {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                            Log.e("CloudflareBypass", "Override " + url);
                            if (url.equals("https://animeflv.net/")) {
                                Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"));
                                saveCookies(context);
                                Toaster.toast("Bypass actualizado");
                                PicassoSingle.clear();
                                innerInterface.onNeedRecreate();
                            }
                            isLoading = false;
                            return false;
                        }
                    });
                    webView.getSettings().setUserAgentString(userAgent);
                    webView.loadUrl("https://animeflv.net/");
                });
            } else {
                Log.e("CloudflareBypass", "Not needed");
            }
        });
    }

    public interface Interface {
        void onFabClicked(FloatingActionButton actionButton);

        void onFabLongClicked(FloatingActionButton actionButton);

        void onImgClicked(ImageView imageView);

        void onNeedRecreate();
    }
}
