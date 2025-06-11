package com.example.sportsapp.ui

import com.example.sportsapp.models.AppNotification
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.sportsapp.R
import com.example.sportsapp.databinding.DialogNotificationDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationDetailDialog : DialogFragment() {

    private var _binding: DialogNotificationDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var notification: AppNotification

    companion object {
        private const val ARG_NOTIFICATION = "notification"

        fun newInstance(notification: AppNotification): NotificationDetailDialog {
            val fragment = NotificationDetailDialog()
            val args = Bundle().apply {
                putSerializable(ARG_NOTIFICATION, notification)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notification = arguments?.getSerializable(ARG_NOTIFICATION) as AppNotification
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_notification_detail,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        populateData()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupDialog() {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun populateData() {
        binding.apply {
            textTitle.text = notification.title
            textMessage.text = notification.message
            textTimestamp.text = formatTimestamp(notification.timestamp)

            // Set notification type icon and color
            when (notification.type) {
                "success" -> {
                    iconType.setImageResource(R.drawable.ic_check_circle)
                    iconType.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success_color))
                }
                "warning" -> {
                    iconType.setImageResource(R.drawable.ic_warning)
                    iconType.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning_color))
                }
                "error" -> {
                    iconType.setImageResource(R.drawable.ic_error)
                    iconType.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_color))
                }
                else -> {
                    iconType.setImageResource(R.drawable.ic_info)
                    iconType.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_color))
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnMarkAsRead.setOnClickListener {
            // TODO: Implement mark as read functionality
            // You can add API call here to mark notification as read
            dismiss()
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            date?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}