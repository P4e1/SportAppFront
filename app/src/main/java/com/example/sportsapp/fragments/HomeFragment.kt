package com.example.sportsapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.R
import com.example.sportsapp.adapters.HorizontalEventsAdapter
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.FragmentHomeBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.Event
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventsAdapter: HorizontalEventsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var tokenManager: TokenManager

    private var currentPosition = 0
    private var allEvents: List<Event> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupNavigationButtons()
        setupRefresh()
        loadEventsFromServer()
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        eventsAdapter = HorizontalEventsAdapter { event ->
            // Обработка клика по событию
            // TODO: Добавить навигацию к детальной странице события
            // findNavController().navigate(R.id.action_home_to_event_detail, bundleOf("eventId" to event.id))
        }

        binding.rvEvents.apply {
            adapter = eventsAdapter
            layoutManager = this@HomeFragment.layoutManager

            // Добавляем отступы между элементами
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.right = 16 // Отступ справа в dp
                }
            })

            // Слушатель скролла для обновления состояния кнопок
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateNavigationButtons()
                }
            })
        }
    }

    private fun setupNavigationButtons() {
        binding.btnScrollLeft.setOnClickListener {
            scrollToPrevious()
        }

        binding.btnScrollRight.setOnClickListener {
            scrollToNext()
        }

        // Изначально левая кнопка неактивна
        updateNavigationButtons()
    }

    private fun setupRefresh() {
        // Если у вас есть SwipeRefreshLayout в макете
        // binding.swipeRefresh?.setOnRefreshListener {
        //     loadEventsFromServer()
        // }
    }

    private fun scrollToPrevious() {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePosition > 0) {
            binding.rvEvents.smoothScrollToPosition(firstVisiblePosition - 1)
        }
    }

    private fun scrollToNext() {
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val itemCount = eventsAdapter.itemCount
        if (lastVisiblePosition < itemCount - 1) {
            binding.rvEvents.smoothScrollToPosition(lastVisiblePosition + 1)
        }
    }

    private fun updateNavigationButtons() {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val itemCount = eventsAdapter.itemCount

        // Левая кнопка активна, если не в начале списка
        binding.btnScrollLeft.isEnabled = firstVisiblePosition > 0
        binding.btnScrollLeft.alpha = if (firstVisiblePosition > 0) 1.0f else 0.5f

        // Правая кнопка активна, если не в конце списка
        binding.btnScrollRight.isEnabled = lastVisiblePosition < itemCount - 1
        binding.btnScrollRight.alpha = if (lastVisiblePosition < itemCount - 1) 1.0f else 0.5f
    }

    private fun loadEventsFromServer() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            loadSampleEvents() // Fallback к примерам данных
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
                // В случае ошибки загружаем примеры данных
                loadSampleEvents()
            }
        })
    }

    private fun handleEventsResponse(response: Response<List<Event>>) {
        if (response.isSuccessful) {
            val events = response.body() ?: emptyList()
            allEvents = events

            if (events.isNotEmpty()) {
                // Получаем статусы участия для событий
                loadJoinStatuses(events)
            } else {
                // Если событий нет, показываем сообщение
                showEmptyState()
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Ошибка авторизации"
                404 -> "События не найдены"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка загрузки событий: ${response.code()}"
            }
            showErrorMessage(errorMessage)
            loadSampleEvents() // Fallback к примерам данных
        }
    }

    private fun loadJoinStatuses(events: List<Event>) {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            // Если нет токена, показываем события без статусов участия
            eventsAdapter.updateEvents(events)
            updateNavigationButtons()
            return
        }

        // Здесь можно добавить загрузку статусов участия для каждого события
        // Пока используем заглушки или показываем события без статусов
        val mockJoinStatuses = events.associate { event ->
            event.id to when ((event.id % 4)) {
                0 -> "подтверждён"
                1 -> "проверка"
                2 -> "отклонён"
                else -> "проверка"
            }
        }

        eventsAdapter.updateEventsWithJoinStatus(events, mockJoinStatuses)

        // Обновляем состояние кнопок после загрузки данных
        binding.rvEvents.post {
            updateNavigationButtons()
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

    private fun showLoading(show: Boolean) {
        // Если у вас есть прогресс-бар в макете
        // binding.progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        // binding.rvEvents.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        // Если у вас есть layout для пустого состояния
        //binding.emptyStateLayout?.visibility = View.VISIBLE
        //binding.rvEvents.visibility = View.GONE
        showErrorMessage("Нет доступных событий")
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error_color))
            .setTextColor(requireContext().getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                loadEventsFromServer()
            }
            .show()
    }

    private fun loadSampleEvents() {
        // Fallback данные на случай проблем с сервером
        val sampleEvents = listOf(
            Event(
                id = 1,
                name = "Турнир по футболу",
                address = "Спортивный комплекс «Олимп»",
                startDate = "2024-03-15",
                startTime = "14:00",
                status = "открыта",
                typeOfCompetition = "командный",
                aLotOfParticipant = 12,
                limitOfParticipants = 20,
                description = "Городской турнир по футболу",
                endDate = "2024-03-15",
                endTime = "18:00",
                categories = emptyList()
            ),
            Event(
                id = 2,
                name = "Забег на 5км",
                address = "Парк Победы",
                startDate = "2024-03-20",
                startTime = "09:00",
                status = "открыта",
                typeOfCompetition = "индивидуальный",
                aLotOfParticipant = 45,
                limitOfParticipants = 50,
                description = "Утренний забег",
                endDate = "2024-03-20",
                endTime = "11:00",
                categories = emptyList()
            ),
            Event(
                id = 3,
                name = "Баскетбольный матч",
                address = "Школа №15",
                startDate = "2024-03-22",
                startTime = "16:00",
                status = "закрыта",
                typeOfCompetition = "командный",
                aLotOfParticipant = 10,
                limitOfParticipants = 10,
                description = "Школьный турнир",
                endDate = "2024-03-22",
                endTime = "19:00",
                categories = emptyList()
            ),
            Event(
                id = 4,
                name = "Теннисный турнир",
                address = "Теннисный клуб «Ace»",
                startDate = "2024-03-25",
                startTime = "10:00",
                status = "открыта",
                typeOfCompetition = "индивидуальный",
                aLotOfParticipant = 8,
                limitOfParticipants = 16,
                description = "Любительский турнир",
                endDate = "2024-03-25",
                endTime = "18:00",
                categories = emptyList()
            ),
            Event(
                id = 5,
                name = "Волейбольная лига",
                address = "Спортзал \"Энергия\"",
                startDate = "2024-03-28",
                startTime = "18:00",
                status = "идёт",
                typeOfCompetition = "командный",
                aLotOfParticipant = 24,
                limitOfParticipants = 24,
                description = "Лига выходного дня",
                endDate = "2024-03-28",
                endTime = "22:00",
                categories = emptyList()
            )
        )

        // Пример статусов участия
        val joinStatuses = mapOf(
            1 to "подтверждён",
            2 to "проверка",
            3 to "отклонён",
            4 to "проверка",
            5 to "подтверждён"
        )

        eventsAdapter.updateEventsWithJoinStatus(sampleEvents, joinStatuses)

        // Обновляем состояние кнопок после загрузки данных
        binding.rvEvents.post {
            updateNavigationButtons()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем события при возврате на фрагмент
        loadEventsFromServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}