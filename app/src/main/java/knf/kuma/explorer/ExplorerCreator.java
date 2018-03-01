package knf.kuma.explorer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import knf.kuma.database.CacheDB;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.pojos.ExplorerObject;

public class ExplorerCreator {
    public static void start(final Context context, final EmptyListener listener) {
        final ExplorerDAO explorerDAO = CacheDB.INSTANCE.explorerDAO();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                AnimeDAO animeDAO = CacheDB.INSTANCE.animeDAO();
                File root = FileAccessHelper.INSTANCE.getDownloadsDirectory();
                if (root.exists()) {
                    List<ExplorerObject> list = new ArrayList<>();
                    File[] files = root.listFiles();
                    if (files != null)
                        for (File file : files) {
                            try {
                                Log.e("Explorer", "Search " + file.getName());
                                list.add(new ExplorerObject(context, animeDAO.getByFile(file.getName())));
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    explorerDAO.insert(list);
                    if (list.size() == 0)
                        listener.onEmpty();
                } else {
                    explorerDAO.deleteAll();
                    listener.onEmpty();
                }
            }
        });
    }

    public interface EmptyListener {
        void onEmpty();
    }
}
