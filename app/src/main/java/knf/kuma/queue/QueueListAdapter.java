package knf.kuma.queue;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;

public class QueueListAdapter extends RecyclerView.Adapter<QueueListAdapter.ListItemHolder> {
    private String current = "0000";
    private List<QueueObject> list = new ArrayList<>();

    @NonNull
    @Override
    public ListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ListItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ListItemHolder holder, int position) {
        QueueObject object = list.get(position);
        holder.chapter.setText(object.chapter.number);
        holder.icon.setImageResource(object.isFile ? R.drawable.ic_chap_down : R.drawable.ic_web);
        holder.action_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public List<QueueObject> getList() {
        return list;
    }

    public void update(String aid, List<QueueObject> list) {
        if (!current.equals(aid)) {
            current = aid;
            this.list = list;
            notifyDataSetChanged();
        }
    }

    public void remove(int position) {
        CacheDB.INSTANCE.queueDAO().remove(list.get(position));
        list.remove(position);
        notifyItemRemoved(position);
    }

    class ListItemHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.action_delete)
        ImageButton action_delete;

        ListItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
