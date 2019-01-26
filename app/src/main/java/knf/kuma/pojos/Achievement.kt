package knf.kuma.pojos

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.EAHelper
import knf.kuma.custom.AchievementUnlocked
import knf.kuma.database.BaseConverter
import java.text.SimpleDateFormat
import java.util.*

@Entity
@TypeConverters(BaseConverter::class)
data class Achievement(
        @PrimaryKey
        var key: Long,
        var name: String,
        var description: String,
        var points: Int,
        var isSecret: Boolean = false,
        var group: String? = null,
        var time: Long = 0,
        var count: Int = 0,
        var goal: Int = 0,
        var isUnlocked: Boolean = false
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
        return if (isSecret && !isUnlocked)
            "Logro secreto"
        else
            name
    }

    fun usableDescription(): String {
        return if (isSecret && !isUnlocked)
            "Usa mas la app para desbloquear"
        else
            description
    }

    fun usableIcon(): Int {
        return if (isSecret && !isUnlocked)
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
                .setBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColor(context)))
                .setIconBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight(context)))
        //.setPopUpOnClickListener { context.startActivity(Intent(context,)) }
    }
}