package com.example.individ_project.ui.home

sealed class HomeItem {
    data class ActHeader(val actIndex: Int) : HomeItem()
    data class LevelItem(val level: Level) : HomeItem()
}
