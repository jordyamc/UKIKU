package knf.kuma.directory;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class DirPagerAdapter extends FragmentPagerAdapter {

    private DirectoryPageFragment animes=DirectoryPageFragment.get(DirectoryPageFragment.DirType.ANIMES);
    private DirectoryPageFragment ovas=DirectoryPageFragment.get(DirectoryPageFragment.DirType.OVAS);
    private DirectoryPageFragment movies=DirectoryPageFragment.get(DirectoryPageFragment.DirType.MOVIES);

    public DirPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            default:
            case 0:
                return "ANIME";
            case 1:
                return "OVA";
            case 2:
                return "PELICULA";
        }
    }

    public void onChangeOrder(){
        animes.onChangeOrder();
        ovas.onChangeOrder();
        movies.onChangeOrder();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            default:
            case 0:
                return animes;
            case 1:
                return ovas;
            case 2:
                return movies;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
