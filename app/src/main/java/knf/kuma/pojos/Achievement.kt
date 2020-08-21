package knf.kuma.pojos

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.EAHelper
import knf.kuma.custom.AchievementUnlocked
import knf.kuma.database.BaseConverter
import java.text.SimpleDateFormat
import java.util.*

@Keep
@Entity
@TypeConverters(BaseConverter::class)
@IgnoreExtraProperties
open class Achievement(
        @SerializedName("key")
        @PrimaryKey
        var key: Long = -1,
        @SerializedName("name")
        var name: String = "",
        @SerializedName("description")
        var description: String = "",
        @SerializedName("points")
        var points: Int = -1,
        @SerializedName("isSecret")
        var isSecret: Boolean = false,
        @SerializedName("group")
        var group: String? = null,
        @SerializedName("time")
        var time: Long = 0,
        @SerializedName("count")
        var count: Int = 0,
        @SerializedName("goal")
        var goal: Int = 0,
        @SerializedName("isUnlocked")
        var isUnlocked: Boolean = false,
        @SerializedName("isRevealed")
        var isRevealed: Boolean = false
) {

    fun getState(): String {
        return if (isUnlocked) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance().also {
                it.timeInMillis = time
            }
            dateFormat.format(calendar.time)
        } else
            "Bloqueado"
    }

    fun usableName(): String {
        return if (isSecret && !isUnlocked && !isRevealed)
            "Logro secreto"
        else
            name
    }

    fun usableDescription(): String {
        return if (isSecret && !isUnlocked && !isRevealed)
            "Usa mas la app para desbloquear"
        else
            description
    }

    fun usableIcon(): Int {
        return if (isSecret && !isUnlocked && !isRevealed)
            R.drawable.ic_locked
        else
            AchievementManager.getIcon(key)
    }

    private fun tintedIcon(context: Context): Drawable? {
        return try {
            val drawable = ContextCompat.getDrawable(context, AchievementManager.getIcon(key))
                    ?: return null
            val drawableWrap = DrawableCompat.wrap(drawable)
            DrawableCompat.setTint(drawableWrap, Color.WHITE)
            drawableWrap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun achievementData(context: Context): AchievementUnlocked.AchievementData {
        return AchievementUnlocked.AchievementData()
                .setTitle(name)
                .setSubtitle(description)
                .setIcon(tintedIcon(context))
                .setTextColor(Color.WHITE)
                .setBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColor()))
                .setIconBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight()))
        //.setPopUpOnClickListener { context.startActivity(Intent(context,)) }
    }
}