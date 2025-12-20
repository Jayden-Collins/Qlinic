package com.example.qlinic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.model.Slot
import com.example.qlinic.data.repository.AppointmentRepository
import com.example.qlinic.data.repository.FirestoreAppointmentRepository
import com.example.qlinic.data.repository.ClinicStaffRepository
import com.example.qlinic.data.repository.PatientRepository
import com.example.qlinic.data.repository.SlotRepository
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

class BookApptViewModel : ViewModel() {
    private val slotRepository = SlotRepository()
    private val appointmentRepository: AppointmentRepository = FirestoreAppointmentRepository()
    private val clinicStaffRepository = ClinicStaffRepository()
    private val patientRepository = PatientRepository()

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots = _availableSlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedSlot = MutableStateFlow<Slot?>(null)
    val selectedSlot = _selectedSlot.asStateFlow()

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

    private val _bookingPatientId = MutableStateFlow<String?>(null)


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

    fun onDateSelected(date: Date) {
        _selectedDate.value = date
    }

    fun onSlotSelected(slot: Slot) {
        _selectedSlot.value = slot
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
            val dateOfBirth = SimpleDateFormat("yyMMdd", Locale.getDefault())
                .parse(_newPatientIc.value.substring(0, 6))

            val icWithDashes = formatIcWithDashes(_newPatientIc.value)
            Log.d("BookApptViewModel", "onCreateNewPatient: Creating patient with IC='$icWithDashes'")

            val newPatient = Patient(
                patientId = "", // ID generated in repository
                firstName = _newPatientFirstName.value,
                lastName = _newPatientLastName.value,
                gender = _newPatientGender.value,
                ic = icWithDashes,
                phoneNumber = "+60" + _newPatientPhoneNumber.value,
                dateOfBirth = dateOfBirth
            )
            val newPatientId = patientRepository.addPatient(newPatient)
            if (newPatientId != "") {
                Log.d("BookApptViewModel", "onCreateNewPatient: Success. ID=$newPatientId")
                _bookingPatientId.value = newPatientId
                _showNewPatientDetailsPopup.value = false
                _showSymptomsPopup.value = true
            } else {
                Log.e("BookApptViewModel", "onCreateNewPatient: FAILED.")
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
        _showSymptomsPopup.value = false
        val slot = _selectedSlot.value ?: return
        val selectedDateAsDate = _selectedDate.value

        // Convert java.util.Date to java.time.LocalDate
        val localDate = selectedDateAsDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        // Parse the time from the selected slot
        val time = LocalTime.parse(slot.SlotStartTime)

        // Combine the date and time into a single LocalDateTime
        val localDateTime = LocalDateTime.of(localDate, time)

        // Convert the LocalDateTime back to a java.util.Date object for Firestore
        val appointmentDateTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())

        //TODO: Replace placeholder patientID
        val finalPatientId = if (isStaff) _bookingPatientId.value ?: return else "v9i0pTJ4KtKUJ77SQwfg"

        viewModelScope.launch {
            val isSuccess = appointmentRepository.bookAppointment(finalPatientId, slot, appointmentDateTime, symptoms)
            if (isSuccess) {
                // Fetch doctor details to get the name
                val staffMember = clinicStaffRepository.getStaffMember(doctorId)
                val doctorName = staffMember?.let { "Dr. ${it.firstName} ${it.lastName}" } ?: "the doctor"

                // Fetch patient details to get the name
                val patient = patientRepository.getPatient(finalPatientId)
                val patientName = patient?.let { "${it.firstName} ${it.lastName}" } ?: "the patient"

                val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(appointmentDateTime)
                val formattedTime = formatTime(slot.SlotStartTime)
                _successMessage.value =
                    if (isStaff) "The appointment for $patientName with $doctorName is confirmed for $formattedDate, at $formattedTime."
                    else "Your appointment with $doctorName is confirmed for $formattedDate, at $formattedTime."
                _showSuccessPopup.value = true
            } else {
                // Handle booking failure if needed (e.g., show an error message)
            }
        }
    }

    fun dismissSuccessPopup() {
        _showSuccessPopup.value = false
        //TODO: Back to home page
    }
}
