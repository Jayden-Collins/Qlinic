package com.example.qlinic.data.repository

import androidx.compose.ui.graphics.Color
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole

class MockAppointmentRepository : AppointmentRepository {

    // Dummy User for Testing
    private val doctorRobinson =
        User("d1", "Dr. James Robinson", UserRole.DOCTOR, "Orthopedic Surgery")
    private val doctorJohnson = User("d2", "Dr. Sarah Johnson", UserRole.DOCTOR, "Gynecologist")
    private val doctorLee = User("d3", "Dr. Daniel Lee", UserRole.DOCTOR, "Gastroenterology")

    private val patientMonica = User("v9i0pTJ4KtKUJ77SQwfg", "Monica Cheng", UserRole.PATIENT, "Female, 40 y/o")

    // Another patient for testing staff view
    private val patientJane = User("p2", "Jane Doe", UserRole.PATIENT, "Female, 28 y/o")

    // Dummy Appointment for Testing
    private val allAppointments = listOf(
        Appointment(
            id = "1",
            dateTime = "Wed, May 22, 2025 - 10.00 AM",
            doctor = doctorRobinson,
            patient = patientMonica,
            locationOrRoom = "R001",
            status = AppointmentStatus.UPCOMING
        ),
        Appointment(
            id = "2",
            dateTime = "Mon, March 12, 2025 - 11.00 AM",
            doctor = doctorJohnson,
            patient = patientMonica,
            locationOrRoom = "R003",
            status = AppointmentStatus.COMPLETED
        ),
        // Canceled Appointment (Matches Screenshot 3)
        Appointment(
            id = "3",
            dateTime = "Wed, May 14, 2025 - 3.00 PM",
            doctor = doctorLee,
            patient = patientMonica,
            locationOrRoom = "R002",
            status = AppointmentStatus.CANCELLED
        ),
        Appointment(
            id = "4",
            dateTime = "Fri, May 24, 2025 - 09.00 AM",
            doctor = doctorRobinson,
            patient = patientJane,
            locationOrRoom = "R004",
            status = AppointmentStatus.UPCOMING
        )
    )

    // Used by the PATIENT home screen to switch tabs
    override suspend fun getAppointmentsForPatient(
        patientId: String,
        status: AppointmentStatus
    ): List<Appointment> {
        return allAppointments.filter {
            it.patient.id == patientId && it.status == status
        }
    }

    // Used by the STAFF home screen to see everything
    override suspend fun getAllUpcomingAppointments(): List<Appointment> {
        return allAppointments.filter {
            it.status == AppointmentStatus.UPCOMING
        }
    }

    // Used by the DOCTOR home screen to see their own schedule
    override suspend fun getAppointmentsForDoctor(doctorId: String): List<Appointment> {
        return allAppointments.filter {
            it.doctor.id == doctorId && it.status == AppointmentStatus.UPCOMING
        }
    }
}