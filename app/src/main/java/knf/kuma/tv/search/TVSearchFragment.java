package knf.kuma.tv.search;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;
import android.support.v4.content.ContextCompat;

import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.tv.anime.AnimePresenter;
import knf.kuma.tv.details.TVAnimesDetails;

public class TVSearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider, SpeechRecognitionCallback, OnItemViewClickedListener {
    private ArrayObjectAdapter arrayObjectAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        arrayObjectAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(this);
        if (getContext().getPackageManager().hasSystemFeature("amazon.hardware.fire_tv"))
            setSpeechRecognitionCallback(this);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 55498);
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return arrayObjectAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        setResult(newQuery);
        return true;
    }

    @Override
    public void recognizeSpeech() {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        setResult(query);
        return true;
    }

    private void setResult(String query) {
        final LiveData<List<AnimeObject>> liveData = CacheDB.INSTANCE.animeDAO().getSearchList("%" + query + "%");
        liveData.observe(getActivity(), animeObjects -> {
            liveData.removeObservers(getActivity());
            arrayObjectAdapter.clear();
            ArrayObjectAdapter objectAdapter = new ArrayObjectAdapter(new AnimePresenter());
            for (AnimeObject object : animeObjects)
                objectAdapter.add(object);
            HeaderItem headerItem = new HeaderItem(animeObjects.size() > 0 ? "Resultados para '" + query + "'" : "Sin resultados");
            arrayObjectAdapter.add(new ListRow(headerItem, objectAdapter));
        });
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        AnimeObject object = (AnimeObject) item;
        TVAnimesDetails.start(getContext(), object.link);
    }
}
