package knf.kuma.emision;

import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.pojos.AnimeObject;

public class EmisionAdapter extends RecyclerView.Adapter<EmisionAdapter.EmisionItem>{

    public List<AnimeObject> list=new ArrayList<>();
    private Fragment fragment;

    public EmisionAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public EmisionItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EmisionItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emision,parent,false));
    }

    @Override
    public void onBindViewHolder(final EmisionItem holder, int position) {
        final AnimeObject animeObject=list.get(position);
        PicassoSingle.get(fragment.getContext()).load(animeObject.img).into(holder.imageView);
        holder.title.setText(animeObject.name);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityAnime.open(fragment,animeObject,holder.imageView,true,true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<AnimeObject> list){
        this.list=list;
        notifyDataSetChanged();
    }

    public void remove(int position){
        if (position>=0){
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    class EmisionItem extends RecyclerView.ViewHolder{
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;

        EmisionItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
