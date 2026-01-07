package com.example.individ_project.cards.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_sets")
data class CardSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val levelTag: String?,              // "A", "B", "C" или null
    val type: CardSetType = CardSetType.CUSTOM
)
