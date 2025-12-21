package com.example.qlinic.data.model

data class ReportDocument(
    var filters: ReportFilterState = ReportFilterState(),
    var weeklyStats: Map<String, AppointmentStatistics> = emptyMap(),
    var monthlyStats: Map<String, AppointmentStatistics> = emptyMap(),
    var yearlyStats: Map<String, AppointmentStatistics> = emptyMap(),

    // The same new structure applies to Peak Hours reports.
    var weeklyPeakHours: Map<String, PeakHoursReportData> = emptyMap(),
    var monthlyPeakHours: Map<String, PeakHoursReportData> = emptyMap(),
    var yearlyPeakHours: Map<String, PeakHoursReportData> = emptyMap()
) {
    // The empty constructor must also be updated
    constructor() : this(
        ReportFilterState(),
        emptyMap(),
        emptyMap(),
        emptyMap(),
        emptyMap(),
        emptyMap()
    )
}