package knf.kuma.animeinfo.viewholders;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityImgFull;
import knf.kuma.animeinfo.AnimePagerAdapter;
import knf.kuma.commons.PicassoSingle;

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
        tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1 && animePagerAdapter != null)
                    animePagerAdapter.onChaptersReselect();
            }
        });
    }

    public void setTitle(final String title) {
        collapsingToolbarLayout.post(new Runnable() {
            @Override
            public void run() {
                collapsingToolbarLayout.setTitle(title);
            }
        });
    }

    public void loadImg(final String link, final View.OnClickListener listener) {
        imageView.post(new Runnable() {
            @Override
            public void run() {
                PicassoSingle.get(imageView.getContext()).load(link).noPlaceholder().into(imageView);
                imageView.setOnClickListener(listener);
            }
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
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setImageResource(isFav ? R.drawable.heart_full : R.drawable.heart_empty);
            }
        });
    }

    public void setFABSeeing() {
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setImageResource(R.drawable.ic_seeing);
            }
        });
    }

    public void showFAB() {
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setEnabled(true);
                fab.setVisibility(View.VISIBLE);
                fab.startAnimation(AnimationUtils.loadAnimation(fab.getContext(), R.anim.scale_up));
            }
        });
    }

    public void hideFAB() {
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setEnabled(false);
                fab.setVisibility(View.INVISIBLE);
                fab.startAnimation(AnimationUtils.loadAnimation(fab.getContext(), R.anim.scale_down));
            }
        });
    }

    public void hideFABForce() {
        fab.post(new Runnable() {
            @Override
            public void run() {
                fab.setEnabled(false);
                fab.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void populate(final AppCompatActivity activity) {
        final String title = intent.getStringExtra("title");
        if (title != null)
            collapsingToolbarLayout.setTitle(title);
        final String img = intent.getStringExtra("img");
        if (img != null) {
            PicassoSingle.get(activity).load(img).into(imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.startActivity(new Intent(activity, ActivityImgFull.class).setData(Uri.parse(img)).putExtra("title", title), ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageView, "img").toBundle());
                }
            });
        }
    }

    public interface Interface {
        void onFabClicked(FloatingActionButton actionButton);

        void onFabLongClicked(FloatingActionButton actionButton);

        void onImgClicked(ImageView imageView);
    }
}
