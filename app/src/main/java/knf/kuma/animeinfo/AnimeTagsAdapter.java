package knf.kuma.animeinfo;

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

/**
 * Created by Jordy on 05/01/2018.
 */

public class AnimeTagsAdapter extends RecyclerView.Adapter<AnimeTagsAdapter.TagHolder>{

    private List<String> list=new ArrayList<>();

    public AnimeTagsAdapter(List<String> list) {
        this.list = list;
    }

    @Override
    public TagHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip,parent,false));
    }

    @Override
    public void onBindViewHolder(TagHolder holder, int position) {
        holder.chip.setText(list.get(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class TagHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.chip)
        TextView chip;
        public TagHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
