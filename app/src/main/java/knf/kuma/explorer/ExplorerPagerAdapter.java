package knf.kuma.explorer;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Jordy on 29/01/2018.
 */

public class ExplorerPagerAdapter extends FragmentPagerAdapter {

    private FragmentFilesRoot filesRoot=FragmentFilesRoot.get();

    public ExplorerPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return "Archivos";
        /*switch (position){
            default:
            case 0:
                return "Archivos";
            case 1:
                return "Descargas";
        }*/
    }

    @Override
    public Fragment getItem(int position) {
        return filesRoot;
    }
}
