package knf.kuma.backup.objects;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Jordy on 20/02/2018.
 */

public class BackupObject<T> {
    public String date;
    public List<T> data;

    public BackupObject(String date, List<T> data) {
        this.date = date;
        this.data = data;
    }

    public BackupObject(List<T> data) {
        this.date = new SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        this.data = data;
    }
}
