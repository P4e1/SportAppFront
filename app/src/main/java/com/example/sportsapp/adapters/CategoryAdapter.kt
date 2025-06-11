package com.example.sportsapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sportsapp.databinding.ItemCategoryBinding
import com.example.sportsapp.models.ParticipantCategory

class CategoryAdapter(
    private val categories: MutableList<ParticipantCategory>,
    private val onRemoveCategory: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        with(holder.binding) {
            tvCategoryType.text = category.type

            when (category.type) {
                "Возрастная" -> {
                    etCategoryName.hint = "от ${category.minValue ?: 0} лет до ${category.maxValue ?: 100} лет"
                    etCategoryName.setText(category.name)
                }
                "Весовая" -> {
                    etCategoryName.hint = "от ${category.minValue ?: 0} кг до ${category.maxValue ?: 200} кг"
                    etCategoryName.setText(category.name)
                }
                else -> {
                    etCategoryName.hint = "Название категории"
                    etCategoryName.setText(category.name)
                }
            }

            btnRemoveCategory.setOnClickListener {
                onRemoveCategory(position)
            }
        }
    }

    override fun getItemCount(): Int = categories.size
}