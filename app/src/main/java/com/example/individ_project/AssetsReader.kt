package com.example.individ_project

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object AssetsReader {
    fun loadLevels(context: Context): List<LevelData> {
        val levelsList = mutableListOf<LevelData>()
        try {
            // ВОТ ЗДЕСЬ БЫЛА ОШИБКА. Меняем на data.json
            val inputStream = context.assets.open("data.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val levelObj = jsonArray.getJSONObject(i)
                val id = levelObj.getInt("id")
                val title = levelObj.optString("title", "Level $id")
                val description = levelObj.optString("description", "")

                val questionsArray = levelObj.getJSONArray("questions")
                val questions = mutableListOf<QuestionData>()

                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)

                    val type = qObj.optString("type", "word")
                    val text = qObj.getString("text")

                    val optArray = qObj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (k in 0 until optArray.length()) {
                        options.add(optArray.getString(k))
                    }

                    val correctIdx = qObj.optInt("correctIndex", -1)

                    val correctAns = if (qObj.has("correctAnswer") && !qObj.isNull("correctAnswer")) {
                        qObj.getString("correctAnswer")
                    } else {
                        null
                    }

                    questions.add(QuestionData(
                        type = type,
                        text = text,
                        options = options,
                        correctIndex = correctIdx,
                        correctAnswer = correctAns
                    ))
                }
                levelsList.add(LevelData(id, title, description, questions))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return levelsList
    }
}
