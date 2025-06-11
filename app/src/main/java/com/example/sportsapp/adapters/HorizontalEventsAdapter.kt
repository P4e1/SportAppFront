package com.example.sportsapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.R
import com.example.sportsapp.databinding.ItemEventHorizontalBinding
import com.example.sportsapp.models.Event
import java.text.SimpleDateFormat
import java.util.*

class HorizontalEventsAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<HorizontalEventsAdapter.EventViewHolder>() {

    private var events = mutableListOf<Event>()
    private var joinStatuses = mutableMapOf<Int, String>()

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        joinStatuses.clear()
        notifyDataSetChanged()
        Log.d("HorizontalEventsAdapter", "Events updated: ${events.size} items")
    }

    fun updateEventsWithJoinStatus(newEvents: List<Event>, statuses: Map<Int, String>) {
        events.clear()
        events.addAll(newEvents)
        joinStatuses.clear()
        joinStatuses.putAll(statuses)
        notifyDataSetChanged()
        Log.d("HorizontalEventsAdapter", "Events with join status updated: ${events.size} items")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(private val binding: ItemEventHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            try {
                with(binding) {
                    // Set event data
                    tvEventName.text = event.name
                    tvEventAddress.text = event.address
                    tvEventParticipants.text = "${event.aLotOfParticipant}/${event.limitOfParticipants}"

                    // Format and set date/time
                    val dateTimeText = formatDateTime(event.startDate, event.startTime)
                    tvEventDateTime.text = dateTimeText

                    // Set event status with appropriate color
                    tvEventStatus.text = getEventStatusText(event.status)
                    tvEventStatus.setTextColor(getEventStatusColor(event.status))
                    setEventStatusBackground(tvEventStatus, event.status)

                    // Set and display join status
//                    val joinStatus = joinStatuses[event.id] ?: ""
//                    if (joinStatus.isNotEmpty()) {
//                        val formattedJoinStatus = getJoinStatusText(joinStatus)
//                        tvJoinStatus.text = formattedJoinStatus
//                        tvJoinStatus.setTextColor(getJoinStatusColor(formattedJoinStatus))
//                        setJoinStatusBackground(tvJoinStatus, formattedJoinStatus)
//                        tvJoinStatus.visibility = View.VISIBLE
//                    } else {
//                        tvJoinStatus.visibility = View.GONE
//                    }

                    // Set location icon based on event type
                    setLocationIcon(event.typeOfCompetition)

                    // Set participants progress
                    val progress = if (event.limitOfParticipants > 0) {
                        ((event.aLotOfParticipant.toFloat() / event.limitOfParticipants.toFloat()) * 100).toInt()
                    } else {
                        0
                    }
                    progressParticipants.progress = progress

                    // Set progress color based on percentage
                    setProgressColor(progress)

                    // Set click listener
                    cardEvent.setOnClickListener {
                        onEventClick(event)
                    }

                }
            } catch (e: Exception) {
                Log.e("HorizontalEventsAdapter", "Error binding event: ${e.message}", e)
            }
        }

        private fun setLocationIcon(typeOfCompetition: String) {
            try {
                val locationIcon = when (typeOfCompetition.lowercase(Locale.getDefault())) {
                    "индивидуальный" -> R.drawable.ic_person
                    "командный" -> R.drawable.ic_group
                    else -> R.drawable.ic_location
                }
                binding.ivLocationIcon.setImageResource(locationIcon)
            } catch (e: Exception) {
                Log.w("HorizontalEventsAdapter", "Could not set location icon, using fallback: ${e.message}")
                // Fallback к системной иконке
                binding.ivLocationIcon.setImageResource(android.R.drawable.ic_dialog_map)
            }
        }

        private fun setProgressColor(progress: Int) {
            try {
                val progressColorRes = when {
                    progress >= 90 -> R.color.background_gradient_end
                    progress >= 70 -> R.color.background_gradient_center
                    else -> R.color.background_gradient_start
                }

                // Fallback если кастомные цвета не найдены
                val fallbackColor = when {
                    progress >= 90 -> android.R.color.holo_red_light
                    progress >= 70 -> android.R.color.holo_orange_light
                    else -> android.R.color.holo_green_light
                }

                try {
                    binding.progressParticipants.progressTintList =
                        ContextCompat.getColorStateList(itemView.context, progressColorRes)
                } catch (e: Exception) {
                    // Если кастомные цвета не найдены, используем стандартные
                    binding.progressParticipants.progressTintList =
                        ContextCompat.getColorStateList(itemView.context, fallbackColor)
                }
            } catch (e: Exception) {
                Log.w("HorizontalEventsAdapter", "Could not set progress color: ${e.message}")
            }
        }

        private fun setJoinStatusBackground(textView: android.widget.TextView, status: String) {
            try {
                val backgroundRes = when (status.lowercase(Locale.getDefault())) {
                    "подтверждён" -> R.drawable.status_confirmed_background
                    "отклонён" -> R.drawable.status_rejected_background
                    "проверка" -> R.drawable.status_pending_background
                    else -> R.drawable.status_neutral_background
                }
                textView.setBackgroundResource(backgroundRes)
            } catch (e: Exception) {
                Log.w("HorizontalEventsAdapter", "Could not set join status background: ${e.message}")
                // Fallback к системному фону
                textView.setBackgroundResource(android.R.drawable.editbox_background_normal)
            }
        }

        private fun setEventStatusBackground(textView: android.widget.TextView, status: String) {
            try {
                val backgroundRes = when (status.lowercase(Locale.getDefault())) {
                    "open", "открыта", "регистрация открыта" -> R.drawable.status_active_background
                    "closed", "закрыта", "регистрация закрыта" -> R.drawable.status_rejected_background
                    "ongoing", "идёт", "в процессе" -> R.drawable.status_ongoing_background
                    "completed", "завершено", "завершён" -> R.drawable.status_completed_background
                    else -> R.drawable.status_neutral_background
                }
                textView.setBackgroundResource(backgroundRes)
            } catch (e: Exception) {
                Log.w("HorizontalEventsAdapter", "Could not set event status background: ${e.message}")
                // Fallback к системному фону
                textView.setBackgroundResource(android.R.drawable.editbox_background_normal)
            }
        }

        private fun getJoinStatusColor(status: String): Int {
            val context = itemView.context
            return try {
                when (status.lowercase(Locale.getDefault())) {
                    "подтверждён" -> ContextCompat.getColor(context, R.color.success_color_light)
                    "отклонён" -> ContextCompat.getColor(context, R.color.error_color_light)
                    "проверка" -> ContextCompat.getColor(context, R.color.gray_light)
                    else -> ContextCompat.getColor(context, R.color.secondary_text)
                }
            } catch (e: Exception) {
                // Fallback к системным цветам
                when (status.lowercase(Locale.getDefault())) {
                    "подтверждён" -> ContextCompat.getColor(context, android.R.color.white)
                    "отклонён" -> ContextCompat.getColor(context, android.R.color.white)
                    "проверка" -> ContextCompat.getColor(context, android.R.color.black)
                    else -> ContextCompat.getColor(context, android.R.color.darker_gray)
                }
            }
        }

        private fun getJoinStatusText(status: String): String {
            return when (status.lowercase(Locale.getDefault())) {
                "confirmed", "подтверждён", "approved" -> "Подтверждён"
                "rejected", "отклонён", "declined" -> "Отклонён"
                "pending", "проверка", "review" -> "Проверка"
                else -> if (status.isNotEmpty()) status else "Неизвестно"
            }
        }

        private fun formatDateTime(date: String, time: String): String {
            return try {
                val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("dd MMM", Locale("ru"))
                val parsedDate = inputDateFormat.parse(date)
                val formattedDate = outputDateFormat.format(parsedDate!!)
                "$formattedDate в $time"
            } catch (e: Exception) {
                Log.w("HorizontalEventsAdapter", "Date formatting error: ${e.message}")
                "$date в $time"
            }
        }

        private fun getEventStatusText(status: String): String {
            return when (status.lowercase(Locale.getDefault())) {
                "open", "открыта", "регистрация открыта" -> "Регистрация открыта"
                "closed", "закрыта", "регистрация закрыта" -> "Регистрация закрыта"
                "ongoing", "идёт", "в процессе" -> "Идёт"
                "completed", "завершено", "завершён" -> "Завершено"
                else -> status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
        }

        private fun getEventStatusColor(status: String): Int {
            val context = itemView.context
            return try {
                when (status.lowercase(Locale.getDefault())) {
                    "open", "открыта", "регистрация открыта" -> ContextCompat.getColor(context, R.color.success_color)
                    "closed", "закрыта", "регистрация закрыта" -> ContextCompat.getColor(context, R.color.error_color)
                    "ongoing", "идёт", "в процессе" -> ContextCompat.getColor(context, R.color.warning_color)
                    "completed", "завершено", "завершён" -> ContextCompat.getColor(context, R.color.secondary_text)
                    else -> ContextCompat.getColor(context, R.color.primary_text)
                }
            } catch (e: Exception) {
                // Fallback к белому цвету для всех статусов
                ContextCompat.getColor(context, android.R.color.white)
            }
        }
    }
}