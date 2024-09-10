package com.example.stfapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var userIdEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val PREFS_NAME = "LoginPrefs"
    private val PREF_KEY_USER_ID = "userId"
    private val PREF_KEY_LOGGED_IN = "is_logged_in"  // Consistent key name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firestore = FirebaseFirestore.getInstance()
        userIdEditText = findViewById(R.id.user_id_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (isLoggedIn()) {
            navigateToMainActivity()
        }

        loginButton.setOnClickListener {
            val userId = userIdEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (userId.isNotEmpty() && password.isNotEmpty()) {
                validateLogin(userId, password)
            } else {
                Toast.makeText(this, "Please enter User ID and Password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateLogin(userId: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    // Assuming the userId field is unique, we should have only one document
                    val document = querySnapshot.documents[0]
                    val storedPassword = document.getString("password")
                    if (storedPassword == password) {
                        // Password matches
                        saveLoginState(userId)
                        navigateToMainActivity()
                    } else {
                        // Password does not match
                        Toast.makeText(this, "Invalid Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // User ID does not exist
                    Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error validating login", e)
                Toast.makeText(this, "Error validating login", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveLoginState(userId: String) {
        val editor = sharedPreferences.edit()
        editor.putString(PREF_KEY_USER_ID, userId)
        editor.putBoolean(PREF_KEY_LOGGED_IN, true)
        editor.apply()
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(PREF_KEY_LOGGED_IN, false)
    }

    private fun navigateToMainActivity() {
        Log.d("LoginActivity", "Navigating to MainActivity after successful login")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
