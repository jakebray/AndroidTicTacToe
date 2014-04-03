AndroidTicTacToe
================
Android Networked Tic-Tac-Toe Game for 2 players

Protocol Design
---------------
When each client connects, they send a FIND request to the server, which either:
  (a) places the client in an empty group and has it wait until it is joined by  another, or
  (b) places the client in a group that already has one client waiting in it.

From here, each client in the group generates a random integer value, and sends it to the other. They each compare the values, and the client with the higher value goes first.

They each take turns by sending the info of whichever button was clicked during their turn, and waiting to receive the info regarding whichever button their opponent clicked during their opponent's turn. After each turn passes, a check is made to determine whether the game has been won, lost, or tied. When any of these states have been reached, a message is displayed, and the game is over.
