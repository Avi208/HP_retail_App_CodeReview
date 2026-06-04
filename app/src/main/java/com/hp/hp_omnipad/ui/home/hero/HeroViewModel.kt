package com.hp.hp_omnipad.ui.home.hero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hp.hp_omnipad.data.repository.HeroRepository
import com.hp.hp_omnipad.ui.home.model.Hero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeroViewModel : ViewModel() {

    private val _heroes = MutableStateFlow<List<Hero>>(emptyList())
    val heroes: StateFlow<List<Hero>> = _heroes

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    init {
        // Observe heroes from repository to get updates automatically when sync happens
        viewModelScope.launch {
            HeroRepository.heroesFlow.collect { list ->
                if (list.isNotEmpty()) {
                    _heroes.value = list
                }
            }
        }
        
        // Initial load
        loadHeroes()
    }

    fun loadHeroes() {
        viewModelScope.launch {
            try {
                val data = HeroRepository.getHeroes()
                if (data.isNotEmpty()) {
                    _heroes.value = data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /*
    Move to next hero video
     */
    fun playNext() {

        val list = _heroes.value

        if (list.isEmpty()) return

        val next = (_currentIndex.value + 1) % list.size

        _currentIndex.value = next
    }

    /*
    User clicks a hero video card
     */
    fun playHeroAt(index: Int) {

        val list = _heroes.value

        if (index < 0 || index >= list.size) return

        _currentIndex.value = index
    }

    /*
    Find index of hero
     */
    fun getHeroIndex(hero: Hero): Int {

        return _heroes.value.indexOfFirst {
            it.videoUrl == hero.videoUrl
        }
    }

    /*
    Return up next videos
     */
    fun getNextHeroes(currentIndex: Int): List<Hero> {

        val list = _heroes.value

        if (list.size <= 1) return emptyList()

        val after = list.drop(currentIndex + 1)
        val before = list.take(currentIndex)

        return after + before
    }
}
