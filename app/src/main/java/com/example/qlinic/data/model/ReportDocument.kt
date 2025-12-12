package com.example.qlinic.data.model

data class ReportDocument(
    var filters: ReportFilterState = ReportFilterState(),
    var weeklyStats: AppointmentStatistics = AppointmentStatistics(),
    var monthlyStats: AppointmentStatistics = AppointmentStatistics(),
    var yearlyStats: AppointmentStatistics = AppointmentStatistics(),

    var weeklyPeakHours: PeakHoursReportData = PeakHoursReportData(),
    var monthlyPeakHours: PeakHoursReportData = PeakHoursReportData(),
    var yearlyPeakHours: PeakHoursReportData = PeakHoursReportData()
) {
    // The empty constructor must also be updated
    constructor() : this(
        ReportFilterState(),
        AppointmentStatistics(),
        AppointmentStatistics(),
        AppointmentStatistics(),
        PeakHoursReportData(), // for weeklyPeakHours
        PeakHoursReportData(), // for monthlyPeakHours
        PeakHoursReportData()  // for yearlyPeakHours

    )
}