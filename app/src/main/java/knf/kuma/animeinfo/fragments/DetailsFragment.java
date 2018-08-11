package knf.kuma.animeinfo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import knf.kuma.R;
import knf.kuma.animeinfo.AnimeViewModel;
import knf.kuma.animeinfo.viewholders.AnimeDetailsHolder;
import knf.kuma.pojos.AnimeObject;
import xdroid.toaster.Toaster;

public class DetailsFragment extends Fragment {
    private AnimeDetailsHolder holder;

    public DetailsFragment() {
    }

    @NonNull
    public static DetailsFragment get() {
        return new DetailsFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProviders.of(getActivity()).get(AnimeViewModel.class).getLiveData().observe(this, new Observer<AnimeObject>() {
            @Override
            public void onChanged(@Nullable AnimeObject object) {
                if (object != null)
                    holder.populate(DetailsFragment.this, object);
                else {
                    Toaster.toast("No se pudo obtener la informacion");
                    getActivity().finish();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_anime_details, container, false);
        holder = new AnimeDetailsHolder(view);
        return view;
    }
}
