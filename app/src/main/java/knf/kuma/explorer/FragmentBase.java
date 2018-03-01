package knf.kuma.explorer;

import android.support.v4.app.Fragment;

public abstract class FragmentBase extends Fragment {
    public FragmentBase() {
    }

    public abstract boolean onBackPressed();
}
