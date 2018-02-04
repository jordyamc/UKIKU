package knf.kuma.explorer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;

/**
 * Created by Jordy on 29/01/2018.
 */

public class ExplorerActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;
    private ExplorerPagerAdapter adapter;

    public static void start(Context context) {
        context.startActivity(new Intent(context, ExplorerActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        pager.setOffscreenPageLimit(2);
        adapter = new ExplorerPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public void onBackPressed() {
        if (!((FragmentBase) adapter.getItem(pager.getCurrentItem())).onBackPressed())
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        CacheDB.INSTANCE.explorerDAO().getAll();
        super.onDestroy();
    }
}
