package knf.kuma.explorer;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.ExplorerObject;

public class ExplorerCreator {
    static boolean IS_CREATED = false;
    static boolean IS_FILES = true;
    static ExplorerObject FILES_NAME;
    private static MutableLiveData<String> STATE_LISTENER = new MutableLiveData<>();
    public static void start(final Context context, final EmptyListener listener) {
        IS_CREATED = true;
        final ExplorerDAO explorerDAO = CacheDB.INSTANCE.explorerDAO();
        postState("Iniciando busqueda");
        AsyncTask.execute(() -> {
            AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
            File root = FileAccessHelper.INSTANCE.getDownloadsDirectory();
            if (root.exists()) {
                postState("Buscando animes");
                List<ExplorerObject> list = new ArrayList<>();
                File[] files = root.listFiles(File::isDirectory);
                if (files != null) {
                    List<String> names = new ArrayList<>();
                    int progress = 0;
                    for (File file : files) {
                        names.add(file.getName());
                    }
                    for (AnimeObject object : animeDAO.getAllByFile(names))
                        try {
                            progress++;
                            postState(String.format(Locale.getDefault(), "Procesando animes %d/%d", progress, files.length));
                            list.add(new ExplorerObject(object));
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    postState("Creando lista");
                    explorerDAO.insert(list);
                }
                if (list.size() == 0) {
                    listener.onEmpty();
                    postState(null);
                }
            } else {
                explorerDAO.deleteAll();
                listener.onEmpty();
                postState(null);
            }
        });
    }

    public static void onDestroy() {
        IS_CREATED = false;
        IS_FILES = true;
        FILES_NAME = null;
        CacheDB.INSTANCE.explorerDAO().deleteAll();
    }

    static LiveData<String> getStateListener() {
        return STATE_LISTENER;
    }

    private static void postState(final String state) {
        new Handler(Looper.getMainLooper()).post(() -> STATE_LISTENER.setValue(state));
    }

    public interface EmptyListener {
        void onEmpty();
    }
}
