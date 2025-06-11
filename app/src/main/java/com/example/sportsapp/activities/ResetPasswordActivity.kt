package com.example.sportsapp.activities

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.sportsapp.R
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityResetPasswordBinding
import com.example.sportsapp.requests.ResetPasswordRequest
import com.example.sportsapp.responses.ResetPasswordResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Настройка кнопки "Назад" в ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Сброс пароля"
        }
    }

    private fun setupClickListeners() {
        binding.btnResetPassword.setOnClickListener {
            showConfirmationDialog()
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupTextWatchers() {
        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.etEmail.error = null
        }
    }

    private fun showConfirmationDialog() {
        val email = binding.etEmail.text.toString().trim()

        if (!validateEmail(email)) return

        AlertDialog.Builder(this)
            .setTitle("Подтверждение сброса пароля")
            .setMessage("Вы точно хотите сбросить пароль для адреса:\n$email\n\nНа указанный email будет отправлена ссылка для создания нового пароля.")
            .setPositiveButton("Сбросить") { _, _ ->
                performPasswordReset(email)
            }
            .setNegativeButton("Отмена", null)
            .setIcon(R.drawable.ic_warning)
            .show()
    }

    private fun performPasswordReset(email: String) {
        showLoading(true)
        val request = ResetPasswordRequest(email)

        ApiClient.apiService.resetPassword(request).enqueue(object : Callback<ResetPasswordResponse> {
            override fun onResponse(
                call: Call<ResetPasswordResponse>,
                response: Response<ResetPasswordResponse>
            ) {
                showLoading(false)

                if (response.isSuccessful) {
                    showSuccessMessage("Ссылка для сброса пароля отправлена на ваш email!")

                    // Задержка перед возвратом к экрану входа
                    binding.root.postDelayed({
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }, 2000)
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Неверный формат email адреса"
                        404 -> "Пользователь с таким email не найден"
                        422 -> "Некорректные данные. Проверьте email адрес"
                        429 -> "Слишком много запросов. Попробуйте позже"
                        500 -> "Ошибка сервера. Попробуйте позже"
                        else -> "Ошибка сброса пароля: ${response.code()}"
                    }
                    showErrorMessage(errorMessage)
                }
            }

            override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                showLoading(false)
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true ->
                        "Превышено время ожидания. Проверьте подключение к интернету"
                    t.message?.contains("network", ignoreCase = true) == true ->
                        "Проблемы с сетью. Проверьте подключение к интернету"
                    else -> "Ошибка подключения к серверу"
                }
                showErrorMessage(errorMessage)
            }
        })
    }

    private fun validateEmail(email: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Введите email адрес"
                binding.etEmail.requestFocus()
                shakeButton()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Введите корректный email адрес"
                binding.etEmail.requestFocus()
                shakeButton()
                return false
            }
        }
        return true
    }

    private fun shakeButton() {
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.btnResetPassword.startAnimation(shakeAnimation)
    }

    private fun showLoading(show: Boolean) {
        binding.btnResetPassword.apply {
            text = if (show) "Отправка..." else "СБРОСИТЬ ПАРОЛЬ"
            isEnabled = !show
        }
        binding.etEmail.isEnabled = !show
        binding.btnBackToLogin.isEnabled = !show
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
                showConfirmationDialog()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}