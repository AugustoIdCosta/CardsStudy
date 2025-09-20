package com.example.cardsstudy

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class SummaryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CORRECT_COUNT = "extra_correct_count"
        const val EXTRA_INCORRECT_COUNT = "extra_incorrect_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val correctCount = intent.getIntExtra(EXTRA_CORRECT_COUNT, 0)
        val incorrectCount = intent.getIntExtra(EXTRA_INCORRECT_COUNT, 0)
        val total = correctCount + incorrectCount

        val percentage = if (total > 0) {
            (correctCount.toDouble() / total) * 100
        } else {
            0.0
        }
        val df = DecimalFormat("#.#")

        findViewById<TextView>(R.id.summary_correct_text).text = "Acertos: $correctCount"
        findViewById<TextView>(R.id.summary_incorrect_text).text = "Erros: $incorrectCount"
        findViewById<TextView>(R.id.summary_percentage_text).text = "Aproveitamento: ${df.format(percentage)}%"

        findViewById<Button>(R.id.summary_finish_button).setOnClickListener {
            finish()
        }
    }
}