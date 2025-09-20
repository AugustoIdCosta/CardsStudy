package com.example.cardsstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Referências da UI
        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text_register)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text_register)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirm_password_edit_text_register)
        val registerButton = findViewById<Button>(R.id.register_button)
        val loginTextView = findViewById<TextView>(R.id.login_text_view)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Validações
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "As senhas não correspondem.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cria o usuário no Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                        // Navega para a tela principal
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finishAffinity() // Fecha a tela de login e cadastro
                    } else {
                        Toast.makeText(this, "Falha no cadastro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginTextView.setOnClickListener {
            // Volta para a tela de login
            finish()
        }
    }
}