package com.example.qlinic.ui.viewModels

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ScheduleAppointmentViewModel : ViewModel(){
    // repository for specific doctor schedule details
    private val repository = SpecificDoctorScheduleDetails()
    // repository for rescheduling (slots + reschedule operation)
    private val rescheduleRepository = RescheduleAppointmentDetails()

    // Temporary hardcoded staff ID until authentication/login is implemented
    // Replace this with the real staff ID from the authenticated user when available
    // Made mutable so you can set it from the login/auth flow later
    private var currentStaffID: String = "S003"

    // Call this from your auth/login flow once you have the real staff ID
    fun setCurrentStaffId(staffId: String) {
        currentStaffID = staffId
    }

    // state flow to hold specific doctor info
    private val _doctor = MutableStateFlow<SpecificDoctorInfo?>(null)
    val doctor: StateFlow<SpecificDoctorInfo?> = _doctor

    // state flow to hold list of appointments
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _availabilityExceptions = MutableStateFlow<List<AvailabilityException>>(emptyList())
    val availabilityExceptions: StateFlow<List<AvailabilityException>> = _availabilityExceptions

    private val _leaveDates = MutableStateFlow<List<Date>>(emptyList())
    val leaveDates: StateFlow<List<Date>> = _leaveDates
    // whether leave dates have been loaded at least once for the current view/month
    private val _leaveDatesLoaded = MutableStateFlow(false)
    val leaveDatesLoaded: StateFlow<Boolean> = _leaveDatesLoaded

    private val _availability = MutableStateFlow<Map<Date, Availability>>(emptyMap())
    val availabilityStatus: StateFlow<Map<Date, Availability>> = _availability

    // state flow to hold selected date (now nullable)
    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate

    private val _selectedDates = MutableStateFlow<List<Date>>(emptyList())
    val selectedDates: StateFlow<List<Date>> = _selectedDates

    // Add booked dates state
    private val _bookedDates = MutableStateFlow<List<Date>>(emptyList())
    val bookedDates: StateFlow<List<Date>> = _bookedDates

    // Add availability status for each date map state flow
    private val _dateStatus = MutableStateFlow<Map<Date, SpecificDoctorScheduleDetails.Availability>>(emptyMap())
    val dateStatus: StateFlow<Map<Date, SpecificDoctorScheduleDetails.Availability>> = _dateStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // New: map of patientId -> displayName populated after appointments load
    private val _patientNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val patientNames: StateFlow<Map<String, String>> = _patientNames

    // New: map of slotId -> slotStartTime populated after appointments load
    private val _slotTimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val slotTimes: StateFlow<Map<String, String>> = _slotTimes

    // load doctor schedule details
    fun loadDoctorSchedule(doctorID: String){
        viewModelScope.launch{
            _isLoading.value = true
            _errorMessage.value = null

            try{
                // Load doctor details
                val doctorInfo = repository.getDoctorById(doctorID)
                println("[loadDoctorSchedule] called with doctorID=$doctorID, doctorInfoExists=${doctorInfo != null}")
                _doctor.value = doctorInfo

                if (doctorInfo == null) {
                    _errorMessage.value = "Doctor not found"
                    return@launch
                }
                // load appointments for current month
                loadAppointmentDetailsByMonth(doctorID, Date())

                // Load availability exceptions
                loadAvailabilityExceptions(doctorID)

                // Load leave dates for current month
                loadLeaveDates(doctorID, Date())

                //update leaves dates
                updateDateStatus(doctorID, Date())

            } catch (e: Exception){
                _errorMessage.value = "Error loading doctor details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // Function to mark dates as leave
    fun markAsLeave(doctorID: String, dates: List<Date>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Call repository to mark leave in Firestore
                val success = repository.markDoctorAsLeave(
                    doctorID = doctor.value?.id ?: "",
                    dates = dates,
                    staffID = currentStaffID,
                    reason = "On Leave"
                )

                if (success) {
                    // Update local state
                    val updatedLeaves = _leaveDates.value.toMutableList()
                    dates.forEach { date ->
                        if (!updatedLeaves.any { isSameDay(it, date) }) {
                            updatedLeaves.add(date)
                        }
                    }
                    _leaveDates.value = updatedLeaves

                    // Update date status
                    updateDateStatusForDates(dates, Availability.ONLEAVE)

                    // Clear selection
                    _selectedDates.value = emptyList()
                    _selectedDate.value = null

                    // Reload data from Firebase using doctor id (not staff id)
                    val docId = doctor.value?.id ?: doctorID
                    loadLeaveDates(docId, Date())
                    // refresh full month status so calendar markers update
                    updateDateStatus(docId, Date())
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


    // load appointment details by month for a specific doctor
    fun loadAppointmentDetailsByMonth(doctorID: String, month: Date){
        viewModelScope.launch{
            try {
                val calendar = Calendar.getInstance().apply {
                    time = month
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val startDate = calendar.time
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = calendar.time

                val appointmentsList = repository.getAppointmentsForDoctor(
                    doctorID = doctorID,
                    startDate = startDate,
                    endDate = endDate
                )

                println("[loadAppointmentDetailsByMonth] doctorID=$doctorID, start=$startDate, end=$endDate, returned=${appointmentsList.size}")

                _appointments.value = appointmentsList
                updateBookedDates(appointmentsList)

                // After appointments are loaded, fetch patient names for unique patient IDs
                val patientIds = appointmentsList.mapNotNull { it.patientID }.distinct()
                if (patientIds.isNotEmpty()) {
                    try {
                        val namesMap = repository.getPatientNames(patientIds)
                        _patientNames.value = namesMap
                    } catch (e: Exception) {
                        println("Error loading patient names: ${e.message}")
                        _patientNames.value = emptyMap()
                    }
                } else {
                    _patientNames.value = emptyMap()
                }

                // Fetch slot start times for the appointments' slot IDs and store in state
                val slotIds = appointmentsList.mapNotNull { it.slotID }.distinct()
                if (slotIds.isNotEmpty()) {
                    try {
                        val slotMap = repository.getSlotTimesByIds(slotIds)
                        _slotTimes.value = slotMap
                    } catch (e: Exception) {
                        println("Error loading slot times: ${e.message}")
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

    // Function to update booked dates from appointments
    private fun updateBookedDates(appointments: List<Appointment>) {
        val bookedDates = appointments.map { appointment ->
            // Extract just the date part (without time)
            val cal = Calendar.getInstance().apply {
                time = appointment.appointmentDateTime.toDate()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.time
        }.distinct()

        _bookedDates.value = bookedDates
    }

    // Function to cancel leave for a given doctor (accept doctorID explicitly)
    fun cancelLeave(doctorID: String, dates: List<Date>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.cancelDoctorLeave(doctorID, dates)

                if (success) {
                    // Update local state
                    val updatedLeaves = _leaveDates.value.filter { date ->
                        !dates.any { isSameDay(it, date) }
                    }
                    _leaveDates.value = updatedLeaves

                    // Update date status
                    updateDateStatusForDates(dates, Availability.AVAILABLE)

                    // Clear selection
                    _selectedDates.value = emptyList()
                    _selectedDate.value = null

                    // Reload leave dates for that doctor
                    loadLeaveDates(doctorID, Date())
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

    // load availability exceptions for a specific doctor
    suspend fun loadAvailabilityExceptions(doctorId: String) {
        try {
            val exceptions = repository.getDoctorAvailabilityExceptions(doctorId)
            _availabilityExceptions.value = exceptions
        } catch (e: Exception) {
            println("Error loading availability exceptions: ${e.message}")
        }
    }

    // Non-suspending wrapper to refresh availability exceptions from UI
    fun refreshAvailabilityExceptions(doctorId: String) {
        viewModelScope.launch {
            loadAvailabilityExceptions(doctorId)
        }
    }

    // load leave dates for a specific doctor
    suspend fun loadLeaveDates(doctorID: String, month: Date){
        try {
            _leaveDatesLoaded.value = false
            val leaves = repository.getDoctorLeavesForMonth(doctorID, month)
            _leaveDates.value = leaves

            // Update availability status map
            updateAvailabilityStatusMap(leaves)
            _leaveDatesLoaded.value = true
        } catch (e: Exception) {
            println("Error loading leave dates: ${e.message}")
            _leaveDatesLoaded.value = true // mark loaded to avoid blocking UI forever
        }
    }

    // Convenience non-suspending wrapper to refresh leave dates from UI (launches in viewModelScope)
    fun refreshLeaveDates(doctorID: String, month: Date) {
        viewModelScope.launch {
            loadLeaveDates(doctorID, month)
        }
    }

    // Update date status based on appointments and leaves
    private suspend fun updateDateStatus(doctorId: String, monthDate: Date) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = monthDate
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startDate = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val endDate = calendar.time

            val newStatusMap = mutableMapOf<Date, SpecificDoctorScheduleDetails.Availability>()
            val newBookedDates = mutableListOf<Date>()

            // Check each day in the month
            val tempCalendar = Calendar.getInstance()
            tempCalendar.time = startDate

            while (tempCalendar.time <= endDate) {
                val currentDate = tempCalendar.time
                val status = repository.isDoctorAvailable(doctorId, currentDate)

                newStatusMap[currentDate] = status

                if (status == SpecificDoctorScheduleDetails.Availability.BOOKED) {
                    newBookedDates.add(currentDate)
                }

                tempCalendar.add(Calendar.DATE, 1)
            }

            _dateStatus.value = newStatusMap
            _bookedDates.value = newBookedDates

        } catch (e: Exception) {
            println("Error updating date status: ${e.message}")
        }
    }
    // Get status for a specific date
    fun getDateStatus(date: Date): SpecificDoctorScheduleDetails.Availability {
        return _dateStatus.value[date] ?: SpecificDoctorScheduleDetails.Availability.AVAILABLE
    }

    // Check if date is booked
    fun isDateBooked(date: Date): Boolean {
        return getDateStatus(date) == SpecificDoctorScheduleDetails.Availability.BOOKED
    }


    // update availability status map based on leave dates
    private fun updateAvailabilityStatusMap(leaveDates: List<Date>) {
        val newStatusMap = mutableMapOf<Date, Availability>()

        leaveDates.forEach { date ->
            newStatusMap[date] = Availability.ONLEAVE
        }

        _availability.value = newStatusMap
    }

    // when staff selects a date
    fun selectDate(date: Date) {
        _selectedDate.value = date
        _selectedDates.value = listOf(date)
    }

    // when staff selects multiple dates
    // In ScheduleAppointmentViewModel.kt
    fun selectMultipleDates(dates: List<Date>) {
        _selectedDates.value = dates
        // set selectedDate to single date if exactly one selected, otherwise clear
        _selectedDate.value = if (dates.size == 1) dates[0] else null
    }

    // helper function to check if two dates are on the same day
    fun getAppointmentsForDate(date: Date): List<Appointment> {
        return _appointments.value.filter { appointment ->
            isSameDay(appointment.appointmentDateTime.toDate(), date)
        }
    }


    // Exposed helper so UI composables can check if two dates are the same day
    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    // helper function to get all appointment dates
    fun getAppointmentDates(): List<Date> {
        // Return dates normalized to start of day so calendar matching works by date only
        return _appointments.value.map { appt ->
            val cal = Calendar.getInstance().apply { time = appt.appointmentDateTime.toDate() }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.time
        }.distinct()
    }

    // Helper: normalize a Date to start of day (00:00:00.000)
    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    // check date availability for a specific doctor
    suspend fun checkDateAvailability(doctorId: String, date: Date): Availability {
        return repository.isDoctorAvailable(doctorId, date)
    }

    // check if a date is on leave
    fun isDateOnLeave(date: Date): Boolean {
        return _leaveDates.value.any { leaveDate ->
            isSameDay(leaveDate, date)
        }
    }

    // New helper to read patient display name by id (fallback to id)
    fun getPatientDisplayName(patientId: String): String {
        return _patientNames.value[patientId] ?: patientId
    }

    // Helper to get slot start time by slotId (fallback null)
    fun getSlotStartTime(slotId: String?): String? {
        if (slotId == null) return null
        return _slotTimes.value[slotId]
    }

    // Delete appointment by business appointmentId and refresh appointments for the given doctor
    fun deleteAppointment(doctorID: String, appointmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteAppointment(appointmentId)
                if (success) {
                    // Refresh appointments for current month
                    loadAppointmentDetailsByMonth(doctorID, Date())
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

    // reschedule appointment 
    fun listenForSlots(doctorID: String, date: Date): Flow<List<Slot>> {
        return rescheduleRepository.listenForDoctorSlotsByDate(doctorID, date)
    }

    fun rescheduleAppointment(slot: Slot, appointmentId: String, newAppointmentDate: Date, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                rescheduleRepository.rescheduleAppointment(slot, appointmentId, newAppointmentDate)

                // Refresh appointments for the current month (use doctor id if available)
                val docId = doctor.value?.id ?: return@launch
                loadAppointmentDetailsByMonth(docId, newAppointmentDate)

                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}