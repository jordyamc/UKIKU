package knf.kuma.tv.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import knf.kuma.tv.TVBaseActivity;

public class TVSearch extends TVBaseActivity {
    public static void start(Context context) {
        context.startActivity(new Intent(context, TVSearch.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addFragment(new TVSearchFragment());
    }
}
