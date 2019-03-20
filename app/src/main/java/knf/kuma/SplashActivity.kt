package knf.kuma

import android.content.Intent
import android.os.Bundle
import knf.kuma.achievements.AchievementManager
import knf.kuma.custom.GenericActivity
import knf.kuma.tv.ui.TVMain

class SplashActivity : GenericActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AchievementManager.onAppStart()
        if (resources.getBoolean(R.bool.isTv))
            startActivity(Intent(this, TVMain::class.java))
        else
            startActivity(Intent(this, Main::class.java))
        finish()
    }
}