package com.mapxushsitp.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.data.model.InstructionDto
import com.mapxushsitp.R

interface OnRouteInstructionListener {
    fun onBackButtonPressed(currentPosition: Int)
    fun onNextButtonPressed(currentPosition: Int)
}

class RouteInstructionAdapter(
    private val instructions: List<InstructionDto>,
    private val listener: OnRouteInstructionListener
) : RecyclerView.Adapter<RouteInstructionAdapter.InstructionViewHolder>() {

    inner class InstructionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInstruction: TextView = itemView.findViewById(R.id.tv_instruction)
        val tvBuilding: TextView = itemView.findViewById(R.id.tv_building)
        val btnNext: ImageView = itemView.findViewById(R.id.btn_next)
        val btnPrev: ImageView = itemView.findViewById(R.id.btn_prev)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_instructions, parent, false)
        return InstructionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstructionViewHolder, position: Int) {
        val instruction = instructions[position]
        holder.tvInstruction.text = instruction.text
        holder.tvBuilding.text = instruction.floorId

        holder.btnPrev.isEnabled = position > 0
        holder.btnNext.isEnabled = position < instructions.size - 1

        holder.btnPrev.setOnClickListener {
            listener.onBackButtonPressed(position)
        }

        holder.btnNext.setOnClickListener {
            listener.onNextButtonPressed(position)
        }
    }

    override fun getItemCount(): Int = instructions.size
}
