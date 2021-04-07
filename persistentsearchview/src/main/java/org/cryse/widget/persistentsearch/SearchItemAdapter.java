package org.cryse.widget.persistentsearch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SearchItemAdapter extends ArrayAdapter<SearchItem> {

    public SearchItemAdapter(Context context, ArrayList<SearchItem> options) {
        super(context, 0, options);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SearchItem searchItem = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.layout_searchitem, parent, false);
        }

        View border = convertView.findViewById(R.id.view_border);
        if (position == 0) {
            border.setVisibility(View.VISIBLE);
        } else {
            border.setVisibility(View.GONE);
        }
        final TextView title = (TextView) convertView
                .findViewById(R.id.textview_title);
        title.setText(searchItem.getTitle());
        ImageView icon = (ImageView) convertView.findViewById(R.id.imageview_icon);
        if(searchItem.getIcon() == null) {
            switch (searchItem.getType()) {
                case SearchItem.TYPE_SEARCH_ITEM_HISTORY:
                    icon.setImageResource(R.drawable.ic_history_black);
                    break;
                default:
                case SearchItem.TYPE_SEARCH_ITEM_SUGGESTION:
                    icon.setImageResource(R.drawable.ic_search_black);
                    break;
            }
        } else {
            icon.setImageDrawable(searchItem.getIcon());
        }
        return convertView;
    }
}