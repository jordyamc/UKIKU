package knf.kuma.pojos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EAObject(
        @PrimaryKey
        var code: Int
)
