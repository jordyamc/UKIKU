package knf.kuma.explorer;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.EAHelper;

public class ExplorerActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;
    private ExplorerPagerAdapter adapter;

    public static void open(Context context) {
        context.startActivity(new Intent(context, ExplorerActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        ButterKnife.bind(this);
        toolbar.setTitle("Explorador");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        if (savedInstanceState == null)
            ExplorerCreator.onDestroy();
        pager.setOffscreenPageLimit(2);
        adapter = new ExplorerPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (CastUtil.get().connected()) {
            getMenuInflater().inflate(R.menu.menu_explorer_connected, menu);
            CastUtil.get().getCasting().observe(this, new Observer<String>() {
                @Override
                public void onChanged(@Nullable String s) {
                    try {
                        if (s.equals(CastUtil.NO_PLAYING)) {
                            menu.findItem(R.id.casting).setEnabled(false);
                        } else {
                            menu.findItem(R.id.casting).setEnabled(true);
                            menu.findItem(R.id.casting).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    CastUtil.get().openControls();
                                    return true;
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onBackPressed() {
        if (adapter == null || !((FragmentBase) adapter.getItem(pager.getCurrentItem())).onBackPressed())
            super.onBackPressed();
    }
}
