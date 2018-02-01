package knf.kuma.animeinfo.viewholders;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeRelatedAdapter;
import knf.kuma.animeinfo.AnimeTagsAdapter;
import knf.kuma.custom.ExpandableTV;
import knf.kuma.pojos.AnimeObject;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * Created by Jordy on 05/01/2018.
 */

public class AnimeDetailsHolder {
    @BindViews({R.id.card_title, R.id.card_desc, R.id.card_details, R.id.card_genres, R.id.card_related})
    List<CardView> cardViews;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.expand_icon)
    ImageButton expand_icon;
    @BindView(R.id.expandable_desc)
    ExpandableTV desc;
    @BindView(R.id.type)
    TextView type;
    @BindView(R.id.state)
    TextView state;
    @BindView(R.id.aid)
    TextView id;
    @BindView(R.id.ratingBar)
    MaterialRatingBar ratingBar;
    @BindView(R.id.recycler_genres)
    RecyclerView recyclerView_genres;
    @BindView(R.id.recycler_related)
    RecyclerView recyclerView_related;
    private int RETARD = 0;

    public AnimeDetailsHolder(View view) {
        ButterKnife.bind(this, view);
        recyclerView_genres.setLayoutManager(ChipsLayoutManager.newBuilder(view.getContext()).build());
        recyclerView_genres.addItemDecoration(new SpacingItemDecoration(5, 5));
        recyclerView_related.setLayoutManager(new LinearLayoutManager(view.getContext()));
    }

    public void populate(final Fragment fragment, final AnimeObject object) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                title.setText(object.name);
                showCard(cardViews.get(0));
                if (object.description != null && !object.description.trim().equals("")) {
                    desc.setIndicator(expand_icon);
                    desc.setText(object.description.trim());
                    desc.setAnimationDuration(300);
                    desc.setExpandInterpolator(new LinearInterpolator());
                    desc.setCollapseInterpolator(new LinearInterpolator());
                    View.OnClickListener onClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            expand_icon.post(new Runnable() {
                                @Override
                                public void run() {
                                    expand_icon.setImageResource(desc.isExpanded() ? R.drawable.action_expand : R.drawable.action_shrink);
                                    desc.toggle();
                                }
                            });
                        }
                    };
                    desc.setOnClickListener(onClickListener);
                    expand_icon.setOnClickListener(onClickListener);
                    showCard(cardViews.get(1));
                }
                type.setText(object.type);
                state.setText(getStateString(object.state, object.day));
                id.setText(object.aid);
                ratingBar.setRating(Float.parseFloat(object.rate_stars));
                showCard(cardViews.get(2));
                if (object.genres.size() != 0) {
                    recyclerView_genres.setAdapter(new AnimeTagsAdapter(object.genres));
                    showCard(cardViews.get(3));
                }
                if (object.related.size() != 0) {
                    recyclerView_related.setAdapter(new AnimeRelatedAdapter(fragment, object.related));
                    showCard(cardViews.get(4));
                }
            }
        });
    }

    private void showCard(final View view) {
        RETARD += 125;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
                if (desc.getLayout() != null) {
                    if (desc.getLineCount() <= 4)
                        expand_icon.setVisibility(View.GONE);
                }
                Animation animation = AnimationUtils.makeInChildBottomAnimation(view.getContext());
                animation.setDuration(250);
                view.startAnimation(animation);
            }
        }, RETARD);
    }

    private String getStateString(String state, AnimeObject.Day day) {
        switch (day) {
            case MONDAY:
                return state + " - Lunes";
            case TUESDAY:
                return state + " - Martes";
            case WEDNESDAY:
                return state + " - Miércoles";
            case THURSDAY:
                return state + " - Jueves";
            case FRIDAY:
                return state + " - Viernes";
            case SATURDAY:
                return state + " - Sábado";
            case SUNDAY:
                return state + " - Domingo";
            default:
                return state;
        }
    }
}
