package knf.kuma.animeinfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.search.GenreActivity;

public class AnimeTagsAdapter extends RecyclerView.Adapter<AnimeTagsAdapter.TagHolder>{

    private Context context;
    private List<String> list;

    public AnimeTagsAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public TagHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull final TagHolder holder, int position) {
        holder.chip.setText(list.get(position));
        holder.chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GenreActivity.open(context, list.get(holder.getAdapterPosition()));
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class TagHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.chip)
        TextView chip;

        TagHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
