package com.example.digitsrecognizer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DigitListAdapter (
    private val context: Context,
    private val digitList: ArrayList<Digit>
) : RecyclerView.Adapter<DigitListAdapter.DigitViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DigitViewHolder {
        return DigitViewHolder(LayoutInflater.from(context).inflate(
            R.layout.digit_list_item,
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: DigitViewHolder, position: Int) {
        val word = digitList[position]
        holder.predictedDigit.text = word.resultPredicted.toString()
        holder.confidence.text = word.confidence.toString()
        holder.digitPicture.setImageBitmap(word.pictureImage)
    }

    override fun getItemCount(): Int {
        return digitList.size
    }

    class DigitViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val predictedDigit: TextView = view.findViewById(R.id.predictedDigit)
        val confidence: TextView = view.findViewById(R.id.confidence)
        val digitPicture: ImageView = view.findViewById(R.id.digitPicture)
    }
}