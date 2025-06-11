package com.example.sportsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.FragmentNotificationsBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.AppNotification
import com.example.sportsapp.ui.NotificationDetailDialog
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notifications = mutableListOf<AppNotification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        requireActivity().window.statusBarColor = android.graphics.Color.TRANSPARENT
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Setup swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }
        binding.swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.primary_color)
        )
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(notifications) { notification ->
            // Handle notification click - show full text dialog
            showNotificationDialog(notification)
        }

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationsAdapter
        }
    }

    private fun loadNotifications() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        showLoading(true)

        ApiClient.apiService.getAppNotifications("Bearer $token")
            .enqueue(object : Callback<List<AppNotification>> {
                override fun onResponse(
                    call: Call<List<AppNotification>>,
                    response: Response<List<AppNotification>>
                ) {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    handleNotificationsResponse(response)
                }

                override fun onFailure(
                    call: Call<List<AppNotification>>,
                    t: Throwable
                ) {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    handleNetworkError(t)
                }
            })
    }

    private fun handleNotificationsResponse(response: Response<List<AppNotification>>) {
        if (response.isSuccessful) {
            val notificationList = response.body()
            if (notificationList != null) {
                notifications.clear()
                notifications.addAll(notificationList)

                if (notifications.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    notificationsAdapter.notifyDataSetChanged()
                }
            } else {
                showErrorMessage("Ошибка при получении уведомлений")
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Ошибка авторизации"
                403 -> "Нет доступа к уведомлениям"
                404 -> "Уведомления не найдены"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка загрузки уведомлений: ${response.code()}"
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

    private fun showNotificationDialog(notification: AppNotification) {
        val dialog = NotificationDetailDialog.newInstance(notification)
        dialog.show(parentFragmentManager, "NotificationDetailDialog")
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.recyclerViewNotifications.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerViewNotifications.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show && notifications.isEmpty()) {
            binding.recyclerViewNotifications.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error_color))
            .setTextColor(requireContext().getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                loadNotifications()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}