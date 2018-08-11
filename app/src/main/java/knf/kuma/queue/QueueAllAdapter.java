package knf.kuma.queue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.pojos.QueueObject;

public class QueueAllAdapter extends RecyclerView.Adapter<QueueAllAdapter.AnimeHolder> implements ItemTouchHelperAdapter {

    private OnStartDragListener dragListener;
    private List<QueueObject> list = new ArrayList<>();

    QueueAllAdapter(Activity activity) {
        this.dragListener = (OnStartDragListener) activity;
    }

    @NonNull
    @Override
    public AnimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AnimeHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_full, parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final AnimeHolder holder, int position) {
        final QueueObject object = list.get(position);
        holder.title.setText(object.chapter.name);
        holder.chapter.setText(object.chapter.number);
        holder.state.setImageResource(object.isFile ? R.drawable.ic_queue_file : R.drawable.ic_web);
        holder.dragView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dragListener.onStartDrag(holder);
                }
                return false;
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

    public void update(List<QueueObject> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                long fromTime = list.get(i).time;
                list.get(i).time = list.get(i + 1).time;
                list.get(i + 1).time = fromTime;
                QueueManager.update(list.get(i), list.get(i + 1));
                Collections.swap(list, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                long fromTime = list.get(i).time;
                list.get(i).time = list.get(i - 1).time;
                list.get(i - 1).time = fromTime;
                QueueManager.update(list.get(i), list.get(i - 1));
                Collections.swap(list, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        QueueManager.remove(list.get(position));
        list.remove(position);
        notifyItemRemoved(position);
        if (list.size() == 0)
            dragListener.onListCleared();
    }

    interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder holder);

        void onListCleared();
    }

    class AnimeHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.drag)
        ImageView dragView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.chapter)
        TextView chapter;
        @BindView(R.id.state)
        ImageView state;

        AnimeHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
