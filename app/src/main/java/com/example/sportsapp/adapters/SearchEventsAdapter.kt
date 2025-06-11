package com.example.sportsapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.R
import com.example.sportsapp.databinding.ItemSearchEventBinding
import com.example.sportsapp.models.Event
import java.text.SimpleDateFormat
import java.util.*

class SearchEventsAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<SearchEventsAdapter.EventViewHolder>() {

    private var events: List<Event> = emptyList()

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding: ItemSearchEventBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_search_event,
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(
        private val binding: ItemSearchEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.textEventName.text = event.name
            binding.textEventAddress.text = event.address

            // Формат участников
            val participantsText = "${event.aLotOfParticipant}/${event.limitOfParticipants} участников"
            binding.textEventParticipants.text = participantsText

            // Формат даты и времени
            try {
                val dateTimeText = "${event.startDate} в ${event.startTime}"
                binding.textEventDateTime.text = dateTimeText
            } catch (e: Exception) {
                binding.textEventDateTime.text = "${event.startDate} ${event.startTime}"
            }

            // Установка статуса и цвета
            setupEventStatus(event.status)

            // Установка прогресса участников
            setupParticipantsProgress(event.aLotOfParticipant, event.limitOfParticipants)

            // Клик по элементу
            binding.root.setOnClickListener {
                onEventClick(event)
            }

            // Клик по кнопке "Подробнее"
            binding.btnViewDetails.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun setupEventStatus(status: String) {
            when (status.lowercase()) {
                "active", "активный" -> {
                    binding.textEventStatus.text = "АКТИВНОЕ"
                    binding.textEventStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.success_color)
                    )
                    binding.statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.success_color)
                    )
                }
                "completed", "завершенный" -> {
                    binding.textEventStatus.text = "ЗАВЕРШЕНО"
                    binding.textEventStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.secondary_text)
                    )
                    binding.statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.light_gray)
                    )
                }
                "cancelled", "отмененный" -> {
                    binding.textEventStatus.text = "ОТМЕНЕНО"
                    binding.textEventStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.error_color)
                    )
                    binding.statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.error_color)
                    )
                }
                "pending", "ожидающий" -> {
                    binding.textEventStatus.text = "ОЖИДАЕТ"
                    binding.textEventStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.warning_color)
                    )
                    binding.statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.warning_color)
                    )
                }
                else -> {
                    binding.textEventStatus.text = status.uppercase()
                    binding.textEventStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.primary_color)
                    )
                    binding.statusIndicator.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.primary_color)
                    )
                }
            }
        }

        private fun setupParticipantsProgress(current: Int, limit: Int) {
            val progress = if (limit > 0) {
                ((current.toFloat() / limit.toFloat()) * 100).toInt()
            } else {
                0
            }

            binding.progressParticipants.progress = progress

            // Изменение цвета прогресс-бара в зависимости от заполненности
            when {
                progress >= 90 -> {
                    binding.progressParticipants.progressTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.error_color)
                }
                progress >= 70 -> {
                    binding.progressParticipants.progressTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.warning_color)
                }
                else -> {
                    binding.progressParticipants.progressTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.success_color)
                }
            }
        }
    }
}