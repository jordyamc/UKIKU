package knf.kuma.emision;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;

/**
 * Created by Jordy on 24/01/2018.
 */

public class EmisionActivity extends AppCompatActivity {
    public static void open(Context context){
        context.startActivity(new Intent(context,EmisionActivity.class));
    }

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        pager.setAdapter(new EmisionPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(pager);
        pager.setCurrentItem(getCurrentDay()-1,true);
    }

    private int getCurrentDay(){
        int day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        day--;
        if (day==0)
            return 7;
        else return day;
    }
}
