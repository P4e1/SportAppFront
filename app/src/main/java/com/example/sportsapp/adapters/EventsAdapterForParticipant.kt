package com.example.sportsapp

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.databinding.ItemEventForParticipantBinding
import com.example.sportsapp.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapterForParticipant(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapterForParticipant.EventViewHolder>() {

    private var events = mutableListOf<Event>()
    private var joinStatuses = mutableMapOf<Int, String>()

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        joinStatuses.clear()
        notifyDataSetChanged()
    }

    fun updateEventsWithJoinStatus(newEvents: List<Event>, statuses: Map<Int, String>) {
        events.clear()
        events.addAll(newEvents)
        joinStatuses.clear()
        joinStatuses.putAll(statuses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventForParticipantBinding.inflate(
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

    inner class EventViewHolder(private val binding: ItemEventForParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
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
                tvEventStatus.setBackgroundResource(getEventStatusBackground(event.status))

                // Set and display join status (ВАРИАНТ 3)
                val joinStatus = joinStatuses[event.id] ?: "Проверка"
                val formattedJoinStatus = getJoinStatusText(joinStatus)

                tvJoinStatus.text = formattedJoinStatus
                tvJoinStatus.setTextColor(getJoinStatusColor(formattedJoinStatus))
                tvJoinStatus.setBackgroundResource(getJoinStatusBackground(formattedJoinStatus))

                // Set location icon based on event type
                val locationIcon = when (event.typeOfCompetition.lowercase()) {
                    "индивидуальный" -> R.drawable.ic_person
                    "командный" -> R.drawable.ic_group
                    else -> R.drawable.ic_location
                }
                ivLocationIcon.setImageResource(locationIcon)

                // Set participants progress
                val progress = if (event.limitOfParticipants > 0) {
                    ((event.aLotOfParticipant.toFloat() / event.limitOfParticipants.toFloat()) * 100).toInt()
                } else {
                    0
                }
                progressParticipants.progress = progress

                // Set progress color based on percentage
                val progressColor = when {
                    progress >= 90 -> R.color.error_color
                    progress >= 70 -> R.color.warning_color
                    else -> R.color.success_color
                }
                progressParticipants.progressTintList =
                    ContextCompat.getColorStateList(itemView.context, progressColor)

                // Set click listener
                cardEvent.setOnClickListener {
                    onEventClick(event)
                }

                // Add item animation
                val slideInAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.slide_in_right)
                slideInAnimation.startOffset = (adapterPosition * 100).toLong()
                itemView.startAnimation(slideInAnimation)
            }
        }

        private fun getJoinStatusColor(status: String): Int {
            val context = itemView.context
            return when (status.lowercase()) {
                "подтверждён" -> ContextCompat.getColor(context, R.color.white)        // Белый текст на зеленом фоне
                "отклонён" -> ContextCompat.getColor(context, R.color.white)           // Белый текст на красном фоне
                "проверка" -> ContextCompat.getColor(context, R.color.primary_color)           // Белый текст на оранжевом фоне
                else -> ContextCompat.getColor(context, R.color.secondary_text)        // Серый текст на нейтральном фоне
            }
        }

        private fun getJoinStatusBackground(status: String): Int {
            return when (status.lowercase()) {
                "подтверждён" -> R.drawable.status_confirmed_background     // Зеленый фон
                "отклонён" -> R.drawable.status_rejected_background         // Красный фон
                "проверка" -> R.drawable.status_pending_background          // Оранжевый/Желтый фон
                else -> R.drawable.status_neutral_background                // Нейтральный серый фон
            }
        }

        private fun getJoinStatusText(status: String): String {
            return when (status.lowercase()) {
                "confirmed", "подтверждён", "approved" -> "Подтверждён"
                "rejected", "отклонён", "declined" -> "Отклонён"
                "pending", "проверка", "review" -> "Проверка"
                else -> if (status.isNotEmpty()) status else "Неизвестно"
            }
        }

        // Остальные методы остаются без изменений
        private fun formatDateTime(date: String, time: String): String {
            return try {
                val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
                val parsedDate = inputDateFormat.parse(date)
                val formattedDate = outputDateFormat.format(parsedDate!!)
                "$formattedDate в $time"
            } catch (e: Exception) {
                "$date в $time"
            }
        }

        private fun getEventStatusText(status: String): String {
            return when (status.lowercase()) {
                "open", "открыта", "регистрация открыта" -> "Регистрация открыта"
                "closed", "закрыта", "регистрация закрыта" -> "Регистрация закрыта"
                "ongoing", "идёт", "в процессе" -> "Идёт"
                "completed", "завершено", "завершён" -> "Завершено"
                else -> status
            }
        }

        private fun getEventStatusColor(status: String): Int {
            val context = itemView.context
            return when (status.lowercase()) {
                "open", "открыта", "регистрация открыта" -> ContextCompat.getColor(context, R.color.success_color)
                "closed", "закрыта", "регистрация закрыта" -> ContextCompat.getColor(context, R.color.warning_color)
                "ongoing", "идёт", "в процессе" -> ContextCompat.getColor(context, R.color.white)
                "completed", "завершено", "завершён" -> ContextCompat.getColor(context, R.color.secondary_text)
                else -> ContextCompat.getColor(context, R.color.secondary_text)
            }
        }

        private fun getEventStatusBackground(status: String): Int {
            return when (status.lowercase()) {
                "open", "открыта", "регистрация открыта" -> R.drawable.status_active_background
                "closed", "закрыта", "регистрация закрыта" -> R.drawable.status_pending_background
                "ongoing", "идёт", "в процессе" -> R.drawable.status_ongoing_background
                "completed", "завершено", "завершён" -> R.drawable.status_completed_background
                else -> R.drawable.status_pending_background
            }
        }
    }
}