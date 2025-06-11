package com.example.sportsapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.sportsapp.R
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityEventProfileBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.requests.EventUpdateRequest
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventProfileBinding
    private lateinit var tokenManager: TokenManager
    private var isEditMode = false
    private var selectedExcelUri: Uri? = null

    // Original values for cancel functionality
    private var originalEventName = ""
    private var originalEventDescription = ""
    private var originalEventType = ""
    private var originalEventCategory = ""
    private var originalEventAddress = ""
    private var originalStartDate = ""
    private var originalStartTime = ""
    private var originalEndDate = ""
    private var originalEndTime = ""
    private var originalCurrentParticipants = 0
    private var originalLimitParticipants = 0
    private var eventId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_event_profile)
        binding.layoutCompleteEvent.visibility = View.VISIBLE
        tokenManager = TokenManager(this)

        setupUI()
        loadEventData()
        setupClickListeners()
    }

    private fun setupUI() {
        // Animation
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        // Transparent status bar
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun loadEventData() {
        // Get data from intent
        val id = intent.getIntExtra("event_id", -1)
        eventId = if (id != -1) id.toString() else ""

        originalEventName = intent.getStringExtra("event_name") ?: ""
        originalEventDescription = intent.getStringExtra("event_description") ?: ""
        originalEventType = intent.getStringExtra("event_type") ?: ""
        originalEventCategory = intent.getStringExtra("event_category") ?: ""
        originalEventAddress = intent.getStringExtra("event_address") ?: ""
        originalStartDate = intent.getStringExtra("event_start_date") ?: ""
        originalStartTime = intent.getStringExtra("event_start_time") ?: ""
        originalEndDate = intent.getStringExtra("event_end_date") ?: ""
        originalEndTime = intent.getStringExtra("event_end_time") ?: ""
        originalCurrentParticipants = intent.getIntExtra("event_current_participants", 0)
        originalLimitParticipants = intent.getIntExtra("event_limit_participants", 0)
        val eventStatus = intent.getStringExtra("event_status") ?: ""

        // Set values to views
        binding.textEventName.text = originalEventName
        binding.textEventDescription.text = originalEventDescription
        binding.tvEventType.text = originalEventType
        binding.tvEventCategory.text = if (originalEventCategory == "all") "Все категории" else originalEventCategory
        binding.tvEventAddress.text = originalEventAddress
        binding.tvStartDateTime.text = formatDateTime(originalStartDate, originalStartTime)
        binding.tvEndDateTime.text = formatDateTime(originalEndDate, originalEndTime)
        binding.tvParticipantsCount.text = "$originalCurrentParticipants/$originalLimitParticipants"

        // Set progress
        val progress = if (originalLimitParticipants > 0) {
            ((originalCurrentParticipants.toFloat() / originalLimitParticipants.toFloat()) * 100).toInt()
        } else {
            0
        }
        binding.progressParticipants.progress = progress

        // Set status
        setEventStatus(eventStatus)
        updateJoinButton(eventStatus, originalCurrentParticipants, originalLimitParticipants)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            if (isEditMode) {
                exitEditMode()
            } else {
                onBackPressed()
            }
        }

        binding.btnSelectExcel.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            startActivityForResult(Intent.createChooser(intent, "Выберите Excel-файл"), 101)
        }

        binding.btnCompleteEvent.setOnClickListener {
            if (selectedExcelUri != null) {
                parseAndSendExcel(selectedExcelUri!!)
            } else {
                showErrorMessage("Выберите Excel-файл перед отправкой")
            }
        }


        binding.fabEdit.setOnClickListener {
            if (isEditMode) {
                saveChanges()
            } else {
                enterEditMode()
            }
        }

        binding.btnJoinEvent.setOnClickListener {
            if (isEditMode) {
                exitEditMode()
            } else {
                val intent = Intent(this, ParticipantsListActivity::class.java).apply {
                    putExtra("event_id", eventId.toIntOrNull() ?: -1)
                    putExtra("event_name", originalEventName)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

    }

    private fun enterEditMode() {
        isEditMode = true

        // Update FAB
        binding.fabEdit.setImageResource(R.drawable.ic_check)
        binding.fabEdit.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_color)

        // Update join button to cancel
        binding.btnJoinEvent.text = "ОТМЕНА"
        binding.btnJoinEvent.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error_color)
        binding.btnJoinEvent.icon = ContextCompat.getDrawable(this, R.drawable.ic_close)

        // Hide status card
        binding.statusContainer.visibility = View.GONE

        // Switch to editable fields
        binding.textEventName.visibility = View.GONE
        binding.etEventName.visibility = View.VISIBLE
        binding.etEventName.isEnabled = true
        binding.etEventName.setText(originalEventName)

        binding.textEventDescription.visibility = View.GONE
        binding.etEventDescription.visibility = View.VISIBLE
        binding.etEventDescription.isEnabled = true
        binding.etEventDescription.setText(originalEventDescription)

        binding.tvEventType.visibility = View.GONE
        binding.etEventType.visibility = View.VISIBLE
        binding.etEventType.isEnabled = true
        binding.etEventType.setText(originalEventType)

        binding.tvEventCategory.visibility = View.GONE
        binding.etEventCategory.visibility = View.VISIBLE
        binding.etEventCategory.isEnabled = true
        binding.etEventCategory.setText(if (originalEventCategory == "all") "Все категории" else originalEventCategory)

        binding.tvEventAddress.visibility = View.GONE
        binding.etEventAddress.visibility = View.VISIBLE
        binding.etEventAddress.isEnabled = true
        binding.etEventAddress.setText(originalEventAddress)

        showInfoMessage("Режим редактирования активирован")
    }

    private fun exitEditMode(showCancelMessage: Boolean = true) {
        isEditMode = false

        // Restore FAB
        binding.fabEdit.setImageResource(R.drawable.ic_edit)
        binding.fabEdit.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)

        // Restore join button
        binding.btnJoinEvent.text = "ПРОСМОТРЕТЬ СПИСОК УЧАСТНИКОВ"
        binding.btnJoinEvent.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
        binding.btnJoinEvent.icon = ContextCompat.getDrawable(this, R.drawable.ic_person_add)

        // Show status card
        binding.statusContainer.visibility = View.VISIBLE

        // Switch back to display fields and restore original values
        binding.etEventName.visibility = View.GONE
        binding.etEventName.isEnabled = false
        binding.textEventName.visibility = View.VISIBLE
        binding.textEventName.text = originalEventName

        binding.etEventDescription.visibility = View.GONE
        binding.etEventDescription.isEnabled = false
        binding.textEventDescription.visibility = View.VISIBLE
        binding.textEventDescription.text = originalEventDescription

        binding.etEventType.visibility = View.GONE
        binding.etEventType.isEnabled = false
        binding.tvEventType.visibility = View.VISIBLE
        binding.tvEventType.text = originalEventType

        binding.etEventCategory.visibility = View.GONE
        binding.etEventCategory.isEnabled = false
        binding.tvEventCategory.visibility = View.VISIBLE
        binding.tvEventCategory.text = if (originalEventCategory == "all") "Все категории" else originalEventCategory

        binding.etEventAddress.visibility = View.GONE
        binding.etEventAddress.isEnabled = false
        binding.tvEventAddress.visibility = View.VISIBLE
        binding.tvEventAddress.text = originalEventAddress

        if (showCancelMessage) {
            showInfoMessage("Изменения отменены")
        }
    }


    private fun saveChanges() {
        val currentEventName = binding.etEventName.text.toString().trim()
        val currentEventDescription = binding.etEventDescription.text.toString().trim()
        val currentEventType = binding.etEventType.text.toString().trim()
        val currentEventCategory = binding.etEventCategory.text.toString().trim()
        val currentEventAddress = binding.etEventAddress.text.toString().trim()

        if (currentEventName.isEmpty()) {
            binding.etEventName.error = "Введите название события"
            return
        }
        if (currentEventDescription.isEmpty()) {
            binding.etEventDescription.error = "Введите описание события"
            return
        }
        if (currentEventAddress.isEmpty()) {
            binding.etEventAddress.error = "Введите адрес"
            return
        }

        val updateRequest = EventUpdateRequest(
            id = eventId.toInt(),
            name = currentEventName,
            description = currentEventDescription,
            typeOfCompetition = originalEventType,
            startDate = originalStartDate,
            startTime = originalStartTime,
            endDate = originalEndDate,
            endTime = originalEndTime,
            address = currentEventAddress,
            aLotOfParticipant = originalCurrentParticipants,
            limitOfParticipants = originalLimitParticipants
        )



        val token = tokenManager.getToken()
        if (token != null) {
            ApiClient.apiService.updateEvent("Bearer $token", eventId.toInt(), updateRequest)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            showSuccessMessage("Изменения сохранены")
                            exitEditMode(showCancelMessage = false)
                        } else {
                            showErrorMessage("Ошибка при сохранении: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        showErrorMessage("Сетевая ошибка: ${t.message}")
                    }
                })
        } else {
            showErrorMessage("Ошибка авторизации: токен не найден")
        }
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

    private fun setEventStatus(status: String) {
        when (status.lowercase()) {
            "active" -> {
                binding.textEventStatus.text = "АКТИВНОЕ"
                binding.textEventStatus.setTextColor(ContextCompat.getColor(this, R.color.success_color))
                binding.statusContainer.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_light)
                binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_color)
            }
            "completed" -> {
                binding.textEventStatus.text = "ЗАВЕРШЕННОЕ"
                binding.textEventStatus.setTextColor(ContextCompat.getColor(this, R.color.secondary_text))
                binding.statusContainer.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
                binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.secondary_text)
            }
            else -> {
                binding.textEventStatus.text = status.uppercase()
                binding.textEventStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
                binding.statusContainer.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_light)
                binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
            }
        }
    }

    private fun updateJoinButton(status: String, current: Int, limit: Int) {
        when (status.lowercase()) {
            "active", "регистрация открыта", "регистрация закрыта", "идёт" -> {
                binding.btnJoinEvent.isEnabled = true
                binding.btnJoinEvent.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
            }
            else -> {
                binding.btnJoinEvent.isEnabled = false
                binding.btnJoinEvent.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
            }
        }
    }

    private fun showInfoMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_color))
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.success_color))
            .show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.error_color))
            .show()
    }

    override fun onBackPressed() {
        if (isEditMode) {
            exitEditMode()
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            selectedExcelUri = data?.data
            showInfoMessage("Файл выбран: ${selectedExcelUri?.lastPathSegment}")
        }
    }
    private fun parseAndSendExcel(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            val resultList = mutableListOf<Map<String, String>>()

            for (row in sheet.drop(1)) { // пропускаем заголовок
                val category = row.getCell(0)?.stringCellValue ?: continue
                val teamName = row.getCell(1)?.stringCellValue ?: continue
                val place = row.getCell(2)?.stringCellValue ?: continue

                resultList.add(
                    mapOf(
                        "category" to category,
                        "team" to teamName,
                        "place" to place
                    )
                )
            }

            sendCompletionResults(resultList)

        } catch (e: Exception) {
            showErrorMessage("Ошибка чтения Excel: ${e.localizedMessage}")
        }
    }
    private fun sendCompletionResults(results: List<Map<String, String>>) {
        val token = tokenManager.getToken() ?: return showErrorMessage("Нет токена")

        ApiClient.apiService.completeEvent(
            authHeader = "Bearer $token",
            eventId = eventId.toInt(),
            results = results
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showSuccessMessage("Событие успешно завершено")
                    finish()
                } else {
                    showErrorMessage("Ошибка завершения: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showErrorMessage("Сетевая ошибка: ${t.localizedMessage}")
            }
        })
    }
}