package knf.kuma.seeing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.SeeingObject;

public class SeeingActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    View error;
    private SeeingAdapter adapter;
    private boolean internalMove = false;
    private boolean isFisrt = true;

    public static void open(Context context) {
        context.startActivity(new Intent(context, SeeingActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        ButterKnife.bind(this);
        toolbar.setTitle("Siguiendo");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        adapter = new SeeingAdapter(this);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                final SeeingObject seeingObject = adapter.list.get(position);
                internalMove=true;
                adapter.remove(position);
                final Snackbar snackbar = Snackbar.make(recyclerView, "Anime dropeado", Snackbar.LENGTH_LONG);
                snackbar.setAction("Deshacer", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        error.post(new Runnable() {
                            @Override
                            public void run() {
                                error.setVisibility(View.GONE);
                            }
                        });
                        snackbar.dismiss();
                        internalMove=true;
                        adapter.undo(seeingObject, position);
                    }
                });
                snackbar.show();
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);
        CacheDB.INSTANCE.seeingDAO().getAll().observe(this, new Observer<List<SeeingObject>>() {
            @Override
            public void onChanged(@Nullable List<SeeingObject> list) {
                error.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                if (!internalMove) {
                    adapter.update(list);
                    if (isFisrt) {
                        isFisrt = false;
                        recyclerView.scheduleLayoutAnimation();
                    }
                }else {
                    internalMove=false;
                }
            }
        });
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type","0").equals("0")){
            return R.layout.activity_seening;
        }else {
            return R.layout.activity_seening_grid;
        }
    }
}
