package com.example.qlinic.data.model
/**
 * File to set user for the app, for testing purpose
 **/
object TestUsers {
//    val patient = User(
//        id = "v9i0pTJ4KtKUJ77SQwfg",
//        name = "Monica Cheng",
//        role = UserRole.PATIENT,
//        details = "Female, 40 y/o"
//    )

    val patient = User(
        id = "X2tLJZX7bTLPoZZSMnjY",
        name = "Johnny smith",
        role = UserRole.PATIENT,
        details = "Male, 24 y/o"
    )

    val doctor = User(
        "S008",
        "Dr. David Patel",
        UserRole.DOCTOR,
        "Cardiology"
    )

    val staff = User(
        "S005",
        "Susan Tan",
        UserRole.STAFF,
        "Front_Desk"
    )
    val current = staff
}

