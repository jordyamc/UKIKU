package knf.kuma.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;
import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.QueueObject;

@Dao
@TypeConverters({BaseConverter.class})
public interface QueueDAO {
    @Query("SELECT count(*) FROM queueobject WHERE eid = :eid")
    Boolean isInQueue(String eid);

    @Query("SELECT MIN(aid) AS id,`key`,aid,name,number,eid,isFile,uri,time,link FROM queueobject ORDER BY name")
    LiveData<List<QueueObject>> getAllAlone();

    @Query("SELECT * FROM queueobject ORDER BY name")
    LiveData<List<QueueObject>> getAll();

    @Query("SELECT * FROM queueobject ORDER BY time ASC")
    LiveData<List<QueueObject>> getAllAsort();

    @Query("SELECT count(*) FROM queueobject WHERE aid LIKE :aid")
    int countAlone(String aid);

    @Query("SELECT * FROM queueobject WHERE aid = :aid ORDER BY eid ASC")
    LiveData<List<QueueObject>> getByAid(String aid);

    @Query("SELECT * FROM queueobject WHERE aid = :aid ORDER BY eid ASC")
    List<QueueObject> getAllByAid(String aid);

    @Query("SELECT count(*) FROM queueobject")
    LiveData<Integer> getCountLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(QueueObject object);

    @Update
    void update(QueueObject... objects);

    @Delete
    void remove(QueueObject object);

    @Delete
    void remove(List<QueueObject> object);

    @Query("DELETE FROM queueobject WHERE aid LIKE :aid")
    void removeByID(String aid);

    @Query("DELETE FROM queueobject WHERE eid LIKE :eid")
    void removeByEID(String eid);
}
