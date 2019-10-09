package knf.kuma.pojos

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class EAObject(
        @PrimaryKey
        var code: Int = -1
)
