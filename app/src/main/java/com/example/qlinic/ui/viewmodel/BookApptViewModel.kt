package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Slot
import com.example.qlinic.data.repository.AppointmentRepository
import com.example.qlinic.data.repository.SlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class BookApptViewModel : ViewModel() {

    private val slotRepository = SlotRepository()
    private val appointmentRepository = AppointmentRepository()

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots = _availableSlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedSlot = MutableStateFlow<Slot?>(null)
    val selectedSlot = _selectedSlot.asStateFlow()

    fun getDoctorSlots(doctorId: String, date: Date) {
        viewModelScope.launch {
            // 1. Set loading to true and clear old slots
            _isLoading.value = true
            _availableSlots.value = emptyList()
            
            slotRepository.listenForDoctorSlotsByDate(doctorId, date)
                .collect { slots ->
                    // 2. Update slots and set loading to false
                    _availableSlots.value = slots.sortedBy { it.SlotStartTime }
                    _isLoading.value = false
                    Log.d("BookApptViewModel", "slots.size is ${slots.size}, availableSlots.value.size is ${availableSlots.value.size}")
                }
        }
    }
    
    fun onDateSelected(date: Date) {
        _selectedDate.value = date
    }

    fun onSlotSelected(slot: Slot) {
        _selectedSlot.value = slot
    }

    fun bookAppointment(patientId: String, slot: Slot, date: Date){
        viewModelScope.launch {
            appointmentRepository.bookAppointment(patientId, slot, date)
        }
    }
}
