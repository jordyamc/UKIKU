package knf.kuma.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import knf.kuma.pojos.DownloadObject;

/**
 * Created by Jordy on 10/01/2018.
 */

@Dao
public interface DownloadsDAO {
    @Query("SELECT * FROM downloadobject")
    DataSource.Factory<Integer,DownloadObject> getAll();

    @Query("SELECT * FROM downloadobject WHERE eid LIKE :eid")
    DownloadObject getByEid(String eid);

    @Query("SELECT * FROM downloadobject WHERE file LIKE :name")
    DownloadObject getByFile(String name);

    @Query("SELECT * FROM downloadobject WHERE eid LIKE :eid")
    LiveData<DownloadObject> getLiveByEid(String eid);

    @Query("SELECT * FROM downloadobject WHERE `key` LIKE :key")
    LiveData<DownloadObject> getLiveByKey(int key);

    @Query("SELECT * FROM downloadobject WHERE state<=0")
    LiveData<List<DownloadObject>> getActive();

    @Query("SELECT count(*) FROM downloadobject WHERE state=-1")
    int countPending();

    @Query("DELETE FROM downloadobject WHERE eid LIKE :eid")
    void deleteByEid(String eid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadObject object);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(DownloadObject object);

    @Delete
    void delete(DownloadObject object);
}
