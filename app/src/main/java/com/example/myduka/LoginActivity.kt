package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    lateinit var loginBinding: ActivityLoginBinding
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        val view = loginBinding.root
        setContentView(view)


        loginBinding.buttonSignIn.setOnClickListener {
            loginBinding.progressBarSignIn.visibility = View.VISIBLE
            signIn()

        }
        loginBinding.textViewSignUp.setOnClickListener {
            val intent = Intent(this,SignUp::class.java)
            startActivity(intent)

        }
        loginBinding.textViewForgotPassword.setOnClickListener {
            val intent = Intent(this,ForgotPassword::class.java)
            startActivity(intent)

        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun signIn(){
        val email = loginBinding.editTextEmailSignIn.text.toString()
        val password = loginBinding.editTextPasswordSignIn.text.toString()
        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this,"Email or Password cannot be empty",Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val intent = Intent(this, PasswordLogin::class.java)
                startActivity(intent)
                finish()
            }else{
                Toast.makeText(this,task.exception?.localizedMessage,Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser

        if(user != null){
            Toast.makeText(this,"Welcome Back",Toast.LENGTH_SHORT).show()
            val intent = Intent(this,Dashboard::class.java)
            startActivity(intent)
            finish()
        }
    }
}