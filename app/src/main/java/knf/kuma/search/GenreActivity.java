package knf.kuma.search;

import android.arch.lifecycle.Observer;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;

public class GenreActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    private GenreAdapter adapter;
    private boolean isFirst = true;

    public static void open(Context context, String name) {
        Intent intent = new Intent(context, GenreActivity.class);
        intent.putExtra("name", name);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_genre);
        ButterKnife.bind(this);
        toolbar.setTitle(getIntent().getStringExtra("name"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down));
        adapter = new GenreAdapter(this);
        recyclerView.setAdapter(adapter);
        new LivePagedListBuilder<>(CacheDB.INSTANCE.animeDAO().getAllGenre("%" + getIntent().getStringExtra("name") + "%"), 25).build().observe(this, new Observer<PagedList<AnimeObject>>() {
            @Override
            public void onChanged(@Nullable PagedList<AnimeObject> animeObjects) {
                adapter.submitList(animeObjects);
                if (isFirst) {
                    progressBar.setVisibility(View.GONE);
                    isFirst = false;
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });
    }
}
