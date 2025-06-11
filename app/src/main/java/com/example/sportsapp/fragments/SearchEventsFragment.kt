package com.example.sportsapp.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportsapp.R
import com.example.sportsapp.activities.EventProfileForParticipantActivity
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.FragmentSearchEventsBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.Event
import com.example.sportsapp.adapters.SearchEventsAdapter
import com.example.sportsapp.models.ParticipantCategory
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchEventsFragment : Fragment() {

    private var _binding: FragmentSearchEventsBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private lateinit var eventsAdapter: SearchEventsAdapter

    private var allEvents: List<Event> = emptyList()
    private var filteredEvents: List<Event> = emptyList()
    private var currentSearchQuery = ""
    private var currentStatusFilter = "Все статусы"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_events, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupFilters()
        setupSearch()
        loadEvents()
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

        // Filter toggle
        binding.btnToggleFilters.setOnClickListener {
            toggleFilters()
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = SearchEventsAdapter { event ->
            openEventProfile(event)
        }

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter

            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }

    private fun setupFilters() {
        // Category filter

        // Status filter
        val statuses = arrayOf("Все статусы", "Запланировано", "Регистрация открыта", "Регистрация закрыта", "Идёт")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        // Filter listeners

        binding.spinnerStatus.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStatusFilter = statuses[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Clear filters button
        binding.btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupSearch() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString().trim()
                applyFilters()
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.editTextSearch.text?.clear()
        }
    }

    private fun toggleFilters() {
        if (binding.filtersLayout.visibility == View.VISIBLE) {
            binding.filtersLayout.visibility = View.GONE
            binding.btnToggleFilters.text = "ПОКАЗАТЬ ФИЛЬТРЫ"
        } else {
            binding.filtersLayout.visibility = View.VISIBLE
            binding.btnToggleFilters.text = "СКРЫТЬ ФИЛЬТРЫ"
        }
    }

    private fun applyFilters() {
        filteredEvents = allEvents.filter { event ->
            val matchesSearch = if (currentSearchQuery.isEmpty()) {
                true
            } else {
                event.name.contains(currentSearchQuery, ignoreCase = true) ||
                        event.description.contains(currentSearchQuery, ignoreCase = true) ||
                        event.address.contains(currentSearchQuery, ignoreCase = true)
            }

            val matchesStatus = if (currentStatusFilter == "Все статусы") {
                true
            } else {
                event.status.equals(currentStatusFilter, ignoreCase = true)
            }

            matchesSearch && matchesStatus
        }

        eventsAdapter.updateEvents(filteredEvents)
        updateResultsText()

        if (filteredEvents.isEmpty() && allEvents.isNotEmpty()) {
            showEmptyFilterState()
        } else if (filteredEvents.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun clearFilters() {
        binding.editTextSearch.text?.clear()
        binding.spinnerCategory.setSelection(0)
        binding.spinnerStatus.setSelection(0)
        currentSearchQuery = ""
        currentStatusFilter = "Все статусы"
        applyFilters()
    }

    private fun updateResultsText() {
        val resultsText = if (filteredEvents.size == allEvents.size) {
            "Найдено ${filteredEvents.size} событий"
        } else {
            "Показано ${filteredEvents.size} из ${allEvents.size} событий"
        }
        binding.textViewResults.text = resultsText
    }

    private fun loadEvents() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        ApiClient.apiService.getAllEvents("Bearer $token").enqueue(object : Callback<List<Event>> {
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
            allEvents = response.body() ?: emptyList()
            applyFilters()
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
                "Возрастная", "Весовая" -> "${cat.type}: ${cat.minValue}-${cat.maxValue}"
                else -> "${cat.type}: ${cat.name}"
            }
        }
    }

    private fun openEventProfile(event: Event) {
        val intent = Intent(requireContext(), EventProfileForParticipantActivity::class.java).apply {
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
            putExtra("is_search_mode", true) // Указываем, что пришли из поиска
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
        binding.emptyStateText.text = "События не найдены"
        binding.emptyStateSubtext.text = "Попробуйте изменить параметры поиска или создать новое событие"
    }

    private fun showEmptyFilterState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewEvents.visibility = View.GONE
        binding.emptyStateText.text = "Нет результатов"
        binding.emptyStateSubtext.text = "Попробуйте изменить фильтры или поисковый запрос"
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
        loadEvents() // Обновляем события при возврате на фрагмент
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}