package com.example.cardsstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val cardList: List<Card>,
    private val onDeleteClick: (Card) -> Unit // Adicionamos a função de clique para apagar
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frontTextView: TextView = itemView.findViewById(R.id.card_front_text_view)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_card_button)

        init {
            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(cardList[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun getItemCount(): Int = cardList.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val currentCard = cardList[position]



        // Usamos o 'when' para tratar cada tipo de cartão
        when (currentCard) {
            is FrontBackCard -> {
                holder.frontTextView.text = currentCard.front
            }
            is MultipleChoiceCard -> {
                holder.frontTextView.text = "[Múltipla Escolha] ${currentCard.question}"
            }
            is TypeAnswerCard -> {
                holder.frontTextView.text = "[Digite a Resposta] ${currentCard.prompt}"
            }
            is ClozeCard -> {
                holder.frontTextView.text = "[Omissão] ${currentCard.textWithCloze}"
            }
        }
    }
}