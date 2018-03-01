package knf.kuma.animeinfo;

import android.content.Context;
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
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.pojos.AnimeObject;

public class AnimeRelatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private Context context;
    private Fragment fragment;
    private List<AnimeObject.WebInfo.AnimeRelated> list=new ArrayList<>();
    private AnimeDAO dao= CacheDB.INSTANCE.animeDAO();

    public AnimeRelatedAdapter(Fragment fragment, List<AnimeObject.WebInfo.AnimeRelated> list) {
        this.context=fragment.getContext();
        this.fragment=fragment;
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0:
                return new RelatedHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related, parent, false));
                default:
            case 1:
                return new RelatedNoImgHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_noimg,parent,false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        final AnimeObject.WebInfo.AnimeRelated related=list.get(position);
        if (h.getItemViewType()==0){
            final RelatedHolder holder=(RelatedHolder)h;
            final AnimeObject object=dao.getByLink("%"+related.link);
            PicassoSingle.get(context).load(object.img).into(holder.imageView);
            holder.textView.setText(related.name);
            holder.relation.setText(related.relation);
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityAnime.open(fragment,object,holder.imageView);
                }
            });
        }else {
            final RelatedNoImgHolder holder=(RelatedNoImgHolder) h;
            holder.textView.setText(related.name);
            holder.relation.setText(related.relation);
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityAnime.open(fragment,related);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (dao.existSid(PatternUtil.getLinkNumber(list.get(position).link))?0:1);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class RelatedHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView textView;
        @BindView(R.id.relation)
        TextView relation;

        public RelatedHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }

    class RelatedNoImgHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.title)
        TextView textView;
        @BindView(R.id.relation)
        TextView relation;

        public RelatedNoImgHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
