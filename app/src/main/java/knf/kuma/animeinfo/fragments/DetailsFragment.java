package knf.kuma.animeinfo.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import knf.kuma.R;
import knf.kuma.animeinfo.AnimeViewModel;
import knf.kuma.animeinfo.viewholders.AnimeDetailsHolder;
import knf.kuma.pojos.AnimeObject;

public class DetailsFragment extends Fragment {
    private AnimeDetailsHolder holder;

    public DetailsFragment() {
    }

    @NonNull
    public static DetailsFragment get(){
        return new DetailsFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(getActivity()).get(AnimeViewModel.class).getLiveData().observe(this, new Observer<AnimeObject>() {
            @Override
            public void onChanged(@Nullable AnimeObject object) {
                holder.populate(DetailsFragment.this,object);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_anime_details,container,false);
        holder=new AnimeDetailsHolder(view);
        return view;
    }
}
