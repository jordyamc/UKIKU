package knf.kuma.explorer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class ExplorerPagerAdapter extends PagerAdapter {

    private FragmentManager fragmentManager;
    private Fragment[] fragments;

    public ExplorerPagerAdapter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        fragments = new Fragment[2];
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment fragment = getItem(position);
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.add(container.getId(), fragment, "fragment:" + position);
        trans.commit();
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.remove(fragments[position]);
        trans.commit();
        fragments[position] = null;
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
            case 0:
                return "Archivos";
            case 1:
                return "Descargas";
        }
    }

    public Fragment getItem(int position) {
        if (fragments[position] == null) {
            fragments[position] = createFragment(position);
        }
        return fragments[position];
    }

    private Fragment createFragment(int position) {
        switch (position) {
            default:
            case 0:
                return FragmentFilesRoot.get();
            case 1:
                return FragmentDownloads.get();
        }
    }
}
