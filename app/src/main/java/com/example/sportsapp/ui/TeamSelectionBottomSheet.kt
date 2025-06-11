// TeamSelectionBottomSheet.kt
package com.example.sportsapp.ui.bottomsheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.sportsapp.R
import com.example.sportsapp.databinding.ActivityTeamSelectionBottomSheetBinding

class TeamSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ActivityTeamSelectionBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var listener: TeamSelectionListener? = null
    private var eventId: Int = -1

    interface TeamSelectionListener {
        fun onJoinTeam(eventId: Int, teamName: String)
        fun onCreateTeam(eventId: Int, teamName: String)
    }

    companion object {
        fun newInstance(eventId: Int): TeamSelectionBottomSheet {
            val fragment = TeamSelectionBottomSheet()
            val args = Bundle().apply {
                putInt("event_id", eventId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = arguments?.getInt("event_id", -1) ?: -1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTeamSelectionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Настройка спиннера для выбора действия
        val actionOptions = arrayOf("Присоединиться к команде", "Создать команду")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAction.adapter = adapter

        // Изначально скрываем поле ввода и кнопку
        binding.layoutTeamName.visibility = View.GONE
        binding.btnConfirm.visibility = View.GONE

        // Слушатель для спиннера
        binding.spinnerAction.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                showTeamNameInput(position)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun showTeamNameInput(actionPosition: Int) {
        binding.layoutTeamName.visibility = View.VISIBLE
        binding.btnConfirm.visibility = View.VISIBLE

        when (actionPosition) {
            0 -> { // Присоединиться к команде
                binding.textInputLayout.hint = "Название команды"
                binding.textTeamNameHint.text = "Укажите точное название существующей команды"
                binding.btnConfirm.text = "Присоединиться"
            }
            1 -> { // Создать команду
                binding.textInputLayout.hint = "Название новой команды"
                binding.textTeamNameHint.text = "Придумайте уникальное название для вашей команды"
                binding.btnConfirm.text = "Создать команду"
            }
        }

        // Анимация появления
        binding.layoutTeamName.alpha = 0f
        binding.btnConfirm.alpha = 0f
        binding.layoutTeamName.animate().alpha(1f).setDuration(300).start()
        binding.btnConfirm.animate().alpha(1f).setDuration(300).setStartDelay(150).start()
    }

    private fun setupClickListeners() {
        binding.btnConfirm.setOnClickListener {
            val teamName = binding.editTextTeamName.text.toString().trim()

            if (teamName.isEmpty()) {
                binding.textInputLayout.error = "Введите название команды"
                return@setOnClickListener
            }

            if (teamName.length < 3) {
                binding.textInputLayout.error = "Название команды должно содержать минимум 3 символа"
                return@setOnClickListener
            }

            binding.textInputLayout.error = null

            val selectedAction = binding.spinnerAction.selectedItemPosition
            when (selectedAction) {
                0 -> listener?.onJoinTeam(eventId, teamName)
                1 -> listener?.onCreateTeam(eventId, teamName)
            }

            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setTeamSelectionListener(listener: TeamSelectionListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}