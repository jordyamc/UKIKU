package knf.kuma.widgets.emision;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;

public class WEListProvider implements RemoteViewsService.RemoteViewsFactory {
    private ArrayList<WEListItem> items = new ArrayList<>();
    private Context context;

    WEListProvider(Context context) {
        this.context = context;
        populateListItem();
    }

    private void populateListItem() {
        items.clear();
        List<AnimeObject> list = CacheDB.INSTANCE.animeDAO().getByDayDirect(getActualDayCode(), getBlacklist(context));
        for (AnimeObject obj : list) {
            items.add(new WEListItem(obj.key, obj.link, obj.name, obj.aid, obj.img));
        }
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        populateListItem();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.item_widget_list);
        try {
            WEListItem listItem = items.get(position);
            remoteView.setTextViewText(R.id.heading, listItem.title);
            remoteView.setTextColor(R.id.heading, getColor(true));
            Intent clickIntent = ActivityAnime.getSimpleIntent(context, listItem);
            remoteView.setOnClickFillInIntent(R.id.linear, clickIntent);
            remoteView.setInt(R.id.linear, "setBackgroundColor", getColor(false));
        } catch (Exception e) {
            //e
        }
        return remoteView;
    }

    private int getColor(boolean isText) {
        String type = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_value", "0");
        switch (type) {
            default:
            case "1":
                if (isText) return Color.parseColor("#323232");
                else return Color.parseColor("#FFFFFFFF");
            case "2":
                if (isText) return Color.parseColor("#bebebe");
                else return Color.parseColor("#FF424242");
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).key;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private int getActualDayCode() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return 2;
            case Calendar.TUESDAY:
                return 3;
            case Calendar.WEDNESDAY:
                return 4;
            case Calendar.THURSDAY:
                return 5;
            case Calendar.FRIDAY:
                return 6;
            case Calendar.SATURDAY:
                return 7;
            case Calendar.SUNDAY:
                return 1;
            default:
                return 2;
        }
    }

    private Set<String> getBlacklist(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", new LinkedHashSet<String>());
    }

}
