package com.example.doglens.data.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.doglens.R

class FavoriteSortAdapter(
    private val context: Context,
    private val options: List<String>
) : BaseAdapter() {

    override fun getCount(): Int {
        return options.size
    }

    override fun getItem(position: Int): String {
       return options[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_favorite_sort, parent, false)
        view.findViewById<TextView>(R.id.txtSortLabel).text=getItem(position)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val textView = (convertView as? TextView) ?: TextView(context)
        textView.text=getItem(position)
        textView.setTextColor(android.graphics.Color.BLACK)
        textView.textSize=14f
        textView.setPadding(16,16,16, 16)
        return textView
    }
}