package knf.kuma.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import knf.kuma.database.BaseConverter;
import knf.kuma.pojos.EAObject;

@Dao
@TypeConverters({BaseConverter.class})
public interface EaDAO {
    @Query("SELECT count(*) FROM eaobject WHERE code=:code")
    Boolean isUnlocked(int code);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void unlock(EAObject object);
}
