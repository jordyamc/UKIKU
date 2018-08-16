package knf.kuma.recents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.Network;
import knf.kuma.recents.viewholders.RecyclerRefreshHolder;

public class RecentFragment extends BottomFragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecentsViewModel viewModel;
    private RecyclerRefreshHolder holder;
    private RecentsAdapter adapter;

    @NonNull
    public static RecentFragment get() {
        return new RecentFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(RecentsViewModel.class);
        viewModel.getDBLiveData().observe(this, objects -> {
            if (objects != null) {
                holder.setError(objects.size() == 0);
                holder.setRefreshing(false);
                adapter.updateList(objects);
            }
        });
        updateList();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_refresh_fragment, container, false);
        holder = new RecyclerRefreshHolder(view);
        holder.refreshLayout.setOnRefreshListener(this);
        adapter = new RecentsAdapter(this, holder.recyclerView);
        holder.recyclerView.setAdapter(adapter);
        holder.setRefreshing(true);
        EAHelper.enter1("R");
        return view;
    }

    @Override
    public void onRefresh() {
        updateList();
    }

    private void updateList() {
        if (!Network.isConnected()) {
            holder.setRefreshing(false);
        } else {
            viewModel.reload(getContext());
        }
    }

    @Override
    public void onReselect() {
        EAHelper.enter1("R");
        if (holder != null) holder.scrollToTop();
    }
}
