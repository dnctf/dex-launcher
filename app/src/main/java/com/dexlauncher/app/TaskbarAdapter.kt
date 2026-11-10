package com.dexlauncher.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class TaskbarAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongClick: (AppItem, View) -> Unit
) : RecyclerView.Adapter<TaskbarAdapter.ViewHolder>() {

    private val pinnedApps = mutableListOf<AppItem>()
    private var iconShape: String = "squircle"

    fun updateData(newApps: List<AppItem>, shape: String) {
        pinnedApps.clear()
        pinnedApps.addAll(newApps)
        iconShape = shape
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_taskbar_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pinnedApps[position], iconShape, onAppClick, onAppLongClick)
    }

    override fun getItemCount(): Int = pinnedApps.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.img_taskbar_icon)

        fun bind(
            app: AppItem,
            shape: String,
            onClick: (AppItem) -> Unit,
            onLongClick: (AppItem, View) -> Unit
        ) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val sizePx = (36 * density).toInt()

            app.icon?.let { rawIcon ->
                val styled = IconCustomizer.shapeIcon(context, rawIcon, shape, sizePx)
                iconView.setImageDrawable(styled)
            } ?: iconView.setImageResource(android.R.drawable.sym_def_app_icon)

            itemView.setOnClickListener { onClick(app) }
            itemView.setOnLongClickListener {
                onLongClick(app, itemView)
                true
            }
        }
    }
}
