package knf.kuma.explorer;

import android.support.v4.app.Fragment;

/**
 * Created by Jordy on 30/01/2018.
 */

public abstract class FragmentBase extends Fragment {
    public FragmentBase() {
    }

    /**Abort*/
    public abstract boolean onBackPressed();
}
