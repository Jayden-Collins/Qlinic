package com.example.qlinic.data.model

import java.util.Date

// Added default values to all properties to ensure Firestore deserialization works.
data class Slot(
    var SlotID: String = "",
    var DoctorID: String = "",
    var StaffID: String = "",
    var DayOfWeek: String = "",
    var RecurStartDate: Date = Date(),
    var SlotStartTime: String = ""
)
