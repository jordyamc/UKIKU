package knf.kuma.animeinfo.viewholders;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;

import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeRelatedAdapter;
import knf.kuma.animeinfo.AnimeTagsAdapter;
import knf.kuma.custom.ExpandableTV;
import knf.kuma.pojos.AnimeObject;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;
import xdroid.toaster.Toaster;

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
    @BindView(R.id.rating_count)
    TextView rating_count;
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
        new Handler(Looper.getMainLooper()).post(() -> {
            title.setText(object.name);
            cardViews.get(0).setOnLongClickListener(view -> {
                try {
                    ClipboardManager clipboard = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Anime title", object.name);
                    clipboard.setPrimaryClip(clip);
                    Toaster.toast("Título copiado");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toaster.toast("Error al copiar título");
                }
                return true;
            });
            showCard(cardViews.get(0));
            if (object.description != null && !object.description.trim().equals("")) {
                desc.setTextAndIndicator(object.description.trim(), expand_icon);
                desc.setAnimationDuration(300);
                View.OnClickListener onClickListener = view -> new Handler(Looper.getMainLooper()).post(() -> {
                    expand_icon.setImageResource(desc.isExpanded() ? R.drawable.action_expand : R.drawable.action_shrink);
                    desc.toggle();
                });
                desc.setOnClickListener(onClickListener);
                expand_icon.setOnClickListener(onClickListener);
                showCard(cardViews.get(1));
            }
            type.setText(object.type);
            state.setText(getStateString(object.state, object.day));
            id.setText(object.aid);
            rating_count.setText(object.rate_count);
            ratingBar.setRating(Float.parseFloat(object.rate_stars));
            showCard(cardViews.get(2));
            if (object.genres.size() != 0) {
                recyclerView_genres.setAdapter(new AnimeTagsAdapter(fragment.getContext(), object.genres));
                showCard(cardViews.get(3));
            }
            if (object.related.size() != 0) {
                recyclerView_related.setAdapter(new AnimeRelatedAdapter(fragment, object.related));
                showCard(cardViews.get(4));
            }
        });
    }

    private void showCard(final CardView view) {
        RETARD += 125;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            view.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.makeInChildBottomAnimation(view.getContext());
            animation.setDuration(250);
            view.startAnimation(animation);
            if (cardViews.indexOf(view) == 1)
                desc.checkIndicator();
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
