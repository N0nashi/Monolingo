package com.example.individ_project.cards.data

import androidx.room.*

@Dao
interface CardDao {

    // --- НАБОРЫ (Sets) ---

    @Query("SELECT * FROM card_sets")
    suspend fun getAllSets(): List<CardSetEntity>

    @Insert
    suspend fun insertSet(set: CardSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<CardSetEntity>): List<Long>

    // Удаление набора
    @Query("DELETE FROM card_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: Long)

    // --- КАРТОЧКИ (Cards) ---

    @Query("SELECT * FROM cards WHERE setId = :setId")
    suspend fun getCardsBySet(setId: Long): List<CardEntity>

    @Insert
    suspend fun insertCards(cards: List<CardEntity>)

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    // Удаление карточек конкретного набора
    @Query("DELETE FROM cards WHERE setId = :setId")
    suspend fun deleteCardsBySetId(setId: Long)
}
