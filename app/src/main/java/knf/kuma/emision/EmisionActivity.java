package knf.kuma.emision;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;

public class EmisionActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;
    private EmisionPagerAdapter pagerAdapter;

    public static void open(Activity context) {
        context.startActivityForResult(new Intent(context, EmisionActivity.class), 4987);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emision);
        ButterKnife.bind(this);
        toolbar.setTitle("Emisión");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        pager.setOffscreenPageLimit(7);
        pagerAdapter = new EmisionPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(this);
        pager.setCurrentItem(getCurrentDay()-1,true);
        EAHelper.clear2();
    }

    private int getCurrentDay(){
        int day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        day--;
        if (day==0)
            return 7;
        else return day;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_emision, menu);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden", false))
            menu.findItem(R.id.action_hideshow).setIcon(R.drawable.ic_hide);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean show = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden", false);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_hidden", !show).apply();
        supportInvalidateOptionsMenu();
        if (pagerAdapter != null)
            pagerAdapter.reloadPages();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        EAHelper.enter2(String.valueOf(getDayByPos(tab.getPosition())));
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        EAHelper.enter2(String.valueOf(getDayByPos(tab.getPosition())));
    }

    private int getDayByPos(int pos) {
        pos += 2;
        if (pos == 8)
            pos = 1;
        return pos;
    }
}
