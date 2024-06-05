package com.opsc.timeriseprojectmain

import java.util.Date

data class TimesheetEntry(
    var id: Int = 0,
    var date: Date = Date(),
    var startTime: String = "",
    var endTime: String = "",
    var description: String = "",
    var category: Category = Category(),
    var imageUrl: String? = null
)
