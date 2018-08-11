package knf.kuma.random;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;

public class RandomActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.refresh)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    private RandomAdapter adapter;

    public static void open(Context context) {
        context.startActivity(new Intent(context, RandomActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        ButterKnife.bind(this);
        toolbar.setTitle("Random");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        refreshLayout.setOnRefreshListener(this);
        adapter = new RandomAdapter(this);
        recyclerView.setAdapter(adapter);
        refreshLayout.setRefreshing(true);
        refreshList();
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0").equals("0")) {
            return R.layout.recycler_refresh;
        } else {
            return R.layout.recycler_refresh_grid;
        }
    }

    private void refreshList() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CacheDB.INSTANCE.animeDAO().getRandom(PreferenceManager.getDefaultSharedPreferences(RandomActivity.this).getInt("random_limit", 25))
                        .observe(RandomActivity.this, new Observer<List<AnimeObject>>() {
                            @Override
                            public void onChanged(@Nullable List<AnimeObject> animeObjects) {
                                refreshLayout.setRefreshing(false);
                                adapter.update(animeObjects);
                                recyclerView.scheduleLayoutAnimation();
                            }
                        });
            }
        }, 1200);
    }

    @Override
    public void onRefresh() {
        refreshList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_random, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final MaterialNumberPicker picker = new MaterialNumberPicker(
                this, 5, 100,
                PreferenceManager.getDefaultSharedPreferences(this).getInt("random_limit", 25),
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.textPrimary),
                getResources().getDimensionPixelSize(R.dimen.num_picker));
        new MaterialDialog.Builder(this)
                .title("Numero de resultados")
                .customView(picker, false)
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        PreferenceManager.getDefaultSharedPreferences(RandomActivity.this).edit().putInt("random_limit", picker.getValue()).apply();
                        refreshLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshLayout.setRefreshing(true);
                            }
                        });
                        refreshList();
                    }
                }).build().show();
        return super.onOptionsItemSelected(item);
    }
}
