/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Set a global limit on the number of function instances.
setGlobalOptions({ maxInstances: 10 });

/**
 * A scheduled function that runs every 30 minutes to send reminders
 * for appointments occurring in the next 24 hours.
 */
exports.sendAppointmentReminders = onSchedule("every 30 minutes", async (event) => {
  logger.info("Running sendAppointmentReminders function.");

  const now = admin.firestore.Timestamp.now();
  const twentyFourHoursFromNow = admin.firestore.Timestamp.fromMillis(now.toMillis() + 24 * 60 * 60 * 1000);

  const appointmentsRef = db.collection("Appointment");
  const query = appointmentsRef
    .where("isNotifSent", "==", false)
    .where("appointmentDate", ">=", now)
    .where("appointmentDate", "<=", twentyFourHoursFromNow);

  const snapshot = await query.get();

  if (snapshot.empty) {
    logger.info("No upcoming appointments to send reminders for.");
    return;
  }

  const reminderPromises = snapshot.docs.map(async (doc) => {
    const appointment = doc.data();
    const { patientId, appointmentDate, slotId } = appointment;

    if (!patientId || !appointmentDate || !slotId) {
      logger.error(`Appointment ${doc.id} is missing required fields.`);
      return;
    }

    try {
      const patientDoc = await db.collection("Patient").doc(patientId).get();
      if (!patientDoc.exists) throw new Error(`Patient document ${patientId} not found.`);
      const fcmToken = patientDoc.data().fcmToken;

      const slotDoc = await db.collection("Slot").doc(slotId).get();
      if (!slotDoc.exists) throw new Error(`Slot ${slotId} not found.`);
      const doctorId = slotDoc.data().DoctorID;
      if (!doctorId) throw new Error(`Slot ${slotId} is missing a doctorId.`);

      const staffDoc = await db.collection("ClinicStaff").doc(doctorId).get();
      if (!staffDoc.exists) throw new Error(`Doctor ${doctorId} not found.`);
      const { FirstName, LastName } = staffDoc.data();
      const doctorName = `Dr. ${FirstName} ${LastName}`;

      const title = "Appointment Reminder";
      const body = `Your appointment with ${doctorName} is on ${formatTimestamp(appointmentDate)}.`;

      const promises = [];

      const notificationRef = db.collection("Patient").doc(patientId).collection("notifications").doc();
      promises.push(notificationRef.set({
        notifId: notificationRef.id,
        title: title,
        body: body,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        type: "APPOINTMENT_REMINDER",
        appointmentId: doc.id,
      }));

      promises.push(doc.ref.update({ isNotifSent: true }));

      if (fcmToken) {
        const message = { notification: { title, body }, token: fcmToken };
        const pushPromise = admin.messaging().send(message)
          .then((response) => {
            logger.info(`--> Successfully sent push notification for appointment ${doc.id}. Message ID: ${response}`);
          })
          .catch((error) => {
            logger.error(`--> Error sending push notification for appointment ${doc.id}:`, error);
          });
        promises.push(pushPromise);
      } else {
        logger.warn(`Patient ${patientId} does not have an FCM token. Skipping push notification.`);
      }

      await Promise.all(promises);
      logger.info(`Reminder processing complete for appointment ${doc.id}`);

    } catch (error) {
      logger.error(`Failed to process reminder for appointment ${doc.id}:`, error);
    }
  });

  await Promise.all(reminderPromises);
  logger.info(`Finished processing all potential reminders.`);
});

/**
 * Notifies a doctor when a new appointment is created for them.
 */
exports.notifyDoctorOnCreate = onDocumentCreated("Appointment/{appointmentId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        logger.log("No data associated with the event");
        return;
    }
    const appointment = snapshot.data();
    const { slotId, patientId, appointmentDate } = appointment;

    logger.info(`New appointment ${snapshot.id} created. Processing for doctor notification.`);

    try {
        const slotDoc = await db.collection("Slot").doc(slotId).get();
        if (!slotDoc.exists) throw new Error(`Slot ${slotId} not found.`);
        const doctorId = slotDoc.data().DoctorID;
        if (!doctorId) throw new Error(`Slot ${slotId} is missing a DoctorID.`);

        const doctorDoc = await db.collection("Doctor").doc(doctorId).get();
        if (!doctorDoc.exists) throw new Error(`Doctor ${doctorId} not found.`);
        const fcmToken = doctorDoc.data().fcmToken;

        const patientDoc = await db.collection("Patient").doc(patientId).get();
        if (!patientDoc.exists) throw new Error(`Patient ${patientId} not found.`);
        const patientName = `${patientDoc.data().FirstName} ${patientDoc.data().LastName}`;

        const title = "New Appointment Booked";
        const body = `A new appointment has been booked with ${patientName} at ${formatTimestamp(appointmentDate)}.`;

        const promises = [];

        const notifRef = db.collection("Doctor").doc(doctorId).collection("notifications").doc();
        promises.push(notifRef.set({
            notifId: notifRef.id,
            title: title,
            body: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: "APPOINTMENT_CREATED",
            appointmentId: snapshot.id,
        }));

        if (fcmToken) {
            const message = { notification: { title, body }, token: fcmToken };
            promises.push(admin.messaging().send(message).then(response => {
                logger.info(`--> Successfully sent 'create' push notification to Dr. ${doctorId}.`);
            }).catch(error => {
                logger.error(`--> Error sending 'create' push notification to Dr. ${doctorId}:`, error);
            }));
        } else {
            logger.warn(`Doctor ${doctorId} does not have an FCM token.`);
        }

        await Promise.all(promises);
        logger.info(`Successfully processed 'create' notification for Dr. ${doctorId} for appt ${snapshot.id}.`);

    } catch (error) {
        logger.error(`Failed to process 'create' notification for appt ${snapshot.id}:`, error);
    }
});

/**
 * Notifies a doctor if an appointment is rescheduled or cancelled.
 */
exports.notifyDoctorOnUpdate = onDocumentUpdated("Appointment/{appointmentId}", async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    const appointmentId = event.params.appointmentId;

    logger.info(`Appointment ${appointmentId} updated. Checking for notifiable changes.`);

    let title = "";
    let body = "";

    const patientDoc = await db.collection("Patient").doc(before.patientId).get();
    const patientName = patientDoc.exists ? `${patientDoc.data().FirstName} ${patientDoc.data().LastName}` : "A patient";

    if (before.status !== "Cancelled" && after.status === "Cancelled") {
        logger.info(`Appointment ${appointmentId} status changed to Cancelled.`);
        title = "Appointment Cancelled";
        body = `Your appointment with ${patientName} at ${formatTimestamp(before.appointmentDate)} has been cancelled.`;
    } else if (!before.appointmentDate.isEqual(after.appointmentDate)) {
        logger.info(`Appointment ${appointmentId} has been rescheduled.`);
        title = "Appointment Rescheduled";
        body = `Your appointment with ${patientName} at ${formatTimestamp(before.appointmentDate)} has been rescheduled to ${formatTimestamp(after.appointmentDate)}.`;
    } else {
        logger.info(`Update on appointment ${appointmentId} did not require a doctor notification.`);
        return;
    }

    try {
        const slotDoc = await db.collection("Slot").doc(before.slotId).get();
        if (!slotDoc.exists) throw new Error(`Original slot ${before.slotId} not found.`);
        const doctorId = slotDoc.data().DoctorID;
        if (!doctorId) throw new Error(`Slot ${before.slotId} is missing a DoctorID.`);

        const doctorDoc = await db.collection("Doctor").doc(doctorId).get();
        if (!doctorDoc.exists) throw new Error(`Doctor ${doctorId} not found.`);
        const fcmToken = doctorDoc.data().fcmToken;

        const promises = [];

        const notifRef = db.collection("Doctor").doc(doctorId).collection("notifications").doc();
        promises.push(notifRef.set({
            notifId: notifRef.id,
            title: title,
            body: body,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: title.includes("Cancelled") ? "APPOINTMENT_CANCELLED" : "APPOINTMENT_RESCHEDULED",
            appointmentId: appointmentId,
        }));

        if (fcmToken) {
            const message = { notification: { title, body }, token: fcmToken };
            promises.push(admin.messaging().send(message).then(response => {
                logger.info(`--> Successfully sent 'update' push notification to Dr. ${doctorId}.`);
            }).catch(error => {
                logger.error(`--> Error sending 'update' push notification to Dr. ${doctorId}:`, error);
            }));
        } else {
            logger.warn(`Doctor ${doctorId} does not have an FCM token for 'update' notification.`);
        }

        await Promise.all(promises);
        logger.info(`Successfully processed 'update' notification for Dr. ${doctorId} for appointment ${appointmentId}.`);

    } catch (error) {
        logger.error(`Failed to process 'update' notification for appointment ${appointmentId}:`, error);
    }
});

function formatTimestamp(timestamp) {
    const timeZone = "Asia/Singapore";
    const date = timestamp.toDate();

    const d = parseInt(new Intl.DateTimeFormat('en-GB', { day: 'numeric', timeZone }).format(date));
    const m = new Intl.DateTimeFormat('en-GB', { month: 'short', timeZone }).format(date);
    const y = new Intl.DateTimeFormat('en-GB', { year: 'numeric', timeZone }).format(date);

    const getOrdinal = (n) => {
        if (n > 3 && n < 21) return 'th';
        switch (n % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    };

    const dateString = `${d}${getOrdinal(d)} ${m} ${y}`;
    const timeString = date.toLocaleTimeString("en-US", {
        timeZone,
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });

    return `${dateString} at ${timeString}`;
}