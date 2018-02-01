package knf.kuma.emision;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import knf.kuma.pojos.AnimeObject;

/**
 * Created by Jordy on 24/01/2018.
 */

public class EmisionPagerAdapter extends FragmentPagerAdapter {

    private EmisionFragment monday=EmisionFragment.get(AnimeObject.Day.MONDAY);
    private EmisionFragment tuesday=EmisionFragment.get(AnimeObject.Day.TUESDAY);
    private EmisionFragment wednesday=EmisionFragment.get(AnimeObject.Day.WEDNESDAY);
    private EmisionFragment thursday=EmisionFragment.get(AnimeObject.Day.THURSDAY);
    private EmisionFragment friday=EmisionFragment.get(AnimeObject.Day.FRIDAY);
    private EmisionFragment saturday=EmisionFragment.get(AnimeObject.Day.SATURDAY);
    private EmisionFragment sunday=EmisionFragment.get(AnimeObject.Day.SUNDAY);

    EmisionPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return 7;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            default:
            case 0:
                return "Lunes";
            case 1:
                return "Martes";
            case 2:
                return "Miércoles";
            case 3:
                return "Jueves";
            case 4:
                return "Viernes";
            case 5:
                return "Sábado";
            case 6:
                return "Domingo";
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            default:
            case 0:
                return monday;
            case 1:
                return tuesday;
            case 2:
                return wednesday;
            case 3:
                return thursday;
            case 4:
                return friday;
            case 5:
                return saturday;
            case 6:
                return sunday;
        }
    }
}
