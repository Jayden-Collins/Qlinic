package com.example.qlinic.data.model

data class ReportDocument(
    var filters: ReportFilterState = ReportFilterState(),
    var statistics: AppointmentStatistics = AppointmentStatistics()
) {
    constructor() : this(ReportFilterState(), AppointmentStatistics())
}