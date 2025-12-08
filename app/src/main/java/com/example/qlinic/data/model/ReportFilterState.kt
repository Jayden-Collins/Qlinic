package com.example.qlinic.data.model

data class ReportFilterState(
    val selectedType: String = "",
    var selectedDepartment: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCustomRangeVisible: Boolean = false
) {
    constructor() : this("", "", "", "", false)
}
