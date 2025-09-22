package com.example.cardsstudy

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

// A sealed class garante que um Cartão só pode ser de um dos tipos definidos aqui dentro.
sealed class Card {
    @DocumentId // Anotação para que o Firestore preencha o ID do documento automaticamente
    @get:Exclude
    var id: String = ""
    var type: CardType = CardType.FRONT_BACK // Um campo para sabermos qual o tipo do cartão
    var srsLevel: Int = 0 // Nível 0 = Novo, Nível 1 = 1 min, Nível 2 = 5 mins, etc.
    var nextReview: Date = Date()
}

// Enum para representar os tipos de cartão de forma segura
enum class CardType {
    FRONT_BACK,
    MULTIPLE_CHOICE,
    TYPE_ANSWER,
    CLOZE
}

// 1. Frente e Verso (o que já tínhamos)
data class FrontBackCard(
    val front: String = "",
    val back: String = "",
    val frontImageUrl: String? = null
) : Card() {
    init {
        type = CardType.FRONT_BACK
    }
}

// 2. Múltipla Escolha
data class MultipleChoiceCard(
    val question: String = "",
    val correctAnswer: String = "",
    val distractors: List<String> = emptyList()
) : Card() {
    init {
        type = CardType.MULTIPLE_CHOICE
    }
}

// 3. Digite a Resposta
data class TypeAnswerCard(
    val prompt: String = "",
    val acceptableAnswers: List<String> = emptyList() // Lista para aceitar variações (ex: "EUA", "Estados Unidos")
) : Card() {
    init {
        type = CardType.TYPE_ANSWER
    }
}

// 4. Cloze (Omissão)
// Usaremos um formato de texto como: "O [[sol]] é a estrela central do [[Sistema Solar]]."
data class ClozeCard(
    val textWithCloze: String = ""
) : Card() {
    init {
        type = CardType.CLOZE
    }
}