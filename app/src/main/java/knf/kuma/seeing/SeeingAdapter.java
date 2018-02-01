package knf.kuma.seeing;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.SeeingObject;

/**
 * Created by Jordy on 23/01/2018.
 */

public class SeeingAdapter extends RecyclerView.Adapter<SeeingAdapter.SeeingItem>{

    private Activity activity;
    private SeeingDAO seeingDAO= CacheDB.INSTANCE.seeingDAO();
    public List<SeeingObject> list=new ArrayList<>();

    public SeeingAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public SeeingItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SeeingItem(LayoutInflater.from(parent.getContext()).inflate(getLayout(),parent,false));
    }

    @LayoutRes
    private int getLayout(){
        if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type","0").equals("0")){
            return R.layout.item_record;
        }else {
            return R.layout.item_record_grid;
        }
    }

    @Override
    public void onBindViewHolder(final SeeingItem holder, int position) {
        final SeeingObject seeingObject=list.get(position);
        final AnimeObject.WebInfo.AnimeChapter lastChapter=seeingObject.lastChapter;
        PicassoSingle.get(activity).load(seeingObject.img).into(holder.imageView);
        holder.title.setText(seeingObject.title);
        holder.chapter.setText(lastChapter==null?"No empezado":lastChapter.number);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityAnime.open(activity,seeingObject,holder.imageView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void update(List<SeeingObject> list){
        this.list=list;
        notifyDataSetChanged();
    }

    public void undo(SeeingObject object,int position){
        seeingDAO.add(object);
        list.add(position,object);
        notifyItemInserted(position);
    }

    public void remove(int position){
        seeingDAO.remove(list.get(position));
        list.remove(position);
        notifyItemRemoved(position);
    }
    class SeeingItem extends RecyclerView.ViewHolder{
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.img)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.chapter)
        TextView chapter;
        SeeingItem(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
