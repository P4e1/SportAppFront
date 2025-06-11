package com.example.sportsapp.activities

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportsapp.R
import com.example.sportsapp.adapters.ParticipantsAdapter
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityParticipantsListBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.Participant
import com.google.android.material.snackbar.Snackbar
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ParticipantsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParticipantsListBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var participantsAdapter: ParticipantsAdapter
    private var eventId = ""
    private var participants = mutableListOf<Participant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_participants_list)
        tokenManager = TokenManager(this)

        setupUI()
        loadEventData()
        setupRecyclerView()
        setupClickListeners()
        loadParticipants()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Установка цвета индикатора обновления
        binding.swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_color)
        )
    }


    private fun loadEventData() {
        val id = intent.getIntExtra("event_id", -1)
        eventId = if (id != -1) id.toString() else ""
        val eventName = intent.getStringExtra("event_name") ?: "Участники события"
        binding.tvEventName.text = eventName
    }

    private fun setupRecyclerView() {
        participantsAdapter = ParticipantsAdapter(participants) { participant ->
            showParticipantDialog(participant)
        }

        binding.recyclerViewParticipants.apply {
            layoutManager = LinearLayoutManager(this@ParticipantsListActivity)
            adapter = participantsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadParticipants()
        }
    }

    private fun loadParticipants() {
        if (eventId.isEmpty()) {
            showErrorMessage("Ошибка: отсутствует ID события")
            return
        }

        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = true

        ApiClient.apiService.getEventParticipants("Bearer $token", eventId.toInt())
            .enqueue(object : Callback<List<Participant>> {
                override fun onResponse(call: Call<List<Participant>>, response: Response<List<Participant>>) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (response.isSuccessful) {
                        val participantsList = response.body() ?: emptyList()
                        updateParticipantsList(participantsList)
                    } else {
                        when (response.code()) {
                            401 -> showErrorMessage("Ошибка авторизации")
                            403 -> showErrorMessage("Нет прав для просмотра участников")
                            404 -> showErrorMessage("Событие не найдено")
                            else -> showErrorMessage("Ошибка сервера: ${response.code()}")
                        }
                    }
                }

                override fun onFailure(call: Call<List<Participant>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    showErrorMessage("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun updateParticipantsList(newParticipants: List<Participant>) {
        participants.clear()
        participants.addAll(newParticipants)
        participantsAdapter.notifyDataSetChanged()

        // Update UI based on participants count
        if (participants.isEmpty()) {
            binding.recyclerViewParticipants.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewParticipants.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }

        // Update participants count
        binding.tvParticipantsCount.text = "${participants.size} участников"
    }

    private fun showParticipantDialog(participant: Participant) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_participant_details, null)

        // Set participant data
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantName).text = participant.fullname
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantTeam).text = "Команда: ${participant.team}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantBirthDate).text = "Дата рождения: ${participant.dateR}"
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantHeight).text = "Рост: ${participant.height} см"
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantWeight).text = "Вес: ${participant.weight} кг"
        dialogView.findViewById<android.widget.TextView>(R.id.tvParticipantStatus).text = "Статус: ${participant.teamParticipantStatus}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Setup buttons
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmParticipant)
        if (participant.teamParticipantStatus == "Подтверждён") {
            btnConfirm.visibility = View.GONE
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmParticipant).setOnClickListener {
            confirmParticipant(participant.id)
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRejectParticipant).setOnClickListener {
            rejectParticipant(participant.id)
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmParticipant(participantId: Int) {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showInfoMessage("Подтверждение участника...")

        ApiClient.apiService.confirmParticipant("Bearer $token", eventId.toInt(), participantId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        showSuccessMessage("Участник подтвержден!")
                        loadParticipants() // Refresh the list
                    } else {
                        when (response.code()) {
                            400 -> showErrorMessage("Ошибка в данных запроса")
                            401 -> showErrorMessage("Ошибка авторизации")
                            403 -> showErrorMessage("Нет прав для подтверждения участника")
                            404 -> showErrorMessage("Участник или событие не найдены")
                            else -> showErrorMessage("Ошибка сервера: ${response.code()}")
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showErrorMessage("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun rejectParticipant(participantId: Int) {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showInfoMessage("Отклонение участника...")

        ApiClient.apiService.rejectParticipant("Bearer $token", eventId.toInt(), participantId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        showSuccessMessage("Участник отклонен!")
                        loadParticipants() // Refresh the list
                    } else {
                        when (response.code()) {
                            400 -> showErrorMessage("Ошибка в данных запроса")
                            401 -> showErrorMessage("Ошибка авторизации")
                            403 -> showErrorMessage("Нет прав для отклонения участника")
                            404 -> showErrorMessage("Участник или событие не найдены")
                            else -> showErrorMessage("Ошибка сервера: ${response.code()}")
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showErrorMessage("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun showInfoMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_color))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .setAction("OK") { }
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.success_color))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .setAction("OK") { }
            .show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.error_color))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .setAction("OK") { }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}