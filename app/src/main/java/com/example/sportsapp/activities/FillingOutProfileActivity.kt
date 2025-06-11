package com.example.sportsapp.activities

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.sportsapp.R
import com.example.sportsapp.managers.RoleManager
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityFillingOutProfileBinding
import com.example.sportsapp.requests.OrganizerProfileRequest
import com.example.sportsapp.requests.ParticipantProfileRequest
import com.example.sportsapp.responses.ProfileResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class FillingOutProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFillingOutProfileBinding
    private lateinit var tokenManager: TokenManager
    private var selectedRole: String = ""
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_filling_out_profile)
        tokenManager = TokenManager(this)

        setupUI()
        setupRoleSpinner()
        setupClickListeners()
        setupTextWatchers()
        setupPhoneFormatting()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Скрываем все группы полей по умолчанию
        binding.participantFields.visibility = View.GONE
        binding.organizerFields.visibility = View.GONE
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Участник", "Организатор")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)

        val autoCompleteTextView = binding.etRole

        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 1

        autoCompleteTextView.setOnClickListener {
            if (!autoCompleteTextView.isPopupShowing) {
                autoCompleteTextView.showDropDown()
            }
        }

        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            selectedRole = roles[position]
            showFieldsForRole(selectedRole)

            autoCompleteTextView.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
        }

        // Предотвратить ввод текста
        autoCompleteTextView.keyListener = null
    }

    private fun setupPhoneFormatting() {
        // Форматирование основного телефона
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val phonePattern = Pattern.compile("^\\+7\\s\\(\\d{3}\\)\\s\\d{3}-\\d{2}-\\d{2}$")

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true
                val formatted = formatPhoneNumber(s.toString())
                binding.etPhone.setText(formatted)
                binding.etPhone.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // Форматирование телефона организации
        binding.etOrganizationPhone.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true
                val formatted = formatPhoneNumber(s.toString())
                binding.etOrganizationPhone.setText(formatted)
                binding.etOrganizationPhone.setSelection(formatted.length)
                isUpdating = false
            }
        })
    }

    private fun formatPhoneNumber(input: String): String {
        // Удаляем все символы кроме цифр
        val digits = input.replace(Regex("[^\\d]"), "")

        return when {
            digits.isEmpty() -> ""
            digits.length == 1 && digits[0] == '7' -> "+7"
            digits.length == 1 && digits[0] == '8' -> "+7"
            digits.startsWith("8") -> {
                val withoutFirst = digits.substring(1)
                formatRussianNumber(withoutFirst)
            }
            digits.startsWith("7") -> {
                val withoutFirst = digits.substring(1)
                formatRussianNumber(withoutFirst)
            }
            else -> formatRussianNumber(digits)
        }
    }

    private fun formatRussianNumber(digits: String): String {
        return when {
            digits.isEmpty() -> "+7"
            digits.length <= 3 -> "+7 ($digits"
            digits.length <= 6 -> "+7 (${digits.substring(0, 3)}) ${digits.substring(3)}"
            digits.length <= 8 -> "+7 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            digits.length <= 10 -> "+7 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}"
            else -> "+7 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8, 10)}"
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val phonePattern = Pattern.compile("^\\+7\\s\\(\\d{3}\\)\\s\\d{3}-\\d{2}-\\d{2}$")
        return phonePattern.matcher(phone).matches()
    }

    private fun showFieldsForRole(role: String) {
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        when (role) {
            "Участник" -> {
                binding.organizerFields.visibility = View.GONE
                binding.participantFields.visibility = View.VISIBLE
                binding.participantFields.startAnimation(slideUpAnimation)
            }
            "Организатор" -> {
                binding.participantFields.visibility = View.GONE
                binding.organizerFields.visibility = View.VISIBLE
                binding.organizerFields.startAnimation(slideUpAnimation)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupTextWatchers() {
        // Общие поля
        binding.etFullName.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etPhone.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }

        // Участник
        binding.etBirthDate.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etHeight.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etWeight.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }

        // Организатор
        binding.etOrganizationName.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etOrganizationEmail.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etOrganizationPhone.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
        binding.etOrganizationAddress.doOnTextChanged { _, _, _, _ -> clearFieldErrors() }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.etBirthDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Устанавливаем максимальную дату (сегодня)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveProfile() {
        // Проверяем общие поля
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (!validateCommonFields(fullName, phone)) return

        if (selectedRole.isEmpty()) {
            showErrorMessage("Выберите роль")
            return
        }

        when (selectedRole) {
            "Участник" -> saveParticipantProfile()
            "Организатор" -> saveOrganizerProfile()
        }
    }

    private fun validateCommonFields(fullName: String, phone: String): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Введите ФИО"
            isValid = false
        } else if (fullName.length < 2) {
            binding.tilFullName.error = "ФИО должно содержать минимум 2 символа"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Введите номер телефона"
            isValid = false
        } else if (!isValidPhoneNumber(phone)) {
            binding.tilPhone.error = "Введите корректный номер телефона в формате +7 (XXX) XXX-XX-XX"
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        return isValid
    }

    private fun saveParticipantProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val birthDate = binding.etBirthDate.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val role = selectedRole.trim()
        val token = tokenManager.getToken()

        if (!role.isNullOrEmpty()) { // Изменено: добавлена проверка на null
            val roleManager = RoleManager(this@FillingOutProfileActivity)
            roleManager.saveRole(role)
        }
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            showLoading(false)
            return
        }

        if (!validateParticipantInput(birthDate, height, weight)) return

        showLoading(true)

        // Конвертируем дату из dd.MM.yyyy в yyyy-MM-dd для сервера
        val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val formattedDate = try {
            val date = displayDateFormat.parse(birthDate)
            serverDateFormat.format(date!!)
        } catch (e: Exception) {
            showErrorMessage("Ошибка формата даты")
            showLoading(false)
            return
        }

        val request = ParticipantProfileRequest(
            fullname = fullName,
            phone = phone,
            dateR = formattedDate, // Теперь используем dateR вместо birth_date
            height = height.toIntOrNull() ?: 0,
            weight = weight.toIntOrNull() ?: 0,
            role = role
        )

        ApiClient.apiService.createParticipantProfile("Bearer $token", request).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                showLoading(false)
                handleProfileResponse(response)
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                showLoading(false)
                handleNetworkError(t)
            }
        })
    }

    private fun saveOrganizerProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val organizationName = binding.etOrganizationName.text.toString().trim()
        val organizationEmail = binding.etOrganizationEmail.text.toString().trim()
        val organizationPhone = binding.etOrganizationPhone.text.toString().trim()
        val organizationAddress = binding.etOrganizationAddress.text.toString().trim()
        val token = tokenManager.getToken()
        val role = selectedRole.trim()

        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            showLoading(false)
            return
        }

        if (!validateOrganizerInput(organizationName, organizationEmail, organizationPhone, organizationAddress)) return

        showLoading(true)

        val request = OrganizerProfileRequest(
            fullname = fullName,
            phone = phone,
            orgName = organizationName,
            orgEmail = organizationEmail,
            orgPhone = organizationPhone,
            orgAddress = organizationAddress,
            role = role
        )

        ApiClient.apiService.createOrganizerProfile("Bearer $token", request).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                showLoading(false)
                handleProfileResponse(response)
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                showLoading(false)
                handleNetworkError(t)
            }
        })
    }

    private fun validateParticipantInput(birthDate: String, height: String, weight: String): Boolean {
        var isValid = true

        if (birthDate.isEmpty()) {
            binding.tilBirthDate.error = "Выберите дату рождения"
            isValid = false
        } else {
            binding.tilBirthDate.error = null
        }

        if (height.isEmpty()) {
            binding.tilHeight.error = "Введите рост"
            isValid = false
        } else if (height.toIntOrNull() == null || height.toInt() < 50 || height.toInt() > 250) {
            binding.tilHeight.error = "Введите корректный рост (50-250 см)"
            isValid = false
        } else {
            binding.tilHeight.error = null
        }

        if (weight.isEmpty()) {
            binding.tilWeight.error = "Введите вес"
            isValid = false
        } else if (weight.toDoubleOrNull() == null || weight.toDouble() < 20 || weight.toDouble() > 300) {
            binding.tilWeight.error = "Введите корректный вес (20-300 кг)"
            isValid = false
        } else {
            binding.tilWeight.error = null
        }

        if (!isValid) {
            val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.btnSave.startAnimation(shakeAnimation)
        }

        return isValid
    }

    private fun validateOrganizerInput(name: String, email: String, phone: String, address: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilOrganizationName.error = "Введите название организации"
            isValid = false
        } else {
            binding.tilOrganizationName.error = null
        }

        if (email.isEmpty()) {
            binding.tilOrganizationEmail.error = "Введите email организации"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilOrganizationEmail.error = "Введите корректный email"
            isValid = false
        } else {
            binding.tilOrganizationEmail.error = null
        }

        if (phone.isEmpty()) {
            binding.tilOrganizationPhone.error = "Введите номер телефона"
            isValid = false
        } else if (!isValidPhoneNumber(phone)) {
            binding.tilOrganizationPhone.error = "Введите корректный номер телефона в формате +7 (XXX) XXX-XX-XX"
            isValid = false
        } else {
            binding.tilOrganizationPhone.error = null
        }

        if (address.isEmpty()) {
            binding.tilOrganizationAddress.error = "Введите юридический адрес"
            isValid = false
        } else {
            binding.tilOrganizationAddress.error = null
        }



        if (!isValid) {
            val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.btnSave.startAnimation(shakeAnimation)
        }

        return isValid
    }

    private fun handleProfileResponse(response: Response<ProfileResponse>) {
        if (response.isSuccessful) {
            showSuccessMessage("Профиль успешно создан!")

            binding.root.postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }, 1500)
        } else {
            val errorMessage = when (response.code()) {
                400 -> "Некорректные данные профиля"
                401 -> "Ошибка авторизации"
                422 -> "Ошибка валидации данных"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка создания профиля: ${response.code()}"
            }
            showErrorMessage(errorMessage)
        }
    }

    private fun handleNetworkError(t: Throwable) {
        val errorMessage = when {
            t.message?.contains("timeout", ignoreCase = true) == true -> "Превышено время ожидания. Проверьте подключение к интернету"
            t.message?.contains("network", ignoreCase = true) == true -> "Проблемы с сетью. Проверьте подключение к интернету"
            else -> "Ошибка подключения к серверу"
        }
        showErrorMessage(errorMessage)
    }

    private fun clearFieldErrors() {
        binding.tilFullName.error = null
        binding.tilPhone.error = null
        binding.tilBirthDate.error = null
        binding.tilHeight.error = null
        binding.tilWeight.error = null
        binding.tilOrganizationName.error = null
        binding.tilOrganizationEmail.error = null
        binding.tilOrganizationPhone.error = null
        binding.tilOrganizationAddress.error = null
    }

    private fun showLoading(show: Boolean) {
        binding.btnSave.text = if (show) "Сохранение..." else "СОХРАНИТЬ"
        binding.btnSave.isEnabled = !show

        // Отключаем все поля во время загрузки
        binding.tilFullName.isEnabled = !show
        binding.tilPhone.isEnabled = !show
        binding.tilRole.isEnabled = !show
        binding.etBirthDate.isEnabled = !show
        binding.etHeight.isEnabled = !show
        binding.etWeight.isEnabled = !show
        binding.etOrganizationName.isEnabled = !show
        binding.etOrganizationEmail.isEnabled = !show
        binding.etOrganizationPhone.isEnabled = !show
        binding.etOrganizationAddress.isEnabled = !show
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
                saveProfile()
            }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in)
    }
}