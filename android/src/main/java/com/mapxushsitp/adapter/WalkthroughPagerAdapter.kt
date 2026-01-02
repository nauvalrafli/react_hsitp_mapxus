package com.mapxushsitp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.R

data class WalkthroughPage(
    @DrawableRes val iconRes: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val tip: String? = null
)

class WalkthroughPagerAdapter(
    private val pages: List<WalkthroughPage>
) : RecyclerView.Adapter<WalkthroughPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walkthrough_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.walkthrough_icon)
        private val title: TextView = itemView.findViewById(R.id.walkthrough_title)
        private val subtitle: TextView = itemView.findViewById(R.id.walkthrough_subtitle)
        private val description: TextView = itemView.findViewById(R.id.walkthrough_description)
        private val tip: TextView = itemView.findViewById(R.id.walkthrough_tip)

        fun bind(page: WalkthroughPage) {
            icon.setImageResource(page.iconRes)
            title.text = page.title
            subtitle.text = page.subtitle
            description.text = page.description

            if (page.tip.isNullOrBlank()) {
                tip.visibility = View.GONE
            } else {
                tip.visibility = View.VISIBLE
                tip.text = page.tip
            }
        }
    }
}

