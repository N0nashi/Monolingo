package com.example.individ_project.cards.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CardSetEntity::class, CardEntity::class],
    version = 2, // <-- ИЗМЕНЕНИЕ 1: Версия повышена
    exportSchema = false
)
@TypeConverters(CardsTypeConverters::class)
abstract class CardsDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: CardsDatabase? = null

        fun getInstance(context: Context): CardsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardsDatabase::class.java,
                    "cards_db"
                )
                    .fallbackToDestructiveMigration() // <-- ИЗМЕНЕНИЕ 2: Разрешить пересоздание базы
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
