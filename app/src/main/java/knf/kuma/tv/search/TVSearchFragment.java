package knf.kuma.tv.search;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SpeechRecognitionCallback;
import androidx.lifecycle.LiveData;
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
        if (getActivity() != null) {
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
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        AnimeObject object = (AnimeObject) item;
        TVAnimesDetails.start(getContext(), object.link);
    }
}
