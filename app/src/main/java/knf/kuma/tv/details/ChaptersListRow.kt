package knf.kuma.tv.details

import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter

class ChaptersListRow : ListRow {
    constructor(header: HeaderItem, adapter: ObjectAdapter) : super(header, adapter)

    constructor(id: Long, header: HeaderItem, adapter: ObjectAdapter) : super(id, header, adapter)

    constructor(adapter: ObjectAdapter) : super(adapter)
}
