package com.example.sportsapp.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.sportsapp.R
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityEventProfileForParticipantBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.requests.JoinEventRequest
import com.example.sportsapp.requests.LeaveEventRequest
import com.example.sportsapp.requests.JoinTeamRequest
import com.example.sportsapp.requests.CreateTeamRequest
import com.example.sportsapp.responses.CheckUserJoinStatusResponse
import com.example.sportsapp.responses.JoinEventResponse
import com.example.sportsapp.responses.TeamActionResponse
import com.example.sportsapp.ui.bottomsheets.TeamSelectionBottomSheet
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class EventProfileForParticipantActivity : AppCompatActivity(), TeamSelectionBottomSheet.TeamSelectionListener {

    private lateinit var binding: ActivityEventProfileForParticipantBinding
    private lateinit var tokenManager: TokenManager

    private var eventId: Int = -1
    private var isUserJoined: Boolean = false
    private var isSearchMode: Boolean = false
    private var currentParticipants: Int = 0
    private var limitParticipants: Int = 0
    private var eventStatus: String = ""
    private var eventType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_profile_for_participant)
        tokenManager = TokenManager(this)

        setupUI()
        loadEventData()
        setupClickListeners()
    }

    private fun setupUI() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        // Настройка обработчика кнопки "Назад"
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadEventData() {
        val intent = intent

        eventId = intent.getIntExtra("event_id", -1)
        isSearchMode = intent.getBooleanExtra("is_search_mode", false)

        val eventName = intent.getStringExtra("event_name") ?: ""
        val eventDescription = intent.getStringExtra("event_description") ?: ""
        eventType = intent.getStringExtra("event_type") ?: ""
        val eventCategory = intent.getStringExtra("event_category") ?: ""
        val startDate = intent.getStringExtra("event_start_date") ?: ""
        val startTime = intent.getStringExtra("event_start_time") ?: ""
        val endDate = intent.getStringExtra("event_end_date") ?: ""
        val endTime = intent.getStringExtra("event_end_time") ?: ""
        val address = intent.getStringExtra("event_address") ?: ""
        currentParticipants = intent.getIntExtra("event_current_participants", 0)
        limitParticipants = intent.getIntExtra("event_limit_participants", 0)
        eventStatus = intent.getStringExtra("event_status") ?: ""

        // Заполнение данных
        binding.textEventName.text = eventName
        binding.textEventDescription.text = eventDescription
        binding.tvEventType.text = eventType
        binding.tvEventCategory.text = eventCategory
        binding.tvEventAddress.text = address

        // Форматирование даты и времени
        binding.tvStartDateTime.text = formatDateTime(startDate, startTime)
        binding.tvEndDateTime.text = formatDateTime(endDate, endTime)

        // Участники
        val participantsText = "$currentParticipants из $limitParticipants участников"
        binding.textEventParticipants.text = participantsText

        // Прогресс участников
        setupParticipantsProgress()

        // Статус события
        setupEventStatus()

        // Кнопка присоединения
        updateJoinButton(eventStatus, currentParticipants, limitParticipants)

        // Если это режим поиска, проверяем статус участия пользователя
        if (isSearchMode && eventId != -1) {
            checkUserJoinStatus()
        } else {
            // Если это "Мои события", скрываем кнопку присоединения
            binding.btnJoinEvent.visibility = View.GONE
        }
    }

    private fun setupParticipantsProgress() {
        val progress = if (limitParticipants > 0) {
            ((currentParticipants.toFloat() / limitParticipants.toFloat()) * 100).toInt()
        } else {
            0
        }

        binding.progressParticipants.progress = progress
        binding.textProgressPercent.text = "$progress%"

        // Изменение цвета прогресс-бара
        when {
            progress >= 90 -> {
                binding.progressParticipants.progressTintList =
                    ContextCompat.getColorStateList(this, R.color.error_color)
            }
            progress >= 70 -> {
                binding.progressParticipants.progressTintList =
                    ContextCompat.getColorStateList(this, R.color.warning_color)
            }
            else -> {
                binding.progressParticipants.progressTintList =
                    ContextCompat.getColorStateList(this, R.color.success_color)
            }
        }
    }

    private fun setupEventStatus() {
        when (eventStatus.lowercase()) {
            "pending" -> {
                binding.textEventStatus.text = "ОЖИДАЕТ ПРОВЕРКИ"
                binding.textEventStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_color))
                binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.warning_color))
            }
            else -> {
                binding.textEventStatus.text = eventStatus.uppercase()
                binding.textEventStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
                binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnJoinEvent.setOnClickListener {
            if (isUserJoined) {
                showLeaveConfirmationDialog()
            } else {
                handleJoinEventClick()
            }
        }

        // Кнопка поделиться
        binding.btnShare.setOnClickListener {
            shareEvent()
        }
    }

    /**
     * Обработка нажатия на кнопку присоединения к событию
     * Проверяет тип события: командный или индивидуальный
     */
    private fun handleJoinEventClick() {
        when (eventType.lowercase()) {
            "командный", "team", "команда" -> {
                showTeamSelectionBottomSheet()
            }
            else -> {
                showJoinConfirmationDialog()
            }
        }
    }

    /**
     * Показывает BottomSheet для выбора команды
     */
    private fun showTeamSelectionBottomSheet() {
        val bottomSheet = TeamSelectionBottomSheet.newInstance(eventId)
        bottomSheet.setTeamSelectionListener(this)
        bottomSheet.show(supportFragmentManager, "TeamSelectionBottomSheet")
    }

    /**
     * Обработка присоединения к команде (из TeamSelectionBottomSheet)
     */
    override fun onJoinTeam(eventId: Int, teamName: String) {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        val request = JoinTeamRequest(eventId, teamName)
        ApiClient.apiService.joinTeam(request, "Bearer $token")
            .enqueue(object : Callback<TeamActionResponse> {
                override fun onResponse(
                    call: Call<TeamActionResponse>,
                    response: Response<TeamActionResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody?.success == true) {
                            isUserJoined = true
                            currentParticipants++
                            updateEventData()
                            showSuccessMessage("Вы успешно присоединились к команде \"$teamName\"!")
                        } else {
                            showErrorMessage(responseBody?.message ?: "Ошибка присоединения к команде")
                        }
                    } else {
                        handleTeamActionError(response.code(), "присоединения к команде")
                    }
                }

                override fun onFailure(call: Call<TeamActionResponse>, t: Throwable) {
                    showLoading(false)
                    showErrorMessage("Ошибка подключения к серверу")
                }
            })
    }

    /**
     * Обработка создания новой команды (из TeamSelectionBottomSheet)
     */
    override fun onCreateTeam(eventId: Int, teamName: String) {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        val request = CreateTeamRequest(eventId, teamName)
        ApiClient.apiService.createTeam(request, "Bearer $token")
            .enqueue(object : Callback<TeamActionResponse> {
                override fun onResponse(
                    call: Call<TeamActionResponse>,
                    response: Response<TeamActionResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody?.success == true) {
                            isUserJoined = true
                            currentParticipants++
                            updateEventData()
                            showSuccessMessage("Команда \"$teamName\" создана! Вы автоматически присоединились к событию.")
                        } else {
                            showErrorMessage(responseBody?.message ?: "Ошибка создания команды")
                        }
                    } else {
                        handleTeamActionError(response.code(), "создания команды")
                    }
                }

                override fun onFailure(call: Call<TeamActionResponse>, t: Throwable) {
                    showLoading(false)
                    showErrorMessage("Ошибка подключения к серверу")
                }
            })
    }

    /**
     * Обработка ошибок при работе с командами
     */
    private fun handleTeamActionError(code: Int, action: String) {
        val errorMessage = when (code) {
            400 -> "Невозможно выполнить $action. Проверьте введенные данные"
            401 -> "Ошибка авторизации"
            403 -> "Нет прав для выполнения операции"
            404 -> "Событие или команда не найдены"
            409 -> when (action) {
                "присоединения к команде" -> "Вы уже участвуете в этом событии или команда заполнена"
                "создания команды" -> "Команда с таким названием уже существует"
                else -> "Конфликт данных"
            }
            422 -> "Неверные данные команды"
            500 -> "Ошибка сервера. Попробуйте позже"
            else -> "Ошибка $action: $code"
        }
        showErrorMessage(errorMessage)
    }

    private fun updateJoinButton(status: String, currentParticipants: Int, limitParticipants: Int) {
        when (status.lowercase()) {
            "completed", "завершено", "завершенный" -> {
                binding.btnJoinEvent.text = "СОБЫТИЕ ЗАВЕРШЕНО"
                binding.btnJoinEvent.isEnabled = false
                setButtonStyle(binding.btnJoinEvent, R.color.light_gray, R.color.secondary_text)
            }
            "идёт", "идет", "в процессе" -> {
                binding.btnJoinEvent.text = "СОБЫТИЕ ИДЁТ"
                binding.btnJoinEvent.isEnabled = false
                setButtonStyle(binding.btnJoinEvent, R.color.light_gray, R.color.secondary_text)
            }
            "регистрация закрыта" -> {
                binding.btnJoinEvent.text = "РЕГИСТРАЦИЯ ЗАКРЫТА"
                binding.btnJoinEvent.isEnabled = false
                setButtonStyle(binding.btnJoinEvent, R.color.light_gray, R.color.secondary_text)
            }
            "active", "активный", "регистрация открыта", "запланировано" -> {
                if (currentParticipants >= limitParticipants) {
                    binding.btnJoinEvent.text = "МЕСТ НЕТ"
                    binding.btnJoinEvent.isEnabled = false
                    setButtonStyle(binding.btnJoinEvent, R.color.light_gray, R.color.secondary_text)
                } else if (isUserJoined) {
                    binding.btnJoinEvent.text = "ПОКИНУТЬ СОБЫТИЕ"
                    binding.btnJoinEvent.isEnabled = true
                    setButtonStyle(binding.btnJoinEvent, R.color.error_color, R.color.white)
                } else {
                    val buttonText = when (eventType.lowercase()) {
                        "командный", "team", "команда" -> "ПРИСОЕДИНИТЬСЯ К КОМАНДЕ"
                        else -> "ПРИСОЕДИНИТЬСЯ"
                    }
                    binding.btnJoinEvent.text = buttonText
                    binding.btnJoinEvent.isEnabled = true
                    setButtonStyle(binding.btnJoinEvent, R.color.primary_color, R.color.white)
                }
            }
            else -> {
                binding.btnJoinEvent.text = "НЕДОСТУПНО"
                binding.btnJoinEvent.isEnabled = false
                setButtonStyle(binding.btnJoinEvent, R.color.light_gray, R.color.secondary_text)
            }
        }
    }


    private fun checkUserJoinStatus() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        ApiClient.apiService.checkUserJoinStatus(eventId, "Bearer $token")
            .enqueue(object : Callback<CheckUserJoinStatusResponse> {
                override fun onResponse(
                    call: Call<CheckUserJoinStatusResponse>,
                    response: Response<CheckUserJoinStatusResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        isUserJoined = response.body()?.isJoined ?: false
                        updateJoinButton(eventStatus, currentParticipants, limitParticipants)
                    }
                }

                override fun onFailure(call: Call<CheckUserJoinStatusResponse>, t: Throwable) {
                    showLoading(false)
                    // Продолжаем работу без информации о статусе
                }
            })
    }

    private fun showJoinConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Присоединиться к событию")
            .setMessage("Вы точно хотите присоединиться к этому событию?")
            .setPositiveButton("Да") { _, _ ->
                joinEvent()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showLeaveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Покинуть событие")
            .setMessage("Вы точно хотите покинуть это событие?")
            .setPositiveButton("Да") { _, _ ->
                leaveEvent()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun joinEvent() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        val request = JoinEventRequest(eventId)
        ApiClient.apiService.joinEvent(request, "Bearer $token")
            .enqueue(object : Callback<JoinEventResponse> {
                override fun onResponse(call: Call<JoinEventResponse>, response: Response<JoinEventResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        isUserJoined = true
                        currentParticipants++
                        updateEventData()
                        showSuccessMessage("Вы успешно присоединились к событию!")
                    } else {
                        handleJoinError(response.code())
                    }
                }

                override fun onFailure(call: Call<JoinEventResponse>, t: Throwable) {
                    showLoading(false)
                    showErrorMessage("Ошибка подключения к серверу")
                }
            })
    }

    private fun formatDateTime(date: String, time: String): String {
        return try {
            val serverDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))

            val parsedDate = serverDateFormat.parse(date)
            val formattedDate = displayDateFormat.format(parsedDate!!)

            "$formattedDate в $time"
        } catch (e: Exception) {
            "$date в $time"
        }
    }

    private fun leaveEvent() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        val request = LeaveEventRequest(eventId)
        ApiClient.apiService.leaveEvent(request, "Bearer $token")
            .enqueue(object : Callback<JoinEventResponse> {
                override fun onResponse(call: Call<JoinEventResponse>, response: Response<JoinEventResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        isUserJoined = false
                        currentParticipants--
                        updateEventData()
                        showSuccessMessage("Вы покинули событие")
                    } else {
                        handleJoinError(response.code())
                    }
                }

                override fun onFailure(call: Call<JoinEventResponse>, t: Throwable) {
                    showLoading(false)
                    showErrorMessage("Ошибка подключения к серверу")
                }
            })
    }

    private fun updateEventData() {
        // Обновляем отображение участников
        val participantsText = "$currentParticipants из $limitParticipants участников"
        binding.textEventParticipants.text = participantsText

        // Обновляем прогресс
        setupParticipantsProgress()

        // Обновляем кнопку
        updateJoinButton(eventStatus, currentParticipants, limitParticipants)
    }

    private fun handleJoinError(code: Int) {
        val errorMessage = when (code) {
            400 -> "Невозможно присоединиться к событию"
            401 -> "Ошибка авторизации"
            403 -> "Нет прав для выполнения операции"
            404 -> "Событие не найдено"
            409 -> "Вы уже участвуете в этом событии"
            500 -> "Ошибка сервера. Попробуйте позже"
            else -> "Ошибка выполнения операции: $code"
        }
        showErrorMessage(errorMessage)
    }

    private fun shareEvent() {
        val shareText = "Присоединяйся к спортивному событию: ${binding.textEventName.text}\n" +
                "Дата: ${binding.tvStartDateTime.text}\n" +
                "Место: ${binding.tvEventAddress.text}"

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }

        startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться событием"))
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.loadingContainer.visibility = View.VISIBLE
            binding.btnJoinEvent.isEnabled = false
            binding.btnShare.isEnabled = false

            // Добавляем анимацию появления
            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            binding.loadingContainer.startAnimation(fadeIn)
        } else {
            binding.loadingContainer.visibility = View.GONE
            binding.btnJoinEvent.isEnabled = true
            binding.btnShare.isEnabled = true

            // Добавляем анимацию исчезновения
            val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            binding.loadingContainer.startAnimation(fadeOut)
        }
    }

    private fun setButtonStyle(button: com.google.android.material.button.MaterialButton,
                               backgroundColor: Int,
                               textColor: Int,
                               strokeColor: Int? = null) {
        button.setBackgroundColor(ContextCompat.getColor(this, backgroundColor))
        button.setTextColor(ContextCompat.getColor(this, textColor))

        if (strokeColor != null) {
            button.strokeColor = ContextCompat.getColorStateList(this, strokeColor)
        }

        // Добавляем ripple эффект
        button.rippleColor = ContextCompat.getColorStateList(this, android.R.color.white)
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.error_color))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.success_color))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}