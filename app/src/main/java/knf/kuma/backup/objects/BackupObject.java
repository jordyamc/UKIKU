package knf.kuma.backup.objects;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BackupObject<T> {
    public String date;
    public List<T> data;

    public BackupObject(List<T> data) {
        this.date = new SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        this.data = data;
    }
}
