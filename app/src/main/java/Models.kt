package com.example.individ_project

// Главная "коробка", в которой лежат уровни (на случай если JSON начинается с объекта, а не массива)
data class AppData(
    val levels: List<LevelData>
)

// Описание одного уровня (заголовок, описание, список вопросов)
data class LevelData(
    val id: Int,
    val title: String,
    val description: String,
    val questions: List<QuestionData>
)

// Описание одного вопроса (текст, варианты, правильный индекс или правильная фраза)
data class QuestionData(
    val type: String,            // Тип вопроса: "word", "sentence", "fill"
    val text: String,            // Текст вопроса
    val options: List<String>,   // Варианты ответов (или слова для составления)
    val correctIndex: Int,       // Индекс правильного ответа (для теста)
    val correctAnswer: String? = null  // Правильная фраза (для составления предложений). Может быть null.
)
