package knf.kuma.queue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;
import xdroid.toaster.Toaster;

public class QueueActivity extends AppCompatActivity implements QueueAnimesAdapter.OnAnimeSelectedListener, QueueAllAdapter.OnStartDragListener {
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
    private QueueListAdapter listAdapter;

    private ItemTouchHelper mItemTouchHelper;

    private QueueObject current;

    private LiveData<List<QueueObject>> currentData = new MutableLiveData<>();

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
        setLayoutManager(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true));
        list_recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
        listAdapter = new QueueListAdapter();
        list_recyclerView.setAdapter(listAdapter);
        reload();
    }

    private void reload() {
        currentData.removeObservers(this);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true)) {
            currentData = CacheDB.INSTANCE.queueDAO().getAll();
            currentData.observe(this, new Observer<List<QueueObject>>() {
                @Override
                public void onChanged(@Nullable List<QueueObject> list) {
                    error_view.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                    QueueAnimesAdapter animesAdapter = new QueueAnimesAdapter(QueueActivity.this);
                    recyclerView.setAdapter(animesAdapter);
                    dettachHelper();
                    mItemTouchHelper = new ItemTouchHelper(new NoTouchHelperCallback());
                    mItemTouchHelper.attachToRecyclerView(recyclerView);
                    animesAdapter.update(QueueObject.getOne(list));
                }
            });
        } else {
            currentData = CacheDB.INSTANCE.queueDAO().getAllAsort();
            currentData.observe(this, new Observer<List<QueueObject>>() {
                @Override
                public void onChanged(@Nullable List<QueueObject> list) {
                    clearInterfaces();
                    error_view.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                    QueueAllAdapter allAdapter = new QueueAllAdapter(QueueActivity.this);
                    recyclerView.setAdapter(allAdapter);
                    dettachHelper();
                    mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(allAdapter));
                    mItemTouchHelper.attachToRecyclerView(recyclerView);
                    allAdapter.update(list);
                    currentData.removeObserver(this);
                }
            });
        }
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0").equals("0")) {
            return R.layout.activity_queue;
        } else {
            return R.layout.activity_queue_grid;
        }
    }

    private void dettachHelper() {
        if (mItemTouchHelper != null)
            mItemTouchHelper.attachToRecyclerView(null);
    }

    private void clearInterfaces() {
        if (recyclerView.getAdapter() instanceof QueueAnimesAdapter)
            ((QueueAnimesAdapter) recyclerView.getAdapter()).clear();
    }

    private void setLayoutManager(boolean isFull) {
        if (isFull || PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0").equals("0")) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.span_count)));
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.grid_fall_down));
        }
    }

    @Override
    public void onSelect(final QueueObject object) {
        if (object.equalsAnime(current)) {
            current = null;
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            list_toolbar.setTitle(object.chapter.name);
            final LiveData<List<QueueObject>> liveData = CacheDB.INSTANCE.queueDAO().getByAid(object.chapter.aid);
            liveData.observe(this, new Observer<List<QueueObject>>() {
                @Override
                public void onChanged(@Nullable List<QueueObject> list) {
                    if (list.size() == 0)
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    else {
                        listAdapter.update(object.chapter.aid, list);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    current = object;
                    liveData.removeObserver(this);
                }
            });
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder holder) {
        mItemTouchHelper.startDrag(holder);
    }

    @Override
    public void onListCleared() {
        error_view.post(new Runnable() {
            @Override
            public void run() {
                error_view.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("queue_is_grouped", true))
            getMenuInflater().inflate(R.menu.menu_queue_group, menu);
        else
            getMenuInflater().inflate(R.menu.menu_queue_list, menu);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("is_queue_info_shown", false)) {
            preferences.edit().putBoolean("is_queue_info_shown", true).apply();
            onOptionsItemSelected(menu.findItem(R.id.info));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        switch (item.getItemId()) {
            case R.id.info:
                new MaterialDialog.Builder(this)
                        .content("Los episodios añadidos desde servidor podrían dejar de funcionar después de días sin reproducir")
                        .positiveText("OK")
                        .build().show();
                break;
            case R.id.queue_group:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("queue_is_grouped", true).apply();
                setLayoutManager(false);
                reload();
                supportInvalidateOptionsMenu();
                break;
            case R.id.queue_list:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("queue_is_grouped", false).apply();
                setLayoutManager(true);
                reload();
                supportInvalidateOptionsMenu();
                break;
            case R.id.play:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                List<QueueObject> list = ((QueueAllAdapter) recyclerView.getAdapter()).getList();
                if (list.size() > 0)
                    QueueManager.startQueue(getApplicationContext(), list);
                else
                    Toaster.toast("La lista esta vacia");
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
