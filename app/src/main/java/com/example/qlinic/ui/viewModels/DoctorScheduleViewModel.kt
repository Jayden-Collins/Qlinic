package com.example.qlinic.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.data.model.SpecificDoctorInfo
import com.example.qlinic.data.repository.DoctorSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DoctorScheduleViewModel(
    private val doctorRepository: DoctorSchedule
): ViewModel()
{

    // state flow to hold list of doctors
    private val _doctors = MutableStateFlow<List<SpecificDoctorInfo>>(emptyList())
    val doctors: StateFlow<List<SpecificDoctorInfo>> = _doctors

    // state flow to hold loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var allDoctors = emptyList<SpecificDoctorInfo>()

    init{
        viewModelScope.launch{
            loadDoctors()
        }
    }

    // load doctors from repository
    fun loadDoctors(){
        viewModelScope.launch {
            _isLoading.value = true // set loading to true while fetching data
            try{
                val fetchDoctors = doctorRepository.fetchDoctors()
                allDoctors = fetchDoctors
                _doctors.value = fetchDoctors
            } catch (e: Exception){
                // handle error fetching doctors
                println("Error loading doctors: ${e.message}")
            } finally {
                _isLoading.value = false // set loading to false after fetching data
            }
        }
    }

}