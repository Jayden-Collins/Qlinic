package com.example.qlinic.data.model

object TestUsers {
    val patient = CurrentUserInfo(
        id = "X2tLJZX7bTLPoZZSMnjY",
        name = "Johnny Smith",
        role = UserRole.PATIENT
    )

    val doctor = CurrentUserInfo(
        id = "S008",
        name = "Dr. David Patel",
        role = UserRole.DOCTOR
    )

    val staff = CurrentUserInfo(
        id = "S005",
        name = "Susan Tan",
        role = UserRole.STAFF
    )

    val current = patient
}
