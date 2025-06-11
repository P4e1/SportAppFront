package com.example.sportsapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportsapp.activities.EventProfileActivity
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityMyEventsFragmentBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.Event
import com.example.sportsapp.models.ParticipantCategory
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyEventsFragment : Fragment() {

    private var _binding: ActivityMyEventsFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private lateinit var eventsAdapter: EventsAdapterForOrganizer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.activity_my_events_fragment, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()

        // Безопасный вызов после полной инициализации view + binding
        view.post {
            loadEvents()
        }
    }


    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        requireActivity().window.statusBarColor = android.graphics.Color.TRANSPARENT
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding.swipeRefresh.setOnRefreshListener {
            loadEvents()
        }

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.primary_color)
        )
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapterForOrganizer { event ->
            openEventProfile(event)
        }

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter

            // Add item animations
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    private fun loadEvents() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        ApiClient.apiService.getMyEvents("Bearer $token").enqueue(object : Callback<List<Event>> {
            override fun onResponse(call: Call<List<Event>>, response: Response<List<Event>>) {
                showLoading(false)
                handleEventsResponse(response)
            }

            override fun onFailure(call: Call<List<Event>>, t: Throwable) {
                showLoading(false)
                handleNetworkError(t)
            }
        })
    }

    private fun handleEventsResponse(response: Response<List<Event>>) {
        showLoading(false)
        if (response.isSuccessful) {
            val events = response.body() ?: emptyList()
            eventsAdapter.updateEvents(events)

            if (events.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Ошибка авторизации"
                404 -> "События не найдены"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка загрузки событий: ${response.code()}"
            }
            showErrorMessage(errorMessage)
        }
    }

    private fun handleNetworkError(t: Throwable) {
        val errorMessage = when {
            t.message?.contains("timeout", ignoreCase = true) == true ->
                "Превышено время ожидания. Проверьте подключение к интернету"
            t.message?.contains("network", ignoreCase = true) == true ->
                "Проблемы с сетью. Проверьте подключение к интернету"
            else -> "Ошибка подключения к серверу"
        }
        showErrorMessage(errorMessage)
    }
    fun formatCategories(categories: List<ParticipantCategory>): String {
        return categories.joinToString(", ") { cat ->
            when (cat.type) {
                "Возрастная", "Весовая" -> "${cat.name}: ${cat.minValue}-${cat.maxValue}"
                else -> "${cat.type}: ${cat.name}"
            }
        }
    }
    private fun openEventProfile(event: Event) {
        val intent = Intent(requireContext(), EventProfileActivity::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_name", event.name)
            putExtra("event_description", event.description)
            putExtra("event_type", event.typeOfCompetition)
            putExtra("event_category", formatCategories(event.categories))
            putExtra("event_start_date", event.startDate)
            putExtra("event_start_time", event.startTime)
            putExtra("event_end_date", event.endDate)
            putExtra("event_end_time", event.endTime)
            putExtra("event_address", event.address)
            putExtra("event_current_participants", event.aLotOfParticipant)
            putExtra("event_limit_participants", event.limitOfParticipants)
            putExtra("event_status", event.status)
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun showLoading(show: Boolean) {
        if (show && !binding.swipeRefresh.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
            binding.recyclerViewEvents.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            binding.recyclerViewEvents.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewEvents.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewEvents.visibility = View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error_color))
            .setTextColor(requireContext().getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                loadEvents()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadEvents() // Refresh events when returning to fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}