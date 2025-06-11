package com.example.sportsapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sportsapp.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.sportsapp.requests.RegisterRequest
import com.example.sportsapp.responses.RegisterResponse
import com.example.sportsapp.api.ApiClient

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                val request = RegisterRequest(
                    email = email,
                    password = password,
                    username = username
                )

                ApiClient.apiService.registerUser(request).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "На Ваш Email было отправлено письмо для подтверждения. Пожалуйста, перейдите по ссылке из письма. После подтверждения войдите в аккаунт.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(this@RegisterActivity, "Ошибка регистрации: ${response.code()}, $errorBody", Toast.LENGTH_LONG).show()
                            Log.e("RegisterActivity", "Error: $errorBody")
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@RegisterActivity, "Ошибка подключения к серверу: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterActivity", "Failure", t)
                    }
                })
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
