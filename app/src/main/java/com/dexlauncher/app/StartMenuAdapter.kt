package com.dexlauncher.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StartMenuAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongClick: (AppItem, View) -> Unit
) : RecyclerView.Adapter<StartMenuAdapter.ViewHolder>() {

    private val items = mutableListOf<AppItem>()
    private var settings: LauncherSettings = LauncherSettings()

    fun updateData(newItems: List<AppItem>, newSettings: LauncherSettings) {
        items.clear()
        items.addAll(newItems)
        settings = newSettings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_start_menu_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], settings, onAppClick, onAppLongClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.img_start_icon)
        private val labelView: TextView = itemView.findViewById(R.id.tv_start_label)

        fun bind(
            app: AppItem,
            settings: LauncherSettings,
            onClick: (AppItem) -> Unit,
            onLongClick: (AppItem, View) -> Unit
        ) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val sizePx = (48 * density).toInt()

            app.icon?.let { rawIcon ->
                val styled = IconCustomizer.shapeIcon(context, rawIcon, settings.iconShape, sizePx)
                iconView.setImageDrawable(styled)
            } ?: iconView.setImageResource(android.R.drawable.sym_def_app_icon)

            labelView.text = app.label

            itemView.setOnClickListener { onClick(app) }
            itemView.setOnLongClickListener {
                onLongClick(app, itemView)
                true
            }
        }
    }
}
