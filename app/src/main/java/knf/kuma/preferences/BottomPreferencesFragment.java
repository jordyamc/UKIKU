package knf.kuma.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;

public class BottomPreferencesFragment extends BottomFragment {
    public BottomPreferencesFragment() {
    }

    public static BottomPreferencesFragment get(){
        return new BottomPreferencesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        EAHelper.enter1("C");
        return inflater.inflate(R.layout.fragment_preferences,container,false);
    }

    @Override
    public void onReselect() {
        EAHelper.enter1("C");
    }
}
