package knf.kuma.directory.viewholders;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.directory.DirPagerAdapter;
import knf.kuma.R;

/**
 * Created by Jordy on 06/01/2018.
 */

public class DirMainFragmentHolder {
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;
    private DirPagerAdapter adapter;

    public DirMainFragmentHolder(View view, FragmentManager manager) {
        ButterKnife.bind(this,view);
        pager.setOffscreenPageLimit(3);
        adapter=new DirPagerAdapter(manager);
        pager.setAdapter(adapter);
        tabLayout.setupWithViewPager(pager);
    }

    public void onChangeOrder(){
        adapter.onChangeOrder();
    }

    public void onReselect(){
        ((BottomFragment)adapter.getItem(pager.getCurrentItem())).onReselect();
    }
}
