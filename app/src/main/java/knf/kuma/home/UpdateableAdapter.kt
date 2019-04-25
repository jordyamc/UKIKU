package knf.kuma.home

import androidx.recyclerview.widget.RecyclerView

abstract class UpdateableAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract fun updateList(list: List<Any>)
}