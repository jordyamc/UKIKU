package knf.kuma.queue;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;

public class QueueActivity extends AppCompatActivity implements QueueAnimesAdapter.OnAnimeSelectedListener {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.list_toolbar)
    Toolbar list_toolbar;
    @BindView(R.id.list_recycler)
    RecyclerView list_recyclerView;
    @BindView(R.id.bottom_card)
    CardView cardView;
    @BindView(R.id.error)
    View error_view;
    BottomSheetBehavior<CardView> bottomSheetBehavior;
    private QueueAnimesAdapter animesAdapter;
    private QueueListAdapter listAdapter;

    private QueueObject current;

    public static void open(Context context) {
        context.startActivity(new Intent(context, QueueActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        ButterKnife.bind(this);
        toolbar.setTitle("Pendientes");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getMenuInflater().inflate(R.menu.menu_play_queue, list_toolbar.getMenu());
        list_toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.play:
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        QueueManager.startQueue(getApplicationContext(), listAdapter.getList());
                        break;
                    case R.id.clear:
                        new MaterialDialog.Builder(QueueActivity.this)
                                .content("¿Remover los episodios pendientes?")
                                .positiveText("remover")
                                .negativeText("cancelar")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                        QueueManager.remove(listAdapter.getList());
                                    }
                                }).build().show();
                        break;
                }
                return true;
            }
        });
        bottomSheetBehavior = BottomSheetBehavior.from(cardView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    current = null;
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        list_recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
        animesAdapter = new QueueAnimesAdapter(this);
        listAdapter = new QueueListAdapter();
        recyclerView.setAdapter(animesAdapter);
        list_recyclerView.setAdapter(listAdapter);
        reload();
    }

    private void reload() {
        CacheDB.INSTANCE.queueDAO().getAll().observe(this, new Observer<List<QueueObject>>() {
            @Override
            public void onChanged(@Nullable List<QueueObject> list) {
                error_view.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                animesAdapter.update(QueueObject.getOne(list));
            }
        });
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0").equals("0")) {
            return R.layout.activity_queue;
        } else {
            return R.layout.activity_queue_grid;
        }
    }

    @Override
    public void onSelect(final QueueObject object) {
        if (object.equals(current)) {
            current = null;
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            current = object;
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            list_toolbar.setTitle(object.chapter.name);
            CacheDB.INSTANCE.queueDAO().getByAid(object.chapter.aid).observe(this, new Observer<List<QueueObject>>() {
                @Override
                public void onChanged(@Nullable List<QueueObject> list) {
                    if (list.size() == 0)
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    else {
                        listAdapter.update(object.chapter.aid, list);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info_queue, menu);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("is_queue_info_shown", false)) {
            preferences.edit().putBoolean("is_queue_info_shown", true).apply();
            onOptionsItemSelected(menu.findItem(R.id.info));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                new MaterialDialog.Builder(this)
                        .content("Los episodios añadidos desde servidor podrían dejar de funcionar después de días sin reproducir")
                        .positiveText("OK")
                        .build().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        else
            super.onBackPressed();
    }
}
