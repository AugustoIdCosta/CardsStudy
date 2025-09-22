package com.example.cardsstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CardAdapter(
    private val cardList: List<Card>,
    private val onDeleteClick: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frontTextView: TextView = itemView.findViewById(R.id.card_front_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.card_image_view)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_card_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun getItemCount() = cardList.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cardList[position]
        holder.deleteButton.setOnClickListener { onDeleteClick(card) }

        when (card) {
            is FrontBackCard -> {
                // Se o cartão tem uma imagem, mostra a imagem e esconde o texto.
                if (card.frontImageUrl != null) {
                    holder.frontTextView.visibility = View.GONE
                    holder.imageView.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(card.frontImageUrl)
                        .into(holder.imageView)
                } else {
                    // Se não tem imagem, esconde a imagem e mostra o texto.
                    holder.imageView.visibility = View.GONE
                    holder.frontTextView.visibility = View.VISIBLE
                    holder.frontTextView.text = card.front
                }
            }

            is MultipleChoiceCard -> {
                // Garante que a imagem está escondida e o texto visível.
                holder.imageView.visibility = View.GONE
                holder.frontTextView.visibility = View.VISIBLE
                holder.frontTextView.text = "[Múltipla Escolha] ${card.question}"
            }

            is TypeAnswerCard -> {
                // Garante que a imagem está escondida e o texto visível.
                holder.imageView.visibility = View.GONE
                holder.frontTextView.visibility = View.VISIBLE
                holder.frontTextView.text = "[Digite a Resposta] ${card.prompt}"
            }

            is ClozeCard -> {
                // Garante que a imagem está escondida e o texto visível.
                holder.imageView.visibility = View.GONE
                holder.frontTextView.visibility = View.VISIBLE
                holder.frontTextView.text = "[Omissão] ${card.textWithCloze}"
            }
        }
    }
}