# CS5520 Summer Full 2024 Group 11 Final Project

## Description:
    

## Contributor: 
- Da-En Yu(Github: kevinlego2009): 
- Yi-hsuan Lai(Github: DannyLLL): 
- Qichen Wang(GitHub:ZAcoooo): 

---
https://github.com/CS5520Summer2024Feinberg/final-project-finalproject-group11

---

#### Unfix bugs and issues:
- ~~Duplicate room number might be generated.~~
- Dialogs in all activities should be handled correctly if user dismisses it. 
- Users can press "READY" without deployment. 
- Users can go back without pressing "QUIT" in deployment stage.
- When a user quit the deployment stage, he's opponent should be forced to quit with deletion of the room.
- When the winner presses "OK" to head back to MainActivity before the loser, firebase automatically re-generates the room after deletion.
- When a third user tries to enter a full room, player 2 will be logged out forcefully.
- Room will not be deleted if the "QUIT" button is not pressed from the create room dialog.
- ~~When two users create room, when one of them cancel the action then join a room, the other one only enter by himself.~~
- During battle stage, one of the device has same UID and opponentID.


#### Undone feature:
- During deployment and battle stage, actions should be done automatically when time's up.
- Log-in issue.
