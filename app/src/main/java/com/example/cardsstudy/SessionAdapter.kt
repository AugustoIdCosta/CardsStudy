package com.example.cardsstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SessionAdapter(private val sessions: List<StudySession>) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.session_date_text)
        val deckNameText: TextView = itemView.findViewById(R.id.session_deck_name_text) // Adicionado
        val scoreText: TextView = itemView.findViewById(R.id.session_score_text)
        val percentageText: TextView = itemView.findViewById(R.id.session_percentage_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun getItemCount() = sessions.size

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]

        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("pt-BR"))
        holder.dateText.text = session.timestamp?.let { dateFormat.format(it) } ?: "Data indisponível"

        holder.deckNameText.text = "Baralho: ${session.deckName}" // Adicionado

        holder.scoreText.text = "Pontuação: ${session.correctCount} acertos, ${session.incorrectCount} erros"

        val total = session.correctCount + session.incorrectCount
        val percentage = if (total > 0) (session.correctCount.toDouble() / total) * 100 else 0.0
        val df = DecimalFormat("#.#")
        holder.percentageText.text = "Aproveitamento: ${df.format(percentage)}%"
    }
}