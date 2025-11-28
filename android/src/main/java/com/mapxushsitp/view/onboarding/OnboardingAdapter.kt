package com.mapxushsitp.view.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mapxushsitp.R

class OnboardingAdapter(
    private val items: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageVH>() {

    inner class PageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageVH(view)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvDescription.text = item.description

        if (item.isGif) {
            Glide.with(holder.itemView)
                .asGif()
                .load(item.imageRes)
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(item.imageRes)
        }
    }

    override fun getItemCount(): Int = items.size
}

