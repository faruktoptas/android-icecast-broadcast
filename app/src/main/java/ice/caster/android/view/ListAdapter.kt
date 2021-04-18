package ice.caster.android.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ice.caster.android.R
import ice.caster.android.shout.ConfigItem

class ListAdapter(private val onStartStop: (Int) -> Unit,
                  private val onDelete: (Int) -> Unit,
                  private val onAdd: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var list: List<ConfigItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_recycler, parent, false))
        } else {
            FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_footer, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val configItem = list[position]
            holder.title.text = configItem.title
            holder.btnStart.setOnClickListener {
                onStartStop(position)
            }
            holder.btnDelete.setOnClickListener {
                onDelete(position)
            }
            val bgColor: Int
            val text: Int
            if (configItem.isRecording) {
                bgColor = R.color.green
                text = R.string.txt_stop
            } else {
                bgColor = R.color.white
                text = R.string.txt_start
            }
            holder.btnStart.setText(text)
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.card.context, bgColor))
        } else if (holder is FooterViewHolder) {
            holder.btnAdd.setOnClickListener {
                onAdd()
            }
        }
    }

    override fun getItemCount() = list.size + 1

    override fun getItemViewType(position: Int) = if (position == list.size) TYPE_FOOTER else TYPE_ITEM

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvMain) as TextView
        val btnStart: Button = view.findViewById(R.id.btnStartStop)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val card: CardView = view.findViewById(R.id.cv)
    }

    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
    }

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_FOOTER = 1
    }
}