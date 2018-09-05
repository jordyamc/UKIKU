package knf.kuma.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.paging.LivePagedListBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;

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
        toolbar.setNavigationOnClickListener(v -> finish());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down));
        adapter = new GenreAdapter(this);
        recyclerView.setAdapter(adapter);
        new LivePagedListBuilder<>(CacheDB.INSTANCE.animeDAO().getAllGenre("%" + getIntent().getStringExtra("name") + "%"), 25).build().observe(this, animeObjects -> {
            adapter.submitList(animeObjects);
            if (isFirst) {
                progressBar.setVisibility(View.GONE);
                isFirst = false;
                recyclerView.scheduleLayoutAnimation();
            }
        });
    }
}
