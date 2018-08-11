package knf.kuma.explorer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.EAHelper;

public class ExplorerActivity extends AppCompatActivity implements OnFileStateChange {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager pager;
    private ExplorerPagerAdapter adapter;
    private boolean isExplorerFiles = true;

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
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        if (savedInstanceState == null)
            ExplorerCreator.onDestroy();
        pager.setOffscreenPageLimit(2);
        adapter = new ExplorerPagerAdapter(this, getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer_connected, menu);
        if (isExplorerFiles)
            menu.findItem(R.id.delete_all).setVisible(false);
        if (!CastUtil.get().connected())
            menu.findItem(R.id.casting).setVisible(false);
        else {
            CastUtil.get().getCasting().observe(this, s -> {
                try {
                    if (s.equals(CastUtil.NO_PLAYING)) {
                        menu.findItem(R.id.casting).setEnabled(false);
                    } else {
                        menu.findItem(R.id.casting).setEnabled(true);
                        menu.findItem(R.id.casting).setOnMenuItemClickListener(item -> {
                            CastUtil.get().openControls();
                            return true;
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                adapter.onRemoveAllClicked();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onChange(boolean isFile) {
        isExplorerFiles = isFile;
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (adapter == null || !((FragmentBase) adapter.getItem(pager.getCurrentItem())).onBackPressed())
            super.onBackPressed();
    }
}
