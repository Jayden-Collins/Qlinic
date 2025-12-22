package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.data.model.Slot
import com.example.qlinic.data.repository.AppointmentRepository
import com.example.qlinic.data.repository.FirestoreAppointmentRepository
import com.example.qlinic.data.repository.ClinicStaffRepository
import com.example.qlinic.data.repository.PatientRepository
import com.example.qlinic.data.repository.SlotRepository
import com.example.qlinic.data.repository.SpecificDoctorScheduleDetails
import com.example.qlinic.utils.formatTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class BookApptViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private val slotRepository = SlotRepository()
    private val appointmentRepository: AppointmentRepository = FirestoreAppointmentRepository()
    private val clinicStaffRepository = ClinicStaffRepository()
    private val patientRepository = PatientRepository()
    private val doctorScheduleRepository = SpecificDoctorScheduleDetails()

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots = _availableSlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedSlot = MutableStateFlow<Slot?>(null)
    val selectedSlot = _selectedSlot.asStateFlow()

    private val _leaveDates = MutableStateFlow<List<Date>>(emptyList())
    val leaveDates = _leaveDates.asStateFlow()

    private val _leaveDatesLoaded = MutableStateFlow(false)
    val leaveDatesLoaded = _leaveDatesLoaded.asStateFlow()

    // Symptoms Dialog
    private val _symptoms = MutableStateFlow("")
    val symptoms = _symptoms.asStateFlow()

    private val _showSymptomsPopup = MutableStateFlow(false)
    val showSymptomsPopup = _showSymptomsPopup.asStateFlow()

    // Success Dialog
    private val _showSuccessPopup = MutableStateFlow(false)
    val showSuccessPopup = _showSuccessPopup.asStateFlow()

    private val _successMessage = MutableStateFlow("")
    val successMessage = _successMessage.asStateFlow()

    // Staff Booking Flow States
    private val _showPatientTypeSelectionPopup = MutableStateFlow(false)
    val showPatientTypeSelectionPopup = _showPatientTypeSelectionPopup.asStateFlow()

    private val _showExistingPatientIcPopup = MutableStateFlow(false)
    val showExistingPatientIcPopup = _showExistingPatientIcPopup.asStateFlow()

    private val _showNewPatientDetailsPopup = MutableStateFlow(false)
    val showNewPatientDetailsPopup = _showNewPatientDetailsPopup.asStateFlow()

    private val _patientIc = MutableStateFlow("")
    val patientIc = _patientIc.asStateFlow()

    private val _patientIcError = MutableStateFlow<String?>(null)
    val patientIcError = _patientIcError.asStateFlow()

    private val _newPatientFirstName = MutableStateFlow("")
    val newPatientFirstName = _newPatientFirstName.asStateFlow()

    private val _newPatientLastName = MutableStateFlow("")
    val newPatientLastName = _newPatientLastName.asStateFlow()

    private val _newPatientGender = MutableStateFlow("")
    val newPatientGender = _newPatientGender.asStateFlow()

    private val _newPatientIc = MutableStateFlow("")
    val newPatientIc = _newPatientIc.asStateFlow()

    private val _newPatientPhoneNumber = MutableStateFlow("")
    val newPatientPhoneNumber = _newPatientPhoneNumber.asStateFlow()

    private val _newPatientError = MutableStateFlow<String?>(null)
    val newPatientError = _newPatientError.asStateFlow()

    private val _bookingPatientId = MutableStateFlow<String?>(null)

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError = _bookingError.asStateFlow()

    fun clearBookingError() {
        _bookingError.value = null
    }

    private fun parseSlotTime(slotTime: String): java.util.Date? {
        val patterns = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a")
        for (pat in patterns) {
            try {
                val fmt = java.text.SimpleDateFormat(pat, java.util.Locale.getDefault())
                val parsed = fmt.parse(slotTime)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun getDoctorSlots(doctorId: String, date: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            _availableSlots.value = emptyList()
            _selectedSlot.value = null // Deselect slot when date changes

            slotRepository.listenForDoctorSlotsByDate(doctorId, date)
                .collect { slots ->
                    _availableSlots.value = slots.sortedBy { it.SlotStartTime }
                    _isLoading.value = false
                }
        }
    }

    fun loadLeaveDates(doctorId: String, month: Date) {
        viewModelScope.launch {
            _leaveDatesLoaded.value = false
            try {
                val leaves = doctorScheduleRepository.getDoctorLeavesForMonth(doctorId, month)
                _leaveDates.value = leaves
            } catch (e: Exception) {
                Log.e("BookApptViewModel", "Error loading leave dates", e)
            } finally {
                _leaveDatesLoaded.value = true
            }
        }
    }

    fun isDateOnLeave(date: Date): Boolean {
        val cal1 = normalizeDate(date)
        return _leaveDates.value.any { leaveDate ->
            val cal2 = normalizeDate(leaveDate)
            cal1.timeInMillis == cal2.timeInMillis
        }
    }

    private fun normalizeDate(date: Date): java.util.Calendar {
        return java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }

    fun onDateSelected(date: Date) {
        Log.d("BookApptViewModel", "onDateSelected: New date is $date. Current selected slot is ${_selectedSlot.value}")
        val oldNormalized = normalizeDate(_selectedDate.value)
        val newNormalized = normalizeDate(date)

        // Only update if the date has actually changed.
        // This prevents recomposition from clearing the selected slot unnecessarily.
        if (oldNormalized.timeInMillis != newNormalized.timeInMillis) {
            _selectedDate.value = date
            Log.d("BookApptViewModel", "onDateSelected: Date HAS changed. Slot will be cleared by getDoctorSlots.")
        } else {
            Log.d("BookApptViewModel", "onDateSelected: Date has NOT changed. No action taken.")
        }
    }

    fun onSlotSelected(slot: Slot) {
        Log.d("BookApptViewModel", "onSlotSelected: User selected slot ${slot.SlotID} at ${slot.SlotStartTime}. Current selection is ${_selectedSlot.value?.SlotID}")
        // Toggle logic: click again to deselect
        if (_selectedSlot.value?.SlotID == slot.SlotID) {
            _selectedSlot.value = null
            Log.d("BookApptViewModel", "onSlotSelected: Slot DESELECTED.")
        } else {
            _selectedSlot.value = slot
            Log.d("BookApptViewModel", "onSlotSelected: Slot SET to ${slot.SlotID}.")
            clearBookingError()
        }
    }

    fun onSymptomsChanged(newSymptoms: String) {
        _symptoms.value = newSymptoms
    }

    fun onBookAppointmentClick(isStaff: Boolean) {
        if (isStaff) {
            _showPatientTypeSelectionPopup.value = true
        } else {
            _showSymptomsPopup.value = true
        }
    }

    fun onSelectPatientType(isNew: Boolean) {
        _showPatientTypeSelectionPopup.value = false
        if (isNew) {
            _showNewPatientDetailsPopup.value = true
        } else {
            _showExistingPatientIcPopup.value = true
        }
    }

    fun onDismissPatientTypeSelection() {
        _showPatientTypeSelectionPopup.value = false
    }

    fun backToPatientTypeSelection() {
        // Hide current popups
        _showExistingPatientIcPopup.value = false
        _showNewPatientDetailsPopup.value = false

        // Show the previous popup
        _showPatientTypeSelectionPopup.value = true

        // Reset fields from the closed popups
        _patientIc.value = ""
        _patientIcError.value = null
        _newPatientFirstName.value = ""
        _newPatientLastName.value = ""
        _newPatientGender.value = ""
        _newPatientIc.value = ""
        _newPatientPhoneNumber.value = ""
        _newPatientError.value = null
    }

    fun onPatientIcChanged(ic: String) {
        _patientIc.value = ic
        if (_patientIcError.value != null) {
            _patientIcError.value = null // Clear error on new input
        }
    }

    fun onNewPatientInfoChanged(
        firstName: String? = null,
        lastName: String? = null,
        gender: String? = null,
        ic: String? = null,
        phone: String? = null
    ) {
        firstName?.let { _newPatientFirstName.value = it }
        lastName?.let { _newPatientLastName.value = it }
        gender?.let { _newPatientGender.value = it }
        ic?.let { _newPatientIc.value = it }
        phone?.let { _newPatientPhoneNumber.value = it }
        if (_newPatientError.value != null) {
            _newPatientError.value = null // Clear error on new input
        }
    }


    fun onFindExistingPatient() {
        viewModelScope.launch {
            val rawIc = _patientIc.value
            val icWithDashes = formatIcWithDashes(rawIc)
            Log.d("BookApptViewModel", "onFindExistingPatient: Raw IC='$rawIc', Formatted IC='$icWithDashes'")

            val patient = patientRepository.findPatientByIc(icWithDashes)
            if (patient != null) {
                Log.d("BookApptViewModel", "onFindExistingPatient: Patient FOUND. ID=${patient.patientId}, Name=${patient.firstName}")
                _bookingPatientId.value = patient.patientId
                _showExistingPatientIcPopup.value = false
                _showSymptomsPopup.value = true
            } else {
                Log.d("BookApptViewModel", "onFindExistingPatient: Patient NOT FOUND for IC '$icWithDashes'")
                _patientIcError.value = "Patient with this IC not found."
            }
        }
    }

    fun onCreateNewPatient() {
        viewModelScope.launch {
            // 1. Validation - Empty fields
            val firstName = _newPatientFirstName.value
            val lastName = _newPatientLastName.value
            val gender = _newPatientGender.value
            val ic = _newPatientIc.value
            val phone = _newPatientPhoneNumber.value

            if (firstName.isBlank() || lastName.isBlank() || gender.isBlank() || ic.isBlank() || phone.isBlank()) {
                _newPatientError.value = "All fields must be filled."
                return@launch
            }

            // 2. Validation - IC format
            if (ic.length != 12 || !ic.all { it.isDigit() }) {
                _newPatientError.value = "Invalid IC format. It must be 12 digits without dashes."
                return@launch
            }

            val icWithDashes = formatIcWithDashes(ic)

            // 3. Validation - Duplicate IC
            val existingPatient = patientRepository.findPatientByIc(icWithDashes)
            if (existingPatient != null) {
                _newPatientError.value = "A patient with this IC already exists."
                return@launch
            }

            // If all validations pass
            _newPatientError.value = null // Clear any previous errors

            try {
                val dateOfBirth = SimpleDateFormat("yyMMdd", Locale.getDefault())
                    .parse(ic.substring(0, 6))

                Log.d("BookApptViewModel", "onCreateNewPatient: Creating patient with IC='$icWithDashes'")

                val newPatient = Patient(
                    patientId = "", // ID generated in repository
                    firstName = firstName,
                    lastName = lastName,
                    gender = gender,
                    ic = icWithDashes,
                    phoneNumber = "+60$phone",
                    dateOfBirth = dateOfBirth
                )
                val newPatientId = patientRepository.addPatient(newPatient)
                if (newPatientId.isNotEmpty()) {
                    Log.d("BookApptViewModel", "onCreateNewPatient: Success. ID=$newPatientId")
                    _bookingPatientId.value = newPatientId
                    _showNewPatientDetailsPopup.value = false
                    _showSymptomsPopup.value = true
                } else {
                    Log.e("BookApptViewModel", "onCreateNewPatient: FAILED.")
                    _newPatientError.value = "Failed to create patient. Please try again."
                }
            } catch (e: Exception) {
                Log.e("BookApptViewModel", "onCreateNewPatient: Error", e)
                _newPatientError.value = "An unexpected error occurred: ${e.message}"
            }
        }
    }

    private fun formatIcWithDashes(ic: String): String {
        return if (ic.length == 12) {
            "${ic.substring(0, 6)}-${ic.substring(6, 8)}-${ic.substring(8)}"
        } else {
            ic
        }
    }

    fun confirmBooking(isStaff: Boolean, doctorId: String, symptoms: String) {
        Log.d("BookApptViewModel", "confirmBooking: Attempting to book. Current selected slot is \\${_selectedSlot.value}")
        viewModelScope.launch {
            try {
                _showSymptomsPopup.value = false
                val slot = _selectedSlot.value
                Log.d("BookApptViewModel", "confirmBooking: Selected slot: \\${slot}")
                if (slot == null) {
                    Log.e("BookApptViewModel", "confirmBooking: FAILED. Reason: _selectedSlot.value is null.")
                    _bookingError.value = "Please select a time slot."
                    return@launch
                }

                val selectedDateAsDate = _selectedDate.value
                Log.d("BookApptViewModel", "confirmBooking: Selected date: \\${selectedDateAsDate}")

                // Convert java.util.Date to java.time.LocalDate
                val localDate = selectedDateAsDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val time = LocalTime.parse(slot.SlotStartTime)
                val localDateTime = LocalDateTime.of(localDate, time)
                val appointmentDateTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
                Log.d("BookApptViewModel", "confirmBooking: Appointment date/time: \\${appointmentDateTime}")

                val currentUserId = sessionManager.getSavedUserId() ?: ""
                val finalPatientId = if (isStaff) _bookingPatientId.value else currentUserId
                Log.d("BookApptViewModel", "confirmBooking: Final patient ID: \\${finalPatientId}")

                if (finalPatientId.isNullOrEmpty()) {
                    Log.e("BookApptViewModel", "confirmBooking: Patient ID is null or empty.")
                    _bookingError.value = "Could not identify patient. Please log in again."
                    return@launch
                }

                Log.d("BookApptViewModel", "confirmBooking: Checking if slot is taken for slotId=\\${slot.SlotID}, appointmentDateTime=\\${appointmentDateTime}")
                val isTaken = appointmentRepository.isSlotTaken(slot.SlotID, appointmentDateTime)
                Log.d("BookApptViewModel", "confirmBooking: isSlotTaken result: \\${isTaken}")
                if (isTaken) {
                    Log.w("BookApptViewModel", "confirmBooking: Slot is already taken.")
                    _bookingError.value = "This time slot is no longer available. Please select another time."
                    getDoctorSlots(doctorId, selectedDateAsDate) // Refresh slots
                    return@launch
                }

                Log.d("BookApptViewModel", "confirmBooking: Attempting to book appointment for patientId=\\${finalPatientId}, slotId=\\${slot.SlotID}, appointmentDateTime=\\${appointmentDateTime}, symptoms=\\${symptoms}")
                val isSuccess = appointmentRepository.bookAppointment(finalPatientId, slot, appointmentDateTime, symptoms)
                Log.d("BookApptViewModel", "confirmBooking: bookAppointment result: \\${isSuccess}")
                if (isSuccess) {
                    val staffMember = clinicStaffRepository.getStaffMember(doctorId)
                    val doctorName = staffMember?.let { "Dr. \\${it.firstName} \\${it.lastName}" } ?: "the doctor"

                    val patient = patientRepository.getPatient(finalPatientId)
                    val patientName = patient?.let { "\\${it.firstName} \\${it.lastName}" } ?: "the patient"

                    val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(appointmentDateTime)
                    val formattedTime = formatTime(slot.SlotStartTime)
                    _successMessage.value =
                        if (isStaff) "The appointment for $patientName with $doctorName is confirmed for $formattedDate, at $formattedTime."
                        else "Your appointment with $doctorName is confirmed for $formattedDate, at $formattedTime."
                    _showSuccessPopup.value = true
                    Log.d("BookApptViewModel", "confirmBooking: Appointment booked successfully.")
                } else {
                    Log.e("BookApptViewModel", "confirmBooking: bookAppointment returned false. Possible reasons: slot double-booked, Firestore error, or logic bug.")
                    _bookingError.value = "Failed to book appointment. Please try again."
                }
            } catch (e: Exception) {
                Log.e("BookApptViewModel", "confirmBooking failed", e)
                _bookingError.value = "An unexpected error occurred: \\${e.message}"
            }
        }
    }


    fun dismissSuccessPopup() {
        _showSuccessPopup.value = false
        // Trigger navigation back to home via some state or event
    }
}

class BookApptViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookApptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookApptViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
