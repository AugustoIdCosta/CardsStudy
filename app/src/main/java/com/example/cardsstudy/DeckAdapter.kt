package com.example.cardsstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeckAdapter(
    private val deckList: MutableList<Deck>,
    private val onItemClick: (Deck) -> Unit,
    private val onDeleteClick: (Deck) -> Unit // Nova função para apagar
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    inner class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.deck_name_text_view)
        val descriptionTextView: TextView = itemView.findViewById(R.id.deck_description_text_view)
        val dueCountTextView: TextView = itemView.findViewById(R.id.deck_due_cards_count)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_deck_button)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(deckList[adapterPosition])
                }
            }
            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(deckList[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun getItemCount() = deckList.size

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val currentDeck = deckList[position]
        holder.nameTextView.text = currentDeck.name
        holder.descriptionTextView.text = currentDeck.description
        holder.dueCountTextView.text = currentDeck.dueCardsCount.toString()
    }
}