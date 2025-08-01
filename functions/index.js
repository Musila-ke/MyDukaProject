const {initializeApp} = require("firebase-admin/app");
const {getMessaging} = require("firebase-admin/messaging");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {onDocumentCreated, onDocumentDeleted} =
require("firebase-functions/v2/firestore",
);
const {logger} = require("firebase-functions");
const {onSchedule} = require("firebase-functions/v2/scheduler");

initializeApp();
const db = getFirestore();

// 🔔 Notify admin when new worker is added
exports.notifyNewWorker = onDocumentCreated(
    "users/{userId}/workers/{workerId}",
    (event) => {
      const snap = event.data;
      if (!snap) {
        logger.error("No data in snapshot");
        return null;
      }

      const workerData = snap.data();
      const {workerName, branchName} = workerData;

      const message = {
        topic: "admin-notifications",
        notification: {
          title: "New Worker Added",
          body: `${workerName} was added to ${branchName}`,
        },
        data: {
          workerName,
          branchName,
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
        android: {priority: "high"},
      };

      return getMessaging()
          .send(message)
          .then(() => logger.info("New worker notification sent:", workerName))
          .catch((error) => logger.error("Notification error:", error));
    },
);

// 🔕 Unsubscribe FCM token from topic when user uninstalls the app
exports.cleanupFCMToken = onDocumentDeleted(
    "fcmTokens/{tokenId}",
    (event) => {
      const tokenId = event.params.tokenId;

      return getMessaging()
          .unsubscribeFromTopic(tokenId,
              "admin-notifications")
          .then(() =>
            logger.info(`Unsubscribed token ${tokenId}
            from admin-notifications`),
          )
          .catch((error) =>
            logger.error(`Failed to unsubscribe token ${tokenId}:`, error),
          );
    },
);

// ⏰ Daily Expiry Checker
exports.checkExpiredProducts = onSchedule(
    {schedule: "every 24 hours", timeZone: "Africa/Nairobi"},
    async () => {
      const now = new Date();
      const usersSnap = await db.collection("users").get();

      for (const userDoc of usersSnap.docs) {
        const uid = userDoc.id;

        const branchesSnap = await db
            .collection("users").doc(uid)
            .collection("branches")
            .get();

        for (const branchDoc of branchesSnap.docs) {
          const branchId = branchDoc.id;
          const branchName = branchDoc.get("name") || "Unnamed Branch";

          const productsSnap = await db
              .collection("users").doc(uid)
              .collection("branches").doc(branchId)
              .collection("branchproducts")
              .get();

          for (const productDoc of productsSnap.docs) {
            const productId = productDoc.id;
            const productName = productDoc.get("name") || "Unnamed Product";

            const unitsSnap = await db
                .collection("users").doc(uid)
                .collection("branches").doc(branchId)
                .collection("branchproducts").doc(productId)
                .collection("units")
                .get();

            const expiredUnits = unitsSnap.docs
                .map((doc) => {
                  const exp = doc.get("expiryDate");
                  return exp ? {
                    barcode: doc.get("barcode") || "N/A",
                    expiry: exp.toDate(),
                  } : null;
                })
                .filter((unit) => unit && unit.expiry < now);

            if (expiredUnits.length === 0) continue;
            const count = expiredUnits.length;
            const barcodes = expiredUnits.map((u) => u.barcode).join(", ");
            const title = `${count} unit${count > 1 ? "s" : ""}
            from ${productName} in ${branchName} expired`;
            const message = `Barcodes: ${barcodes}`;

            const notifRef = db
                .collection("users").doc(uid)
                .collection("notifications")
                .doc();

            const notifData = {
              id: notifRef.id,
              type: "expiry",
              title,
              message,
              timestamp: FieldValue.serverTimestamp(),
              seen: false,
            };
            // Push notification
            await notifRef.set(notifData);

            // Update branchproduct to mark it has expired units
            await db
                .collection("users").doc(uid)
                .collection("branches").doc(branchId)
                .collection("branchproducts").doc(productId)
                .update({hasExpiredUnits: true});

            // Push notification
            await getMessaging().send({
              topic: "admin-notifications",
              notification: {title, body: message},
              data: {
                type: "expiry",
                productId,
                branchId,
                notificationId: notifRef.id,
                clickAction: "FLUTTER_NOTIFICATION_CLICK",
              },
              android: {
                priority: "high",
              },
            });

            logger.info(`✅ Expired notification sent for ${productName} in
            ${branchName}`);
          }
        }
      }
      return null;
    },
);
