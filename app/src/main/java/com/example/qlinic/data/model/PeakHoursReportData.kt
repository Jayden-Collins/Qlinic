package com.example.qlinic.data.model

data class PeakHoursReportData(
    val chartData: List<ChartData> = emptyList(),
    val busiestDay: String = "No Data",
    val busiestTime: String = "No Data",
    val busiestCategoryLabel: String = "Busiest Day"
)
