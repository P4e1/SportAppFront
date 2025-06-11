package com.example.sportsapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.sportsapp.R
import com.example.sportsapp.activities.LoginActivity
import com.example.sportsapp.activities.ChangePasswordActivity
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.FragmentProfileBinding
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.responses.OrganizerInfo
import com.example.sportsapp.responses.ParticipantInfo
import com.example.sportsapp.responses.ProfileResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        view.post{ loadProfile()}
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        requireActivity().window.statusBarColor = android.graphics.Color.TRANSPARENT
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding.swipeRefresh.setOnRefreshListener {
            loadProfile()
        }

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.primary_color)
        )
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfile() {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            showErrorMessage("Токен не найден. Пожалуйста, авторизуйтесь.")
            return
        }

        showLoading(true)

        ApiClient.apiService.getProfile("Bearer $token")
            .enqueue(object : Callback<List<ProfileResponse>> {
                override fun onResponse(
                    call: Call<List<ProfileResponse>>,
                    response: Response<List<ProfileResponse>>
                ) {
                    showLoading(false)
                    handleProfileResponse(response)
                }

                override fun onFailure(call: Call<List<ProfileResponse>>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    private fun handleProfileResponse(response: Response<List<ProfileResponse>>) {
        if (response.isSuccessful) {
            val profile = response.body()?.firstOrNull()
            if (profile != null) {
                bindProfile(profile)
                showContent()
            } else {
                showErrorMessage("Профиль не найден")
                showEmptyState()
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Ошибка авторизации"
                404 -> "Профиль не найден"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка загрузки профиля: ${response.code()}"
            }
            showErrorMessage(errorMessage)
            showEmptyState()
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
        showEmptyState()
    }

    private fun bindProfile(profile: ProfileResponse) {
        binding.apply {
            // Основная информация
            tvFullName.text = profile.fullname
            tvRole.text = profile.role
            tvUsername.text = profile.username
            tvEmail.text = profile.email
            tvPhone.text = profile.phone

            // Показываем соответствующую карточку в зависимости от роли
            if (profile.organizer != null) {
                setupOrganizerProfile(profile.organizer)
            } else if (profile.participant != null) {
                setupParticipantProfile(profile.participant)
            }
        }
    }

    private fun setupOrganizerProfile(organizer: OrganizerInfo)
    {
        binding.apply {
            cardOrganizer.visibility = View.VISIBLE
            cardParticipant.visibility = View.GONE

            tvOrgName.text = organizer.orgName
            tvOrgEmail.text = organizer.orgEmail
            tvOrgPhone.text = organizer.orgPhone
            tvOrgAddress.text = organizer.orgAddress
        }
    }

    private fun setupParticipantProfile(participant: ParticipantInfo) {
        binding.apply {
            cardParticipant.visibility = View.VISIBLE
            cardOrganizer.visibility = View.GONE

            tvInstitution.text = participant.institution
            tvDateR.text = participant.dateR
            tvHeight.text = "${participant.height} см"
            tvWeight.text = "${participant.weight} кг"
            tvRating.text = participant.rating.toString()
            tvCompetitions.text = participant.numberOfCompetitions.toString()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Выйти") { _, _ ->
                logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logout() {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            performLocalLogout()
            return
        }

        val body = mapOf("refresh" to refreshToken)

        ApiClient.apiService.logout(body)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    performLocalLogout()
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showErrorMessage("Ошибка выхода из аккаунта. Попробуйте снова.")
                }
            })
    }

    private fun performLocalLogout() {
        tokenManager.clearToken()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }


    private fun showLoading(show: Boolean) {
        if (show && !binding.swipeRefresh.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
            binding.contentLayout.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showContent() {
        binding.contentLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error_color))
            .setTextColor(requireContext().getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                loadProfile()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfile() // Обновляем профиль при возврате на фрагмент
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}