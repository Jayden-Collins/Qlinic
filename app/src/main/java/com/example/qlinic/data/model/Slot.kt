package com.example.qlinic.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Slot(
    var SlotID: String = "",
    var DoctorID: String = "",
    var StaffID: String = "",
    var DayOfWeek: String = "",
    var RecurStartDate: Date = Date(),
    var SlotStartTime: String = ""
)
