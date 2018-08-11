package knf.kuma.explorer;

import androidx.fragment.app.Fragment;

public abstract class FragmentBase extends Fragment {

    public FragmentBase() {
    }

    public abstract boolean onBackPressed();
}
