package com.example.individ_project.cards.data

import androidx.room.TypeConverter

class CardsTypeConverters {

    @TypeConverter
    fun fromType(type: CardSetType): String = type.name

    @TypeConverter
    fun toType(value: String): CardSetType = CardSetType.valueOf(value)
}
