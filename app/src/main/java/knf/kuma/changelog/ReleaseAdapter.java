package knf.kuma.changelog;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.changelog.objects.Changelog;
import knf.kuma.changelog.objects.Release;

/**
 * Created by Jordy on 08/02/2018.
 */

public class ReleaseAdapter extends RecyclerView.Adapter<ReleaseAdapter.ReleaseItem> {

    private List<Release> list = new ArrayList<>();

    public ReleaseAdapter(Changelog changelog) {
        this.list = changelog.releases;
    }

    @Override
    public ReleaseItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReleaseItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_changelog, parent, false));
    }

    @Override
    public void onBindViewHolder(ReleaseItem holder, int position) {
        Release release = list.get(position);
        holder.version.setText(release.version);
        holder.code.setText(release.code);
        holder.recyclerView.setAdapter(new ChangeAdapter(release));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ReleaseItem extends RecyclerView.ViewHolder {
        @BindView(R.id.version)
        TextView version;
        @BindView(R.id.code)
        TextView code;
        @BindView(R.id.recycler)
        RecyclerView recyclerView;

        ReleaseItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
