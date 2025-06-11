package com.example.sportsapp.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.sportsapp.R
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityChangePasswordBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.requests.ChangePasswordRequest
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_password)
        tokenManager = TokenManager(this)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Настройка статус бара
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Изменить пароль"
        }
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            validateAndChangePassword()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateAndChangePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Валидация
        if (currentPassword.isEmpty()) {
            binding.tilCurrentPassword.error = "Введите текущий пароль"
            return
        }

        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Введите новый пароль"
            return
        }

        if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Пароль должен содержать минимум 6 символов"
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Подтвердите новый пароль"
            return
        }

        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Пароли не совпадают"
            return
        }

        // Очистка ошибок
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        changePassword(currentPassword, newPassword)
    }

    private fun changePassword(oldPass: String, newPass: String) {
        val token = tokenManager.getToken()
        val data = mapOf(
            "old_password" to oldPass,
            "new_password" to newPass
        )

        ApiClient.apiService.changePassword("Bearer $token", data)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ChangePasswordActivity, "Пароль успешно изменен", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@ChangePasswordActivity, "Ошибка: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@ChangePasswordActivity, "Ошибка подключения", Toast.LENGTH_LONG).show()
                }
            })
    }


    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnChangePassword.isEnabled = false
            binding.btnCancel.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnChangePassword.isEnabled = true
            binding.btnCancel.isEnabled = true
        }
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_color))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.success_color))
            .setTextColor(getColor(R.color.white))
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}