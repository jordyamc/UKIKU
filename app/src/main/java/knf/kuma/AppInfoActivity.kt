package knf.kuma

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import knf.kuma.custom.SingleFragmentActivity

class AppInfoActivity: SingleFragmentActivity() {
    override fun createFragment(): Fragment = AppInfoFragment().apply {
        arguments = Bundle().apply {
            putBoolean("isFlat",false)
        }
    }

    override fun getActivityTitle(): String = "Acerca de"

    companion object{
        fun open(context: Context){
            context.startActivity(Intent(context,AppInfoActivity::class.java))
        }
    }
}