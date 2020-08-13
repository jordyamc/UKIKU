package knf.kuma

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import knf.kuma.custom.SingleFragmentMaterialActivity

class AppInfoActivityMaterial: SingleFragmentMaterialActivity() {
    override fun createFragment(): Fragment = AppInfoFragment().apply {
        arguments = Bundle().apply {
            putBoolean("isFlat",true)
        }
    }

    override fun getActivityTitle(): String = "Acerca de"

    companion object{
        fun open(context: Context){
            context.startActivity(Intent(context,AppInfoActivityMaterial::class.java))
        }
    }
}