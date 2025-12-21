package com.example.qlinic.data.model

data class ReportFilterState(
    var selectedType: String = "Weekly",
    var selectedDepartment: String = "All Department",
    var startDate: String = "",
    var endDate: String = "",
    var isCustomRangeVisible: Boolean = false
) {
    constructor() : this("Weekly", "All Department", "", "", false)
}
