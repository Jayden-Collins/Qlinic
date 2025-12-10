package com.example.qlinic.data.model

data class ReportFilterState(
    var selectedType: String = "",
    var selectedDepartment: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var isCustomRangeVisible: Boolean = false
) {
    constructor() : this("", "", "", "", false)
}
