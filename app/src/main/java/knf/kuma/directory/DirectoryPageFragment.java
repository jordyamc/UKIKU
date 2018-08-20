package knf.kuma.directory;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.pojos.AnimeObject;

public class DirectoryPageFragment extends BottomFragment {
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progress;
    private RecyclerView.LayoutManager manager;
    private DirectorypageAdapter adapter;
    private boolean isFirst = true;
    private boolean listUpdated = false;

    public DirectoryPageFragment() {
    }

    public static DirectoryPageFragment get(DirType type) {
        Bundle bundle = new Bundle();
        bundle.putInt("type", type.value);
        DirectoryPageFragment fragment = new DirectoryPageFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        DirectoryViewModel model = ViewModelProviders.of(getActivity()).get(DirectoryViewModel.class);
        adapter = new DirectorypageAdapter(this);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (positionStart == 0)
                    scrollTop();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                if (toPosition == 0)
                    scrollTop();
            }
        });
        getLiveData(model).observe(this, animeObjects -> {
            hideProgress();
            adapter.submitList(animeObjects);
            makeAnimation();
        });
        recyclerView.setAdapter(adapter);
    }

    private LiveData<PagedList<AnimeObject>> getLiveData(DirectoryViewModel model) {
        switch (getArguments().getInt("type", 0)) {
            default:
            case 0:
                return model.getAnimes(getContext());
            case 1:
                return model.getOvas(getContext());
            case 2:
                return model.getMovies(getContext());
        }
    }

    public void onChangeOrder() {
        if (getActivity() != null && adapter != null && recyclerView != null) {
            getLiveData(ViewModelProviders.of(getActivity()).get(DirectoryViewModel.class)).observe(this, animeObjects -> {
                hideProgress();
                listUpdated = true;
                adapter.submitList(animeObjects);
                makeAnimation();
                scrollTop();
            });
        }
    }

    private void hideProgress() {
        progress.post(() -> progress.setVisibility(View.GONE));
    }

    private void makeAnimation() {
        if (isFirst) {
            recyclerView.scheduleLayoutAnimation();
            isFirst = false;
        }
    }

    @UiThread
    private void scrollTop() {
        try {
            recyclerView.smoothScrollToPosition(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        manager = recyclerView.getLayoutManager();
        recyclerView.setLayoutManager(manager);
        isFirst = true;
        return view;
    }

    @LayoutRes
    private int getLayout() {
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lay_type", "0").equals("0")) {
            return R.layout.recycler_dir;
        } else {
            return R.layout.recycler_dir_grid;
        }
    }

    @Override
    public void onReselect() {
        if (recyclerView != null && manager != null)
            manager.smoothScrollToPosition(recyclerView, null, 0);
    }

    enum DirType {
        ANIMES(0),
        OVAS(1),
        MOVIES(2);
        public int value;

        DirType(int value) {
            this.value = value;
        }
    }
}
