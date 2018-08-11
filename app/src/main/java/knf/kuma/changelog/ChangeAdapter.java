package knf.kuma.changelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.changelog.objects.Change;
import knf.kuma.changelog.objects.Release;

public class ChangeAdapter extends RecyclerView.Adapter<ChangeAdapter.ChangeItem> {

    private List<Change> changes = new ArrayList<>();

    public ChangeAdapter(Release release) {
        this.changes = release.changes;
    }

    @Override
    public ChangeItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ChangeItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_release, parent, false));
    }

    @Override
    public void onBindViewHolder(ChangeItem holder, int position) {
        Change change = changes.get(position);
        setType(holder.type, change.type);
        holder.description.setText(change.text);
    }

    private void setType(TextView textView, String type) {
        switch (type) {
            case "new":
                textView.setText("Nuevo");
                textView.setBackgroundResource(R.drawable.chip_new);
                break;
            default:
            case "change":
                textView.setText("Cambio");
                textView.setBackgroundResource(R.drawable.chip_change);
                break;
            case "fix":
                textView.setText("Arreglo");
                textView.setBackgroundResource(R.drawable.chip_error);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return changes.size();
    }

    class ChangeItem extends RecyclerView.ViewHolder {
        @BindView(R.id.type)
        TextView type;
        @BindView(R.id.description)
        TextView description;

        public ChangeItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
