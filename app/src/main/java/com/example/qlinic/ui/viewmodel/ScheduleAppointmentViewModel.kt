package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AvailabilityException
import com.example.qlinic.data.model.SpecificDoctorInfo
import com.example.qlinic.data.model.Slot
import com.example.qlinic.data.repository.RescheduleAppointmentDetails
import com.example.qlinic.data.repository.SpecificDoctorScheduleDetails
import com.example.qlinic.data.repository.SpecificDoctorScheduleDetails.Availability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ScheduleAppointmentViewModel : ViewModel(){
    private val repository = SpecificDoctorScheduleDetails()
    private val rescheduleRepository = RescheduleAppointmentDetails()

    private var currentStaffID: String = "S001"

    fun setCurrentStaffId(staffId: String) {
        currentStaffID = staffId
    }

    private val _doctor = MutableStateFlow<SpecificDoctorInfo?>(null)
    val doctor: StateFlow<SpecificDoctorInfo?> = _doctor

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _availabilityExceptions = MutableStateFlow<List<AvailabilityException>>(emptyList())
    val availabilityExceptions: StateFlow<List<AvailabilityException>> = _availabilityExceptions

    private val _leaveDates = MutableStateFlow<List<Date>>(emptyList())
    val leaveDates: StateFlow<List<Date>> = _leaveDates

    private val _leaveDatesLoaded = MutableStateFlow(false)
    val leaveDatesLoaded: StateFlow<Boolean> = _leaveDatesLoaded

    private val _availability = MutableStateFlow<Map<Date, Availability>>(emptyMap())
    val availabilityStatus: StateFlow<Map<Date, Availability>> = _availability

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate

    private val _selectedDates = MutableStateFlow<List<Date>>(emptyList())
    val selectedDates: StateFlow<List<Date>> = _selectedDates

    private val _bookedDates = MutableStateFlow<List<Date>>(emptyList())
    val bookedDates: StateFlow<List<Date>> = _bookedDates

    private val _dateStatus = MutableStateFlow<Map<Date, Availability>>(emptyMap())
    val dateStatus: StateFlow<Map<Date, Availability>> = _dateStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _patientNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val patientNames: StateFlow<Map<String, String>> = _patientNames

    private val _slotTimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val slotTimes: StateFlow<Map<String, String>> = _slotTimes

    fun loadDoctorSchedule(doctorId: String){
        viewModelScope.launch{
            _isLoading.value = true
            _errorMessage.value = null

            try{
                val doctorInfo = repository.getDoctorById(doctorId)
                _doctor.value = doctorInfo

                if (doctorInfo == null) {
                    _errorMessage.value = "Doctor not found"
                    return@launch
                }
                loadAppointmentDetailsByMonth(doctorId, Date())
                loadAvailabilityExceptions(doctorId)
                loadLeaveDates(doctorId, Date())
                updateDateStatus(doctorId, Date())

            } catch (e: Exception){
                _errorMessage.value = "Error loading doctor details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsLeave(doctorId: String, dates: List<Date>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val todayStartCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                val filteredDates = dates.filter { d ->
                    val c = Calendar.getInstance().apply { time = d; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    !c.time.before(todayStartCal.time)
                }

                if (filteredDates.isEmpty()) {
                    _errorMessage.value = "Cannot mark past dates as leave"
                    return@launch
                }
                val success = repository.markDoctorAsLeave(
                    doctorID = doctorId,
                    dates = filteredDates,
                    staffID = currentStaffID,
                    reason = "On Leave"
                )

                if (success) {
                    val updatedLeaves = _leaveDates.value.toMutableList()
                    filteredDates.forEach { date ->
                        if (!updatedLeaves.any { isSameDay(it, date) }) {
                            updatedLeaves.add(date)
                        }
                    }
                    _leaveDates.value = updatedLeaves
                    updateDateStatusForDates(filteredDates, Availability.ONLEAVE)
                    _selectedDates.value = emptyList()
                    _selectedDate.value = null

                    loadLeaveDates(doctorId, Date())
                    updateDateStatus(doctorId, Date())
                } else {
                    _errorMessage.value = "Failed to mark leave"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark leave: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateDateStatusForDates(dates: List<Date>, status: Availability) {
        val currentStatus = _dateStatus.value.toMutableMap()
        dates.forEach { date ->
            currentStatus[date] = status
        }
        _dateStatus.value = currentStatus
    }

    fun loadAppointmentDetailsByMonth(doctorId: String, month: Date){
        viewModelScope.launch{
            try {
                val calendar = Calendar.getInstance().apply {
                    time = month
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                val startDate = calendar.time
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = calendar.time

                val appointmentsList = repository.getAppointmentsForDoctor(
                    doctorID = doctorId,
                    startDate = startDate,
                    endDate = endDate
                )

                _appointments.value = appointmentsList
                updateBookedDates(appointmentsList)

                val patientIds = appointmentsList.mapNotNull { it.patientId }.distinct()
                if (patientIds.isNotEmpty()) {
                    try {
                        val namesMap = repository.getPatientNames(patientIds)
                        _patientNames.value = namesMap
                    } catch (e: Exception) {
                        _patientNames.value = emptyMap()
                    }
                } else {
                    _patientNames.value = emptyMap()
                }

                val slotIds = appointmentsList.mapNotNull { it.slotId }.distinct()
                if (slotIds.isNotEmpty()) {
                    try {
                        val slotMap = repository.getSlotTimesByIds(slotIds)
                        _slotTimes.value = slotMap
                    } catch (e: Exception) {
                        _slotTimes.value = emptyMap()
                    }
                } else {
                    _slotTimes.value = emptyMap()
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading appointments: ${e.message}"
            }
        }
    }

    private fun updateBookedDates(appointments: List<Appointment>) {
        val bookedDatesList = appointments.map { appointment ->
            val cal = Calendar.getInstance().apply {
                time = appointment.appointmentDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            cal.time
        }.distinct()

        _bookedDates.value = bookedDatesList
    }

    fun cancelLeave(doctorId: String, dates: List<Date>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.cancelDoctorLeave(doctorId, dates)

                if (success) {
                    val updatedLeaves = _leaveDates.value.filter { date ->
                        !dates.any { isSameDay(it, date) }
                    }
                    _leaveDates.value = updatedLeaves
                    updateDateStatusForDates(dates, Availability.AVAILABLE)
                    _selectedDates.value = emptyList()
                    _selectedDate.value = null
                    loadLeaveDates(doctorId, Date())
                } else {
                    _errorMessage.value = "Failed to cancel leave"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to cancel leave: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun loadAvailabilityExceptions(doctorId: String) {
        try {
            val exceptions = repository.getDoctorAvailabilityExceptions(doctorId)
            _availabilityExceptions.value = exceptions
        } catch (e: Exception) { }
    }

    fun refreshAvailabilityExceptions(doctorId: String) {
        viewModelScope.launch {
            loadAvailabilityExceptions(doctorId)
        }
    }

    suspend fun loadLeaveDates(doctorId: String, month: Date){
        try {
            _leaveDatesLoaded.value = false
            val leaves = repository.getDoctorLeavesForMonth(doctorId, month)
            _leaveDates.value = leaves
            updateAvailabilityStatusMap(leaves)
            _leaveDatesLoaded.value = true
        } catch (e: Exception) {
            _leaveDatesLoaded.value = true 
        }
    }

    fun refreshLeaveDates(doctorId: String, month: Date) {
        viewModelScope.launch {
            loadLeaveDates(doctorId, month)
        }
    }

    private suspend fun updateDateStatus(doctorId: String, monthDate: Date) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = monthDate
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }

            val startDate = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val endDate = calendar.time

            val newStatusMap = mutableMapOf<Date, Availability>()
            val newBookedDates = mutableListOf<Date>()

            val tempCalendar = Calendar.getInstance()
            tempCalendar.time = startDate

            while (tempCalendar.time <= endDate) {
                val currentDate = tempCalendar.time
                val status = repository.isDoctorAvailable(doctorId, currentDate)
                newStatusMap[currentDate] = status
                if (status == Availability.BOOKED) {
                    newBookedDates.add(currentDate)
                }
                tempCalendar.add(Calendar.DATE, 1)
            }

            _dateStatus.value = newStatusMap
            _bookedDates.value = newBookedDates

        } catch (e: Exception) { }
    }

    fun getDateStatus(date: Date): Availability {
        val normalizedDate = startOfDay(date)
        return _dateStatus.value.entries.find { isSameDay(it.key, normalizedDate) }?.value ?: Availability.AVAILABLE
    }

    fun isDateBooked(date: Date): Boolean {
        return getDateStatus(date) == Availability.BOOKED
    }

    private fun updateAvailabilityStatusMap(leaveDates: List<Date>) {
        val newStatusMap = mutableMapOf<Date, Availability>()
        leaveDates.forEach { date ->
            newStatusMap[date] = Availability.ONLEAVE
        }
        _availability.value = newStatusMap
    }

    fun selectDate(date: Date?) {
        if (date == null) {
            _selectedDate.value = null
            _selectedDates.value = emptyList()
            return
        }
        // Toggle logic: if already selected, clear it
        if (_selectedDate.value != null && isSameDay(_selectedDate.value!!, date)) {
            _selectedDate.value = null
            _selectedDates.value = emptyList()
        } else {
            _selectedDate.value = date
            _selectedDates.value = listOf(date)
        }
    }

    fun selectMultipleDates(dates: List<Date>) {
        _selectedDates.value = dates
        _selectedDate.value = if (dates.size == 1) dates[0] else null
    }

    fun getAppointmentsForDate(date: Date): List<Appointment> {
        return _appointments.value.filter { appointment ->
            isSameDay(appointment.appointmentDate, date)
        }
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun getAppointmentDates(): List<Date> {
        return _appointments.value.map { appt ->
            startOfDay(appt.appointmentDate)
        }.distinct()
    }

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    suspend fun checkDateAvailability(doctorId: String, date: Date): Availability {
        return repository.isDoctorAvailable(doctorId, date)
    }

    fun isDateOnLeave(date: Date): Boolean {
        return _leaveDates.value.any { leaveDate ->
            isSameDay(leaveDate, date)
        }
    }

    fun getPatientDisplayName(patientId: String): String {
        return _patientNames.value[patientId] ?: patientId
    }

    fun getSlotStartTime(slotId: String?): String? {
        if (slotId == null) return null
        return _slotTimes.value[slotId]
    }

    fun deleteAppointment(doctorId: String, appointmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteAppointment(appointmentId)
                if (success) {
                    loadAppointmentDetailsByMonth(doctorId, Date())
                } else {
                    _errorMessage.value = "Failed to delete appointment"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting appointment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun listenForSlots(doctorId: String, date: Date): Flow<List<Slot>> {
        return rescheduleRepository.listenForDoctorSlotsByDate(doctorId, date)
    }

    fun rescheduleAppointment(slot: Slot, appointmentId: String, doctorId: String, newAppointmentDate: Date, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                rescheduleRepository.rescheduleAppointment(slot, appointmentId, newAppointmentDate)
                loadAppointmentDetailsByMonth(doctorId, newAppointmentDate)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
