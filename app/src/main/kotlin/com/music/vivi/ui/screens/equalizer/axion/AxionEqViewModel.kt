package com.music.vivi.ui.screens.equalizer.axion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.eq.EqualizerService
import com.music.vivi.eq.data.EQProfileRepository
import com.music.vivi.eq.data.FilterType
import com.music.vivi.eq.data.ParametricEQBand
import com.music.vivi.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AxionEqViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerService: EqualizerService,
    private val eqProfileRepository: EQProfileRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("vivi_eq_prefs", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled = _enabled.asStateFlow()

    private val bandFrequencies = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    
    private val _bandGains = MutableStateFlow(
        FloatArray(10) { prefs.getFloat("band_$it", 0f) }
    )
    val bandGains = _bandGains.asStateFlow()

    private val _mode = MutableStateFlow(prefs.getInt("mode", 0)) // 0: Simple, 1: Advanced
    val mode = _mode.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty = _isDirty.asStateFlow()

    val customProfiles = eqProfileRepository.profiles.map { profiles ->
        profiles.filter { it.isCustom && it.id != "vivi_tuning" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        if (_enabled.value) {
            applyToService()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        prefs.edit().putBoolean("enabled", enabled).apply()
        if (enabled) {
            applyToService()
        } else {
            viewModelScope.launch {
                eqProfileRepository.setActiveProfile(null)
            }
            equalizerService.disable()
        }
    }

    fun setMode(mode: Int) {
        _mode.value = mode
        prefs.edit().putInt("mode", mode).apply()
        _isDirty.value = false // Reset dirty when switching modes
    }

    fun setBandGain(index: Int, gain: Float, applyOnly: Boolean = false) {
        val newGains = _bandGains.value.copyOf()
        newGains[index] = gain
        _bandGains.value = newGains
        _isDirty.value = true
        
        if (!applyOnly) {
            prefs.edit().putFloat("band_$index", gain).apply()
        }
        
        if (_enabled.value) {
            applyToService(saveToStorage = !applyOnly)
        }
    }

    fun commitCurrentGains() {
        val editor = prefs.edit()
        _bandGains.value.forEachIndexed { index, f -> editor.putFloat("band_$index", f) }
        editor.apply()
        if (_enabled.value) {
            applyToService(saveToStorage = true)
        }
    }

    fun setBandsGains(gains: FloatArray, fromUser: Boolean = false, applyOnly: Boolean = false) {
        _bandGains.value = gains
        if (!applyOnly) {
            val editor = prefs.edit()
            gains.forEachIndexed { index, f -> editor.putFloat("band_$index", f) }
            editor.apply()
        }
        _isDirty.value = fromUser // Only dirty if coming from manual tuner moves
        if (_enabled.value) {
            applyToService(saveToStorage = !applyOnly)
        }
    }

    fun reset() {
        val flat = FloatArray(10) { 0f }
        setBandsGains(flat)
    }

    fun saveCustomProfile(name: String) {
        viewModelScope.launch {
            val bandVals = _bandGains.value
            val maxBoostDb = bandVals.maxOrNull()?.toDouble() ?: 0.0
            val calculatedPreamp = if (maxBoostDb > 0.0) -maxBoostDb / 50.0 else 0.0

            val bands = bandVals.mapIndexed { index, f ->
                ParametricEQBand(
                    frequency = bandFrequencies[index],
                    gain = f.toDouble() / 50.0,
                    q = 1.41,
                    filterType = FilterType.PK,
                    enabled = true
                )
            }
            
            val id = "custom_${System.currentTimeMillis()}"
            val profile = SavedEQProfile(
                id = id,
                name = name,
                deviceModel = "ViviEqualizer",
                bands = bands,
                preamp = calculatedPreamp,
                isCustom = true,
                isActive = true
            )
            
            eqProfileRepository.saveProfile(profile)
            eqProfileRepository.setActiveProfile(profile.id)
            _isDirty.value = false
        }
    }

    fun deleteProfiles(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                eqProfileRepository.deleteProfile(id)
            }
        }
    }

    private fun applyToService(saveToStorage: Boolean = true) {
        viewModelScope.launch {
            val bandVals = _bandGains.value
            val maxBoostDb = bandVals.maxOrNull()?.toDouble() ?: 0.0
            val calculatedPreamp = if (maxBoostDb > 0.0) -maxBoostDb / 50.0 else 0.0

            val bands = bandVals.mapIndexed { index, f ->
                ParametricEQBand(
                    frequency = bandFrequencies[index],
                    gain = f.toDouble() / 50.0, // 600 / 50 = 12dB
                    q = 1.41,
                    filterType = FilterType.PK,
                    enabled = true
                )
            }
            
            val profile = SavedEQProfile(
                id = "vivi_tuning",
                name = "Vivi Tuning",
                deviceModel = "ViviEqualizer",
                bands = bands,
                preamp = calculatedPreamp,
                isCustom = false,
                isActive = true
            )
            
            if (saveToStorage) {
                // Save to global repository and set as active to ensure persistence across restarts
                eqProfileRepository.saveProfile(profile)
                eqProfileRepository.setActiveProfile(profile.id)
            }
            
            equalizerService.applyProfile(profile)
        }
    }
}
