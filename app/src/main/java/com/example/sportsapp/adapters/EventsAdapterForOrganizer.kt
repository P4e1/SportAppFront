package com.example.sportsapp

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.databinding.ItemEventForOrganizerBinding
import com.example.sportsapp.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapterForOrganizer(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapterForOrganizer.EventViewHolder>() {

    private var events = mutableListOf<Event>()

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventForOrganizerBinding.inflate(
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

    inner class EventViewHolder(private val binding: ItemEventForOrganizerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            with(binding) {
                tvEventName.text = event.name
                tvEventAddress.text = event.address
                tvEventParticipants.text = "${event.aLotOfParticipant}/${event.limitOfParticipants}"

                val dateTimeText = formatDateTime(event.startDate, event.startTime)
                tvEventDateTime.text = dateTimeText

                tvEventStatus.text = getEventStatusText(event.status)
                tvEventStatus.setTextColor(getEventStatusColor(event.status))
                tvEventStatus.setBackgroundResource(getEventStatusBackground(event.status))

                val locationIcon = when (event.typeOfCompetition.lowercase()) {
                    "индивидуальный" -> R.drawable.ic_person
                    "командный" -> R.drawable.ic_group
                    else -> R.drawable.ic_location
                }
                ivLocationIcon.setImageResource(locationIcon)

                val progress = if (event.limitOfParticipants > 0) {
                    ((event.aLotOfParticipant.toFloat() / event.limitOfParticipants.toFloat()) * 100).toInt()
                } else 0
                progressParticipants.progress = progress

                val progressColor = when {
                    progress >= 90 -> R.color.error_color
                    progress >= 70 -> R.color.warning_color
                    else -> R.color.success_color
                }
                progressParticipants.progressTintList =
                    ContextCompat.getColorStateList(itemView.context, progressColor)

                cardEvent.setOnClickListener { onEventClick(event) }

                val slideInAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.slide_in_right)
                slideInAnimation.startOffset = (adapterPosition * 100).toLong()
                itemView.startAnimation(slideInAnimation)
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