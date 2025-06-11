package com.example.sportsapp.activities

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.sportsapp.fragments.EventCreateFragment
import com.example.sportsapp.EventsFragment
import com.example.sportsapp.MyEventsFragment
import com.example.sportsapp.NotificationsFragment
import com.example.sportsapp.R
import com.example.sportsapp.managers.RoleManager
import com.example.sportsapp.databinding.ActivityMainBinding
import com.example.sportsapp.fragments.*
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragment: Fragment? = null
    private var isFirstLoad = true
    private lateinit var roleManager: RoleManager
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Инициализируем роль пользователя
        roleManager = RoleManager(this)
        userRole = roleManager.getRole() ?: "Участник" // Роль по умолчанию

        setupUI()
        setupBottomNavigationForRole()

        // Устанавливаем начальный фрагмент с анимацией
        if (savedInstanceState == null) {
            when (userRole) {
                "Организатор" -> {
                    loadFragment(MyEventsFragment(), isInitialLoad = true)
                    binding.bottomNavigation.selectedItemId = R.id.nav_my_events
                }
                "Участник" -> {
                    loadFragment(HomeFragment(), isInitialLoad = true)
                    binding.bottomNavigation.selectedItemId = R.id.nav_home
                }
                else -> {
                    loadFragment(ProfileFragment(), isInitialLoad = true)
                    binding.bottomNavigation.selectedItemId = R.id.nav_profile
                }
            }
        }

    }

    private fun setupUI() {
        // Анимация появления всего экрана
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        // Настройка статус бара
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        // Анимация появления bottom navigation с задержкой
        binding.bottomNavigation.alpha = 0f
        binding.bottomNavigation.postDelayed({
            val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.bottomNavigation.startAnimation(slideUpAnimation)
            binding.bottomNavigation.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }, 200)
    }

    private fun setupBottomNavigationForRole() {
        // Загружаем соответствующее меню для роли
        loadMenuForRole()

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            // Добавляем тактильную обратную связь
            binding.bottomNavigation.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            val fragment = getFragmentForMenuItem(menuItem.itemId)

            fragment?.let {
                animateNavigationIcon(menuItem.itemId)
                loadFragment(it)
            }
            true
        }

        // Анимация при повторном нажатии
        binding.bottomNavigation.setOnItemReselectedListener { menuItem ->
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.fragmentContainer.startAnimation(pulseAnimation)
            showNavigationHint(getNavigationItemName(menuItem.itemId))
        }
    }

    private fun loadMenuForRole() {
        binding.bottomNavigation.menu.clear()

        when (userRole) {
            "Участник" -> {
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_navigation_participant)
            }
            "Организатор" -> {
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_navigation_organizer)
            }
            else -> {
                binding.bottomNavigation.inflateMenu(R.menu.menu_bottom_navigation_default)
            }
        }
    }



    private fun getFragmentForMenuItem(itemId: Int): Fragment? {
        return when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_search -> SearchEventsFragment()
            R.id.nav_events -> EventsFragment()
            R.id.nav_notifications -> NotificationsFragment()
            R.id.nav_profile -> ProfileFragment()
            R.id.nav_create_event -> EventCreateFragment()
            R.id.nav_my_events -> MyEventsFragment()
            else -> null
        }
    }

    private fun loadFragment(fragment: Fragment, isInitialLoad: Boolean = false) {
        try {
            val transaction = supportFragmentManager.beginTransaction()

            if (!isInitialLoad && currentFragment != null) {
                // Добавляем анимации перехода между фрагментами
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // enter
                    R.anim.slide_out_left,  // exit
                    R.anim.slide_in_left,   // popEnter
                    R.anim.slide_out_right  // popExit
                )
            } else if (isInitialLoad) {
                // Для первоначальной загрузки используем fade in
                transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            }

            transaction.replace(R.id.fragment_container, fragment)
            transaction.commit()

            currentFragment = fragment

        } catch (e: Exception) {
            // Обработка ошибок
            showErrorMessage("Ошибка загрузки экрана. Попробуйте еще раз.")
        }
    }

    private fun animateNavigationIcon(itemId: Int) {
        // Анимация выбранного элемента навигации
        binding.bottomNavigation.findViewById<View>(itemId)?.let { view ->
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_bounce)
            view.startAnimation(scaleAnimation)
        }
    }

    private fun getNavigationItemName(itemId: Int): String {
        return when (userRole) {
            "Участник" -> {
                when (itemId) {
                    R.id.nav_home -> "Главная"
                    R.id.nav_search -> "Поиск"
                    R.id.nav_events -> "События"
                    R.id.nav_notifications -> "Уведомления"
                    R.id.nav_profile -> "Профиль"
                    else -> "Экран"
                }
            }
            "Организатор" -> {
                when (itemId) {
                    R.id.nav_my_events -> "Мои события"
                    R.id.nav_create_event -> "Новое событие"
                    R.id.nav_notifications -> "Уведомления"
                    R.id.nav_profile -> "Профиль"
                    else -> "Экран"
                }
            }
            else -> {
                when (itemId) {
                    R.id.nav_home -> "Главная"
                    R.id.nav_profile -> "Профиль"
                    else -> "Экран"
                }
            }
        }
    }

    private fun showNavigationHint(screenName: String) {
        Snackbar.make(binding.root, "Вы уже находитесь на экране: $screenName", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.primary_color))
            .setTextColor(getColor(R.color.white))
            .setAnchorView(binding.bottomNavigation)
            .show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_color))
            .setTextColor(getColor(R.color.white))
            .setAction("ПОВТОРИТЬ") {
                // Перезагружаем текущий фрагмент
                currentFragment?.let { loadFragment(it) }
            }
            .setAnchorView(binding.bottomNavigation)
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getColor(R.color.success_color))
            .setTextColor(getColor(R.color.white))
            .setAnchorView(binding.bottomNavigation)
            .show()
    }

    override fun onBackPressed() {
        // Обработка кнопки назад с анимацией
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            // Если это последний экран, показываем подтверждение выхода
            showExitConfirmation()
        }
    }

    private fun showExitConfirmation() {
        Snackbar.make(binding.root, "Нажмите еще раз для выхода из приложения", Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.primary_color))
            .setTextColor(getColor(R.color.white))
            .setAction("ВЫЙТИ") {
                finish()
                overridePendingTransition(R.anim.fade_out, R.anim.fade_in)
            }
            .setAnchorView(binding.bottomNavigation)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Проверяем, не изменилась ли роль пользователя
        val currentRole = roleManager.getRole() ?: "Участник"
        if (currentRole != userRole) {
            userRole = currentRole
            setupBottomNavigationForRole() // Перенастраиваем навигацию

            // Если текущий фрагмент недоступен для новой роли, переходим на главную
            if (!isCurrentFragmentAvailableForRole()) {
                loadFragment(HomeFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            }
        }

        if (!isFirstLoad) {
            // Subtle анимация возвращения
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.fragmentContainer.startAnimation(pulseAnimation)
        }
        isFirstLoad = false
    }

    override fun onPause() {
        super.onPause()
        // Сохраняем состояние перед переходом
    }

    // Публичные методы для использования фрагментами
    fun showMessage(message: String, isError: Boolean = false) {
        if (isError) {
            showErrorMessage(message)
        } else {
            showSuccessMessage(message)
        }
    }

    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
    }



    private fun isCurrentFragmentAvailableForRole(): Boolean {
        val selectedItemId = binding.bottomNavigation.selectedItemId
        // Проверяем, есть ли выбранный элемент в текущем меню
        return binding.bottomNavigation.menu.findItem(selectedItemId) != null
    }
}