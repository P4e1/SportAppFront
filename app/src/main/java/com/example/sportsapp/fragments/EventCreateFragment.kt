package com.example.sportsapp.fragments

import android.util.Log
import com.google.gson.Gson
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportsapp.api.ApiClient
import com.example.sportsapp.databinding.ActivityEventCreateFragmentBinding
import com.example.sportsapp.databinding.ItemCategoryBinding
import com.example.sportsapp.requests.EventCreateRequest
import com.example.sportsapp.responses.EventCreateResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.R
import com.example.sportsapp.adapters.CategoryAdapter
import com.example.sportsapp.managers.TokenManager
import com.example.sportsapp.models.ParticipantCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Модель для категории участников

class EventCreateFragment : Fragment() {

    private var _binding: ActivityEventCreateFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val calendar = Calendar.getInstance()

    // Список категорий и адаптер
    private val participantCategories = mutableListOf<ParticipantCategory>()
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategoryType: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.activity_event_create_fragment, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
        setupCategoryRecyclerView()
    }

    private fun setupUI() {
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeInAnimation)

        requireActivity().window.statusBarColor = android.graphics.Color.TRANSPARENT
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun setupSpinners() {
        // Type of Competition Spinner
        val competitionTypes = arrayOf("Индивидуальный", "Командный")
        val competitionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, competitionTypes)
        binding.etTypeOfCompetition.setAdapter(competitionAdapter)
        setupSpinner(binding.etTypeOfCompetition)

        // Category Type Spinner
        val categoryTypes = arrayOf("Возрастная", "Весовая", "Мой вариант")
        val categoryTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryTypes)
        binding.etCategoryType.setAdapter(categoryTypeAdapter)
        setupSpinner(binding.etCategoryType)

        // Обработчик выбора типа категории
        binding.etCategoryType.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryType = categoryTypes[position]
            showCategoriesSection()
            binding.etCategoryType.clearFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etCategoryType.windowToken, 0)
        }
    }

    private fun setupSpinner(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.threshold = 1
        autoCompleteTextView.setOnClickListener {
            if (!autoCompleteTextView.isPopupShowing) {
                autoCompleteTextView.showDropDown()
            }
        }
        autoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            autoCompleteTextView.clearFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
        }
    }

    private fun setupClickListeners() {
        // Date and Time Pickers
        binding.etStartDate.setOnClickListener { showDatePicker { date -> binding.etStartDate.setText(date) } }
        binding.etStartTime.setOnClickListener { showTimePicker { time -> binding.etStartTime.setText(time) } }
        binding.etEndDate.setOnClickListener { showDatePicker { date -> binding.etEndDate.setText(date) } }
        binding.etEndTime.setOnClickListener { showTimePicker { time -> binding.etEndTime.setText(time) } }

        // Add Category Button
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // Create Event Button
        binding.btnCreateEvent.setOnClickListener {
            createEvent()
        }
    }

    private fun setupTextWatchers() {
        // Валидация полей при вводе текста
        binding.etName.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etDescription.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etAddress.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etStartDate.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etStartTime.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etEndDate.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etEndTime.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etLimitOfParticipants.doOnTextChanged { _, _, _, _ -> validateForm() }
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter(participantCategories) { position ->
            removeCategory(position)
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun showCategoriesSection() {
        binding.llCategoriesSection.visibility = View.VISIBLE
        // Анимация появления секции категорий
        val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
        binding.llCategoriesSection.startAnimation(slideDownAnimation)
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etMinValue = dialogView.findViewById<EditText>(R.id.etMinValue)
        val etMaxValue = dialogView.findViewById<EditText>(R.id.etMaxValue)

        // Настройка полей в зависимости от типа категории
        when (selectedCategoryType) {
            "Возрастная" -> {
                etMinValue.hint = "Минимальный возраст"
                etMaxValue.hint = "Максимальный возраст"
                etCategoryName.hint = "Название возрастной категории"
            }
            "Весовая" -> {
                etMinValue.hint = "Минимальный вес (кг)"
                etMaxValue.hint = "Максимальный вес (кг)"
                etCategoryName.hint = "Название весовой категории"
            }
            "Мой вариант" -> {
                etMinValue.visibility = View.GONE
                etMaxValue.visibility = View.GONE
                etCategoryName.hint = "Название категории"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить категорию: $selectedCategoryType")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = etCategoryName.text.toString().trim()
                val minValue = if (selectedCategoryType != "Мой вариант") {
                    etMinValue.text.toString().toIntOrNull()
                } else null
                val maxValue = if (selectedCategoryType != "Мой вариант") {
                    etMaxValue.text.toString().toIntOrNull()
                } else null

                if (name.isNotEmpty()) {
                    addCategory(name, minValue, maxValue)
                } else {
                    showSnackbar("Введите название категории")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addCategory(name: String, minValue: Int?, maxValue: Int?) {
        val category = ParticipantCategory(
            type = selectedCategoryType,
            name = name,
            minValue = minValue,
            maxValue = maxValue
        )

        participantCategories.add(category)
        categoryAdapter.notifyItemInserted(participantCategories.size - 1)

        // Прокрутка к новому элементу
        binding.rvCategories.smoothScrollToPosition(participantCategories.size - 1)
    }

    private fun removeCategory(position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить категорию")
            .setMessage("Вы уверены, что хотите удалить эту категорию?")
            .setPositiveButton("Удалить") { _, _ ->
                participantCategories.removeAt(position)
                categoryAdapter.notifyItemRemoved(position)
                categoryAdapter.notifyItemRangeChanged(position, participantCategories.size)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                onDateSelected(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Устанавливаем минимальную дату как сегодня
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                onTimeSelected(timeFormat.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun validateForm(): Boolean {
        val isValid = binding.etName.text.toString().trim().isNotEmpty() &&
                binding.etDescription.text.toString().trim().isNotEmpty() &&
                binding.etTypeOfCompetition.text.toString().isNotEmpty() &&
                binding.etStartDate.text.toString().isNotEmpty() &&
                binding.etStartTime.text.toString().isNotEmpty() &&
                binding.etEndDate.text.toString().isNotEmpty() &&
                binding.etEndTime.text.toString().isNotEmpty() &&
                binding.etAddress.text.toString().trim().isNotEmpty() &&
                binding.etLimitOfParticipants.text.toString().isNotEmpty() &&
                participantCategories.isNotEmpty()

        binding.btnCreateEvent.isEnabled = isValid
        return isValid
    }

    private fun createEvent() {
        if (!validateForm()) {
            showSnackbar("Заполните все обязательные поля")
            return
        }

        val request = EventCreateRequest(
            name = binding.etName.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            typeOfCompetition = binding.etTypeOfCompetition.text.toString(),
            categories = participantCategories,
            startDate = formatDateToIso(binding.etStartDate.text.toString()),
            startTime = binding.etStartTime.text.toString(),
            endDate = formatDateToIso(binding.etEndDate.text.toString()),
            endTime = binding.etEndTime.text.toString(),
            address = binding.etAddress.text.toString().trim(),
            limitOfParticipants = binding.etLimitOfParticipants.text.toString().toInt()
        )
        val json = Gson().toJson(request)
        Log.d("CREATE_EVENT_JSON", json)

        // Показываем индикатор загрузки
        binding.btnCreateEvent.isEnabled = false
        binding.btnCreateEvent.text = "Создание..."

        val token = tokenManager.getToken()
        if (token != null) {
            ApiClient.apiService.createEvent("Bearer $token", request)
                .enqueue(object : Callback<EventCreateResponse> {
                    override fun onResponse(
                        call: Call<EventCreateResponse>,
                        response: Response<EventCreateResponse>
                    ) {
                        binding.btnCreateEvent.isEnabled = true
                        binding.btnCreateEvent.text = "СОЗДАТЬ СОБЫТИЕ"

                        if (response.isSuccessful) {
                            showSnackbar("Событие успешно создано!")
                            clearForm()
                            // Можно добавить навигацию к списку событий
                        } else {
                            showSnackbar("Ошибка при создании события: ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<EventCreateResponse>, t: Throwable) {
                        binding.btnCreateEvent.isEnabled = true
                        binding.btnCreateEvent.text = "СОЗДАТЬ СОБЫТИЕ"
                        showSnackbar("Ошибка сети: ${t.message}")
                    }
                })
        } else {
            binding.btnCreateEvent.isEnabled = true
            binding.btnCreateEvent.text = "СОЗДАТЬ СОБЫТИЕ"
            showSnackbar("Ошибка авторизации")
        }
    }

    private fun formatDateToIso(input: String): String {
        // Преобразует "10.06.2025" → "2025-06-10"
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(input)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            input // если что-то пошло не так, вернёт как есть
        }
    }


    private fun clearForm() {
        binding.etName.text?.clear()
        binding.etDescription.text?.clear()
        binding.etTypeOfCompetition.text?.clear()
        binding.etCategoryType.text?.clear()
        binding.etStartDate.text?.clear()
        binding.etStartTime.text?.clear()
        binding.etEndDate.text?.clear()
        binding.etEndTime.text?.clear()
        binding.etAddress.text?.clear()
        binding.etLimitOfParticipants.text?.clear()

        participantCategories.clear()
        categoryAdapter.notifyDataSetChanged()
        binding.llCategoriesSection.visibility = View.GONE
        selectedCategoryType = ""
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}