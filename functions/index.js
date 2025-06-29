const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");

initializeApp();

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
    const { workerName, branchName } = workerData;

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
      android: {
        priority: "high",
      },
    };

    return getMessaging()
      .send(message)
      .then(() => logger.info("New worker notification sent:", workerName))
      .catch((error) => logger.error("Notification error:", error));
  }
);

// 🔕 Unsubscribe FCM token from topic when user uninstalls the app
exports.cleanupFCMToken = onDocumentDeleted(
  "fcmTokens/{tokenId}",
  (event) => {
    const tokenId = event.params.tokenId;

    return getMessaging()
      .unsubscribeFromTopic(tokenId, "admin-notifications")
      .then(() =>
        logger.info(`Unsubscribed token ${tokenId} from admin-notifications`)
      )
      .catch((error) =>
        logger.error(`Failed to unsubscribe token ${tokenId}:`, error)
      );
  }
);
