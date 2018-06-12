package knf.kuma.tv.details;

import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ObjectAdapter;

public class ChaptersListRow extends ListRow {
    public ChaptersListRow(HeaderItem header, ObjectAdapter adapter) {
        super(header, adapter);
    }

    public ChaptersListRow(long id, HeaderItem header, ObjectAdapter adapter) {
        super(id, header, adapter);
    }

    public ChaptersListRow(ObjectAdapter adapter) {
        super(adapter);
    }
}
