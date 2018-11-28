package knf.kuma

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import knf.kuma.achievements.AchievementManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AchievementManager.onAppStart()
        startActivity(Intent(this, Main::class.java))
        finish()
    }


}