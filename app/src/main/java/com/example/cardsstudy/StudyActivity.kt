package com.example.cardsstudy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern

class StudyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DECK_ID = "extra_deck_id"
        const val EXTRA_DECK_NAME = "extra_deck_name"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var allCards = listOf<Card>()
    private var currentCardIndex = 0
    private var correctAnswers = 0
    private var incorrectAnswers = 0
    private var deckId: String? = null

    // Views comuns
    private lateinit var progressText: TextView
    private lateinit var nextButton: Button
    private lateinit var checkButton: Button

    // Views para Frente/Verso e Cloze
    private lateinit var frontBackContainer: View
    private lateinit var frontText: TextView
    private lateinit var backText: TextView
    private lateinit var fbClozeAnswerEditText: EditText

    // Views para Múltipla Escolha
    private lateinit var mcContainer: LinearLayout
    private lateinit var mcQuestionText: TextView
    private val mcOptionButtons = mutableListOf<Button>()

    // Views para Digite a Resposta
    private lateinit var taContainer: View
    private lateinit var taPromptText: TextView
    private lateinit var taAnswerEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study)
        deckId = intent.getStringExtra(EXTRA_DECK_ID) ?: run { finish(); return }

        initializeViews()
        nextButton.setOnClickListener { goToNextCard() }
        loadCards(deckId!!)
    }

    private fun initializeViews() {
        findViewById<MaterialToolbar>(R.id.toolbar_study).setNavigationOnClickListener { finish() }
        progressText = findViewById(R.id.card_progress_text)
        nextButton = findViewById(R.id.next_card_button)
        checkButton = findViewById(R.id.check_answer_button)
        frontBackContainer = findViewById(R.id.front_back_container)
        frontText = findViewById(R.id.study_front_text)
        backText = findViewById(R.id.study_back_text)
        fbClozeAnswerEditText = findViewById(R.id.fb_cloze_answer_edit_text)
        mcContainer = findViewById(R.id.multiple_choice_container)
        mcQuestionText = findViewById(R.id.mc_study_question)
        mcOptionButtons.add(findViewById(R.id.mc_option_1))
        mcOptionButtons.add(findViewById(R.id.mc_option_2))
        mcOptionButtons.add(findViewById(R.id.mc_option_3))
        mcOptionButtons.add(findViewById(R.id.mc_option_4))
        taContainer = findViewById(R.id.type_answer_container)
        taPromptText = findViewById(R.id.ta_study_prompt)
        taAnswerEditText = findViewById(R.id.ta_study_answer_edit_text)
    }

    private fun loadCards(deckId: String) {
        val now = Date() // Pega a data e hora atuais

        db.collection("decks").document(deckId).collection("cards")
            .whereLessThanOrEqualTo("nextReview", now) // AQUI ESTÁ A MAGIA DO SRS
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Nenhum cartão para rever agora. Bom trabalho!", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val cardList = mutableListOf<Card>()
                for (document in querySnapshot.documents) {
                    val cardTypeString = document.getString("type") ?: "FRONT_BACK"
                    val card = try {
                        when (CardType.valueOf(cardTypeString)) {
                            CardType.FRONT_BACK -> document.toObject(FrontBackCard::class.java)
                            CardType.MULTIPLE_CHOICE -> document.toObject(MultipleChoiceCard::class.java)
                            CardType.TYPE_ANSWER -> document.toObject(TypeAnswerCard::class.java)
                            CardType.CLOZE -> document.toObject(ClozeCard::class.java)
                        }
                    } catch (e: Exception) { null }
                    card?.let { it.id = document.id; cardList.add(it) }
                }
                allCards = cardList.shuffled()
                currentCardIndex = -1
                goToNextCard()
            }
            .addOnFailureListener { e ->
                Log.e("StudyActivity", "Erro ao carregar cartões para estudo", e)
                Toast.makeText(this, "Falha ao carregar baralho.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToNextCard() {
        if (currentCardIndex < allCards.size - 1) {
            currentCardIndex++
            displayCard()
        } else {
            saveStudySession()
        }
    }

    private fun saveStudySession() {
        val user = auth.currentUser
            ?: run { navigateToSummary(); return } // Garante que navega mesmo sem utilizador
        val session = StudySession(
            userId = user.uid,
            deckId = deckId ?: "",
            deckName = intent.getStringExtra(EXTRA_DECK_NAME) ?: "",
            correctCount = correctAnswers,
            incorrectCount = incorrectAnswers
        )
        db.collection("studySessions")
            .add(session)
            .addOnSuccessListener { navigateToSummary() }
            .addOnFailureListener { navigateToSummary() }
    }

    private fun navigateToSummary() {
        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra(SummaryActivity.EXTRA_CORRECT_COUNT, correctAnswers)
            putExtra(SummaryActivity.EXTRA_INCORRECT_COUNT, incorrectAnswers)
        }
        startActivity(intent)
        finish()
    }

    private fun displayCard() {
        if (allCards.isEmpty()) {
            Toast.makeText(this, "Nenhum cartão neste baralho.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        progressText.text = "${currentCardIndex + 1} / ${allCards.size}"
        val card = allCards[currentCardIndex]

        frontBackContainer.visibility = View.GONE
        mcContainer.visibility = View.GONE
        taContainer.visibility = View.GONE
        checkButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE

        when (card) {
            is FrontBackCard -> displayFrontBackCard(card)
            is MultipleChoiceCard -> displayMultipleChoiceCard(card)
            is TypeAnswerCard -> displayTypeAnswerCard(card)
            is ClozeCard -> displayClozeCard(card)
            else -> Log.e("StudyActivity", "Tipo de cartão desconhecido: $card")
        }
    }

    private fun handleAnswer(card: Card, isCorrect: Boolean, message: String? = null) {
        if (isCorrect) {
            correctAnswers++
            card.srsLevel++ // Aumenta o nível
            Toast.makeText(this, message ?: "Correto!", Toast.LENGTH_SHORT).show()
        } else {
            incorrectAnswers++
            card.srsLevel = 0 // Errou, volta para o início
            Toast.makeText(this, message ?: "Incorreto!", Toast.LENGTH_SHORT).show()
        }

        // Calcula a próxima revisão e atualiza o cartão no Firebase
        updateCardSrs(card)

        checkButton.visibility = View.GONE
        nextButton.visibility = View.VISIBLE
    }

    private fun updateCardSrs(card: Card) {
        val calendar = Calendar.getInstance()
        val intervals = mapOf(
            0 to 1,  // 1 minuto
            1 to 5,  // 5 minutos
            2 to 10, // 10 minutos
            3 to 60, // 1 hora
            4 to 180 // 3 horas

        )
        val minutesToAdd = intervals[card.srsLevel] ?: intervals.values.last()
        calendar.add(Calendar.MINUTE, minutesToAdd)
        card.nextReview = calendar.time

        deckId?.let {
            db.collection("decks").document(it).collection("cards").document(card.id)
                .set(card) // Usa .set() para atualizar o cartão inteiro com os novos dados de SRS
                .addOnFailureListener { e ->
                    Log.w(
                        "StudyActivity",
                        "Falha ao atualizar o cartão SRS",
                        e
                    )
                }
        }
    }

    private fun displayFrontBackCard(card: FrontBackCard) {
        frontBackContainer.visibility = View.VISIBLE
        checkButton.text = "Verificar Resposta"
        frontText.text = card.front
        backText.text = "Resposta: ${card.back}"
        fbClozeAnswerEditText.text.clear()
        frontText.visibility = View.VISIBLE
        fbClozeAnswerEditText.visibility = View.VISIBLE
        backText.visibility = View.GONE

        checkButton.setOnClickListener {
            val userAnswer = fbClozeAnswerEditText.text.toString().trim()
            val isCorrect = userAnswer.equals(card.back, ignoreCase = true)
            handleAnswer(
                card,
                isCorrect,
                if (isCorrect) "Correto!" else "Incorreto. A resposta era: ${card.back}"
            )

            fbClozeAnswerEditText.visibility = View.GONE
            backText.visibility = View.VISIBLE
        }
    }

    private fun displayMultipleChoiceCard(card: MultipleChoiceCard) {
        mcContainer.visibility = View.VISIBLE
        checkButton.visibility = View.GONE // Múltipla escolha não usa o botão de verificação
        mcQuestionText.text = card.question
        val options = (card.distractors + card.correctAnswer).shuffled()

        mcOptionButtons.forEachIndexed { index, button ->
            if (index < options.size) {
                button.visibility = View.VISIBLE
                button.text = options[index]
                button.setBackgroundColor(Color.LTGRAY)
                button.setOnClickListener {
                    val isCorrect = button.text == card.correctAnswer
                    handleAnswer(card, isCorrect) // Passa o cartão para a lógica de SRS
                    mcOptionButtons.forEach { btn -> btn.isEnabled = false }
                    if (isCorrect) button.setBackgroundColor(Color.GREEN) else button.setBackgroundColor(
                        Color.RED
                    )
                }
                button.isEnabled = true
            } else {
                button.visibility = View.GONE
            }
        }
    }

    private fun displayTypeAnswerCard(card: TypeAnswerCard) {
        taContainer.visibility = View.VISIBLE
        checkButton.text = "Verificar Resposta"
        taPromptText.text = card.prompt
        taAnswerEditText.text.clear()

        checkButton.setOnClickListener {
            val userAnswer = taAnswerEditText.text.toString().trim()
            val isCorrect = card.acceptableAnswers.any { it.equals(userAnswer, ignoreCase = true) }
            handleAnswer(
                card,
                isCorrect,
                if (isCorrect) null else "A resposta correta era: ${card.acceptableAnswers.first()}"
            )
        }
    }

    private fun displayClozeCard(card: ClozeCard) {
        frontBackContainer.visibility = View.VISIBLE
        checkButton.text = "Verificar Resposta"

        val matcher = Pattern.compile("\\[\\[(.*?)]]").matcher(card.textWithCloze)
        val answers = mutableListOf<String>()
        while (matcher.find()) {
            answers.add(matcher.group(1)!!)
        }

        val questionText = card.textWithCloze.replace(Regex("\\[\\[(.*?)]]"), "[...]")
        val correctAnswerText =
            "Resposta: ${card.textWithCloze.replace(Regex("\\[\\[(.*?)]]"), "$1")}"

        frontText.text = questionText
        backText.text = correctAnswerText
        fbClozeAnswerEditText.text.clear()
        frontText.visibility = View.VISIBLE
        fbClozeAnswerEditText.visibility = View.VISIBLE
        backText.visibility = View.GONE

        checkButton.setOnClickListener {
            val userAnswer = fbClozeAnswerEditText.text.toString().trim()
            val isCorrect = answers.any { it.equals(userAnswer, ignoreCase = true) }
            handleAnswer(card, isCorrect, if (isCorrect) "Correto!" else "Incorreto!")

            fbClozeAnswerEditText.visibility = View.GONE
            backText.visibility = View.VISIBLE
        }
    }
}