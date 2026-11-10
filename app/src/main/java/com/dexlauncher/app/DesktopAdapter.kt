package com.dexlauncher.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DesktopAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongClick: (AppItem, View) -> Unit
) : RecyclerView.Adapter<DesktopAdapter.ViewHolder>() {

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
            .inflate(R.layout.item_desktop_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.bind(app, settings, onAppClick, onAppLongClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.img_app_icon)
        private val labelView: TextView = itemView.findViewById(R.id.tv_app_label)

        fun bind(
            app: AppItem,
            settings: LauncherSettings,
            onClick: (AppItem) -> Unit,
            onLongClick: (AppItem, View) -> Unit
        ) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val sizePx = (settings.iconSizeDp * density).toInt()

            iconView.layoutParams.width = sizePx
            iconView.layoutParams.height = sizePx

            app.icon?.let { rawIcon ->
                val styled = IconCustomizer.shapeIcon(context, rawIcon, settings.iconShape, sizePx)
                iconView.setImageDrawable(styled)
            } ?: iconView.setImageResource(android.R.drawable.sym_def_app_icon)

            labelView.text = app.label
            labelView.visibility = if (settings.showLabels) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(app) }
            itemView.setOnLongClickListener {
                onLongClick(app, itemView)
                true
            }
        }
    }
}
