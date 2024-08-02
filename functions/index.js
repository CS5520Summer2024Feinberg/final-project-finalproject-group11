/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const functions = require("firebase-functions");
admin.initializeApp();

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

exports.helloWorld = onRequest((request, response) => {
  logger.info("Hello logs!", {structuredData: true});
  response.send("Hello from Firebase!");
});

exports.deleteEmptyRoom =
    functions.database.ref("/rooms/{roomId}/players/{playerId}/playerState")
        .onUpdate(async (change, context) => {
          const roomId = context.params.roomId;
          const playerState = change.after.val();

          if (playerState === "quit") {
            try {
              // Get all players in the room
              const roomSnapshot =
                    await admin.database().ref(`/rooms/${roomId}/players`)
                        .once("value");
              const players = roomSnapshot.val();

              // Check if all players have surrendered
              const allSurrendered =
                  Object.values(players).
                      every((player) => player.playerState === "quit");


              if (allSurrendered) {
                // Delete the room if all players have surrendered
                await admin.database().ref(`/rooms/${roomId}`).remove();
                console.log(`Room ${roomId} deleted because all players quit.`);
              } else {
                console.log(`Room ${roomId} not deleted. Not all player quit.`);
              }
            } catch (error) {
              console.error(`Failed to check or delete room ${roomId}:`, error);
            }
          }

          return null;
        });

