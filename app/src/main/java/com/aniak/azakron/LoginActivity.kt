package com.aniak.azakron

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aniak.azakron.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val WEB_CLIENT_ID = "814608088595-44g0d9aftqbas5e8ed016mrcb54qiopv.apps.googleusercontent.com"

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isUserLoggedIn()) {
            navigateToNotes()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(WEB_CLIENT_ID) // Usa o Web Client ID
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGoogleLogin.isEnabled = false

        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            saveUserSession(account)

            Toast.makeText(
                this,
                "Bem-vindo, ${account.displayName}!",
                Toast.LENGTH_SHORT
            ).show()

            navigateToNotes()

        } catch (e: ApiException) {
            binding.progressBar.visibility = View.GONE
            binding.btnGoogleLogin.isEnabled = true

            val errorMessage = when (e.statusCode) {
                10 -> "Erro de configuração. Verifique SHA-1 e Client ID"
                12501 -> "Login cancelado"
                7 -> "Sem conexão com a internet"
                else -> "Erro ao fazer login: ${e.statusCode}"
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserSession(account: GoogleSignInAccount) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", account.displayName)
            putString("user_email", account.email)
            putString("user_photo_url", account.photoUrl?.toString())
            putString("user_id", account.id)
            apply()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        return prefs.getBoolean("is_logged_in", false)
    }

    private fun navigateToNotes() {
        val intent = Intent(this, NotesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}