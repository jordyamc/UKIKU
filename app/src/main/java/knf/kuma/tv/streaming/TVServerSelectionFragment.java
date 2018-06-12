package knf.kuma.tv.streaming;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import java.util.ArrayList;
import java.util.List;

public class TVServerSelectionFragment extends GuidedStepSupportFragment {

    public static final String VIDEO_DATA = "option_data";
    public static final String SERVERS_DATA = "list_data";
    public static final String IS_SERVER_DATA = "is_server";

    public TVServerSelectionFragment() {
    }

    public static TVServerSelectionFragment get(ArrayList<String> servers, String name, boolean isServerData) {
        TVServerSelectionFragment fragment = new TVServerSelectionFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SERVERS_DATA, servers);
        bundle.putBoolean(IS_SERVER_DATA, isServerData);
        bundle.putString("server_name", name);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setResult(Activity.RESULT_CANCELED, new Intent()
                .putExtra("is_video_server", getArguments().getBoolean(IS_SERVER_DATA, false)));
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        if (getArguments().getBoolean(IS_SERVER_DATA, false))
            return new GuidanceStylist.Guidance(getArguments().getString("server_name"), "Selecciona calidad", "", null);
        else
            return new GuidanceStylist.Guidance("Selecciona servidor", "", "", null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        List<String> list = getArguments().getStringArrayList(SERVERS_DATA);
        int id = 0;
        for (String name : list) {
            if (!name.equals("Mega"))
                actions.add(new GuidedAction.Builder(getContext())
                        .id(id)
                        .title(name)
                        .build());
            id++;
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);
        getActivity().setResult(Activity.RESULT_OK, new Intent()
                .putExtra("is_video_server", getArguments().getBoolean(IS_SERVER_DATA, false))
                .putExtra("position", (int) action.getId()));
        getActivity().finish();
    }


}
