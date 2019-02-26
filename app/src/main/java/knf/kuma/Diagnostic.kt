package knf.kuma

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import knf.kuma.commons.EAHelper

class Diagnostic : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
    }
}