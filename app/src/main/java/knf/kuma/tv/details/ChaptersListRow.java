package knf.kuma.tv.details;

import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;

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
