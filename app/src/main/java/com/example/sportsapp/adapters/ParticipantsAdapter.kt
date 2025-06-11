package com.example.sportsapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.databinding.ItemParticipantBinding
import com.example.sportsapp.models.Participant

class ParticipantsAdapter(
    private val participants: List<Participant>,
    private val onParticipantClick: (Participant) -> Unit
) : RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder>() {

    inner class ParticipantViewHolder(private val binding: ItemParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: Participant) {
            binding.apply {
                tvParticipantName.text = participant.fullname
                tvParticipantTeam.text = participant.team
                tvParticipantInfo.text = "Рост: ${participant.height} см, Вес: ${participant.weight} кг"

                tvParticipantStatus.text = "Статус: ${participant.teamParticipantStatus}"
                // Format birth date
                tvParticipantBirthDate.text = "Дата рождения: ${participant.dateR}"

                // Set click listener
                root.setOnClickListener {
                    onParticipantClick(participant)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount(): Int = participants.size
}