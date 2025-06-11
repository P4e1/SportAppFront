package com.example.sportsapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.sportsapp.R
import com.example.sportsapp.managers.RoleManager
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityLoginBinding
import com.example.sportsapp.requests.LoginRequest
import com.example.sportsapp.responses.LoginResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ApiClient.init(this)

        setupUI()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)

        binding.ivLogo?.startAnimation(bounceAnimation)
        binding.root.startAnimation(fadeInAnimation)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupTextWatchers() {
        binding.etUsername.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etPassword.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInput(username, password)) return

        showLoading(true)
        val request = LoginRequest(username, password)

        ApiClient.apiService.loginUser(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        val token = loginResponse.access
                        if (!token.isNullOrEmpty()) {
                            val tokenManager = TokenManager(this@LoginActivity)
                            tokenManager.saveToken(token)
                            tokenManager.saveRefreshToken(loginResponse.refresh) // <--- Добавлено
                        }

                        val roleCheck = loginResponse.role
                        if (!roleCheck.isNullOrEmpty()) { // Изменено: добавлена проверка на null
                            val roleCheckManager = RoleManager(this@LoginActivity)
                            roleCheckManager.saveRole(roleCheck)
                        }
                        showSuccessMessage("Успешный вход в систему!")

                        binding.root.postDelayed({
                            // Исправлена проверка role - теперь безопасно для null
                            val intent = if (loginResponse.role.isNullOrEmpty()) {
                                Intent(this@LoginActivity, FillingOutProfileActivity::class.java)
                            } else {
                                Intent(this@LoginActivity, MainActivity::class.java)
                            }
                            startActivity(intent)
                            finish()
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        }, 1000)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Email не подтвержден. Пожалуйста, подтвердите ваш email."
                        401 -> "Неверный логин или пароль"
                        422 -> "Некорректные данные для входа"
                        500 -> "Ошибка сервера. Попробуйте позже"
                        else -> "Ошибка входа: ${response.code()}"
                    }
                    showErrorMessage(errorMessage)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showLoading(false)
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> "Превышено время ожидания. Проверьте подключение к интернету"
                    t.message?.contains("network", ignoreCase = true) == true -> "Проблемы с сетью. Проверьте подключение к интернету"
                    else -> "Ошибка подключения к серверу"
                }
                showErrorMessage(errorMessage)
            }
        })
    }

    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.etUsername.error = "Введите логин или email"
            binding.etUsername.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Введите пароль"
            if (isValid) binding.etPassword.requestFocus()
            isValid = false
        }

        if (!isValid) {
            val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.btnLogin.startAnimation(shakeAnimation)
        }

        return isValid
    }

    private fun clearFieldErrors() {
        binding.etUsername.error = null
        binding.etPassword.error = null
    }

    private fun showLoading(show: Boolean) {
        binding.btnLogin.text = if (show) "Вход..." else "ВОЙТИ"
        binding.btnLogin.isEnabled = !show
        binding.etUsername.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.tvRegister.isEnabled = !show
        binding.tvForgotPassword.isEnabled = !show
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.success_color))
            .setTextColor(getColor(R.color.white))
            .setAction("OK") { }
            .show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_color))
            .setTextColor(getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                performLogin()
            }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.primary_color))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in)
    }
}