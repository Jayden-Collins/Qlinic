package com.example.qlinic.data.model

data class ReportDocument(
    var filters: ReportFilterState = ReportFilterState(),
    var weeklyStats: AppointmentStatistics = AppointmentStatistics(),
    var monthlyStats: AppointmentStatistics = AppointmentStatistics(),
    var yearlyStats: AppointmentStatistics = AppointmentStatistics()
) {
    constructor() : this(ReportFilterState(), AppointmentStatistics(), AppointmentStatistics(), AppointmentStatistics())
}