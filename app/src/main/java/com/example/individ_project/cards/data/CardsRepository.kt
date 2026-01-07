package com.example.individ_project.cards.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardsRepository private constructor(context: Context) {

    private val db = CardsDatabase.getInstance(context)
    private val dao = db.cardDao()

    companion object {
        @Volatile
        private var INSTANCE: CardsRepository? = null

        fun getInstance(context: Context): CardsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CardsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val LEARNED_SET_NAME = "Изученные слова"

    // --- МЕТОДЫ РАБОТЫ С ДАННЫМИ ---

    suspend fun getAllSets(): List<CardSetEntity> =
        withContext(Dispatchers.IO) { dao.getAllSets() }

    suspend fun getCardsBySet(setId: Long): List<CardEntity> =
        withContext(Dispatchers.IO) { dao.getCardsBySet(setId) }

    // Создание пользовательского набора
    suspend fun createCustomSet(
        name: String,
        pairs: List<Pair<String, String>>
    ): Long = withContext(Dispatchers.IO) {
        val setId = dao.insertSet(
            CardSetEntity(
                name = name,
                levelTag = null,
                type = CardSetType.CUSTOM
            )
        )
        val cards = pairs.map { (front, back) ->
            CardEntity(setId = setId, frontText = front, backText = back)
        }
        dao.insertCards(cards)
        setId
    }

    // УДАЛЕНИЕ НАБОРА
    suspend fun deleteSet(setId: Long) {
        withContext(Dispatchers.IO) {
            // Удаляем карточки набора
            dao.deleteCardsBySetId(setId)
            // Удаляем сам набор
            dao.deleteSetById(setId)
        }
    }

    // --- ИНИЦИАЛИЗАЦИЯ И СПЕЦИАЛЬНЫЕ НАБОРЫ ---

    suspend fun ensurePredefinedSets() = withContext(Dispatchers.IO) {
        val existing = dao.getAllSets()
        val hasPredefined = existing.any { it.type == CardSetType.PREDEFINED }
        if (hasPredefined) return@withContext

        val sets = listOf(
            CardSetEntity(name = "Набор A (Beginner)", levelTag = "A", type = CardSetType.PREDEFINED),
            CardSetEntity(name = "Набор B (Intermediate)", levelTag = "B", type = CardSetType.PREDEFINED),
            CardSetEntity(name = "Набор C (Advanced)", levelTag = "C", type = CardSetType.PREDEFINED)
        )

        val ids = dao.insertSets(sets)
        val cards = mutableListOf<CardEntity>()

        // Набор A
        val aId = ids[0]
        cards += listOf(
            CardEntity(setId = aId, frontText = "apple", backText = "яблоко"),
            CardEntity(setId = aId, frontText = "bread", backText = "хлеб"),
            CardEntity(setId = aId, frontText = "water", backText = "вода"),
            CardEntity(setId = aId, frontText = "milk", backText = "молоко"),
            CardEntity(setId = aId, frontText = "egg", backText = "яйцо"),
            CardEntity(setId = aId, frontText = "fish", backText = "рыба"),
            CardEntity(setId = aId, frontText = "rice", backText = "рис"),
            CardEntity(setId = aId, frontText = "cake", backText = "торт"),
            CardEntity(setId = aId, frontText = "juice", backText = "сок"),
            CardEntity(setId = aId, frontText = "eat", backText = "есть"),
            CardEntity(setId = aId, frontText = "drink", backText = "пить"),
            CardEntity(setId = aId, frontText = "mother", backText = "мама"),
            CardEntity(setId = aId, frontText = "father", backText = "папа"),
            CardEntity(setId = aId, frontText = "house", backText = "дом"),
            CardEntity(setId = aId, frontText = "dog", backText = "собака"),
            CardEntity(setId = aId, frontText = "cat", backText = "кошка"),
            CardEntity(setId = aId, frontText = "red", backText = "красный"),
            CardEntity(setId = aId, frontText = "blue", backText = "синий"),
            CardEntity(setId = aId, frontText = "big", backText = "большой"),
            CardEntity(setId = aId, frontText = "small", backText = "маленький")
        )

        // Набор B
        val bId = ids[1]
        cards += listOf(
            CardEntity(setId = bId, frontText = "journey", backText = "путешествие"),
            CardEntity(setId = bId, frontText = "improve", backText = "улучшать"),
            CardEntity(setId = bId, frontText = "meeting", backText = "встреча"),
            CardEntity(setId = bId, frontText = "decision", backText = "решение"),
            CardEntity(setId = bId, frontText = "problem", backText = "проблема"),
            CardEntity(setId = bId, frontText = "solution", backText = "решение"),
            CardEntity(setId = bId, frontText = "office", backText = "офис"),
            CardEntity(setId = bId, frontText = "computer", backText = "компьютер"),
            CardEntity(setId = bId, frontText = "phone", backText = "телефон"),
            CardEntity(setId = bId, frontText = "work", backText = "работа"),
            CardEntity(setId = bId, frontText = "study", backText = "учёба"),
            CardEntity(setId = bId, frontText = "friendship", backText = "дружба"),
            CardEntity(setId = bId, frontText = "holiday", backText = "праздник"),
            CardEntity(setId = bId, frontText = "weather", backText = "погода"),
            CardEntity(setId = bId, frontText = "rain", backText = "дождь"),
            CardEntity(setId = bId, frontText = "sunny", backText = "солнечный"),
            CardEntity(setId = bId, frontText = "time", backText = "время"),
            CardEntity(setId = bId, frontText = "morning", backText = "утро"),
            CardEntity(setId = bId, frontText = "evening", backText = "вечер"),
            CardEntity(setId = bId, frontText = "help", backText = "помощь")
        )

        // Набор C
        val cId = ids[2]
        cards += listOf(
            CardEntity(setId = cId, frontText = "sophisticated", backText = "сложный, утончённый"),
            CardEntity(setId = cId, frontText = "comprehensive", backText = "всесторонний"),
            CardEntity(setId = cId, frontText = "perspective", backText = "перспектива"),
            CardEntity(setId = cId, frontText = "implement", backText = "осуществлять"),
            CardEntity(setId = cId, frontText = "strategy", backText = "стратегия"),
            CardEntity(setId = cId, frontText = "innovation", backText = "инновация"),
            CardEntity(setId = cId, frontText = "sustainable", backText = "устойчивый"),
            CardEntity(setId = cId, frontText = "collaborate", backText = "сотрудничать"),
            CardEntity(setId = cId, frontText = "challenge", backText = "вызов"),
            CardEntity(setId = cId, frontText = "opportunity", backText = "возможность"),
            CardEntity(setId = cId, frontText = "analyze", backText = "анализировать"),
            CardEntity(setId = cId, frontText = "evaluate", backText = "оценивать"),
            CardEntity(setId = cId, frontText = "significant", backText = "значительный"),
            CardEntity(setId = cId, frontText = "complex", backText = "сложный"),
            CardEntity(setId = cId, frontText = "diverse", backText = "разнообразный"),
            CardEntity(setId = cId, frontText = "efficient", backText = "эффективный"),
            CardEntity(setId = cId, frontText = "priority", backText = "приоритет"),
            CardEntity(setId = cId, frontText = "objective", backText = "цель"),
            CardEntity(setId = cId, frontText = "achievement", backText = "достижение"),
            CardEntity(setId = cId, frontText = "commitment", backText = "обязательство")
        )

        dao.insertCards(cards)
    }

    private suspend fun ensureLearnedSetId(): Long = withContext(Dispatchers.IO) {
        val all = dao.getAllSets()
        val existing = all.firstOrNull { it.type == CardSetType.LEARNED }
        if (existing != null) return@withContext existing.id

        val set = CardSetEntity(
            name = LEARNED_SET_NAME,
            levelTag = null,
            type = CardSetType.LEARNED
        )
        dao.insertSet(set)
    }

    suspend fun addLearnedWordsToCards(words: List<String>) = withContext(Dispatchers.IO) {
        if (words.isEmpty()) return@withContext
        val setId = ensureLearnedSetId()

        val existing = dao.getCardsBySet(setId)
        val existingFronts = existing.map { it.frontText }.toSet()

        val newCards = words
            .filter { it.isNotBlank() && it !in existingFronts }
            .map { w -> CardEntity(setId = setId, frontText = w, backText = "") }

        if (newCards.isNotEmpty()) {
            dao.insertCards(newCards)
        }
    }

    suspend fun addLearnedPairsToCards(pairs: List<Pair<String, String>>) =
        withContext(Dispatchers.IO) {
            if (pairs.isEmpty()) return@withContext
            val setId = ensureLearnedSetId()

            val existing = dao.getCardsBySet(setId)
            val existingPairs = existing
                .map { it.frontText.lowercase() to it.backText.lowercase() }
                .toSet()

            val newCards = pairs
                .filter { (front, back) ->
                    val key = front.lowercase() to back.lowercase()
                    front.isNotBlank() && back.isNotBlank() && key !in existingPairs
                }
                .map { (front, back) ->
                    CardEntity(setId = setId, frontText = front, backText = back)
                }

            if (newCards.isNotEmpty()) {
                dao.insertCards(newCards)
            }
        }
}
