# Forbidden Island Game

A Java Swing-based desktop version of the "Forbidden Island" board game.

## Game Play

*   **The Main Objective:** Players must work together as a team of adventurers to collect four sacred treasures (The Earth Stone, The Statue of the Wind, The Crystal of Fire, and The Ocean's Chalice) from the sinking island. After successfully capturing all four treasures, the entire team must make their way to the "Fools' Landing" tile and use a "Helicopter Lift" card to escape the island before it sinks completely.

*   **Player Turns:** On a player's turn, they perform the following three phases in order:
    1.  **Take up to 3 actions.** Players can choose from Move, Shore Up, Give a Treasure Card, or Capture a Treasure. They can perform any combination of these actions, or fewer than 3 actions.
    2.  **Draw 2 Treasure cards.** Add these to your hand (face up). If a "Waters Rise!" card is drawn, resolve it immediately (it doesn't go into your hand).
    3.  **Draw Flood cards.** Draw a number of Flood cards equal to the current water level indicated on the Water Meter. For each Flood card drawn, the corresponding island tile is either flooded (if unflooded) or sinks (if already flooded).

*   **Key Actions:**
    *   **Move (1 action):** Move your pawn to an adjacent (up, down, left, or right) island tile. Some special roles have enhanced movement abilities.
    *   **Shore Up (1 action):** Flip a flooded island tile (either the one your pawn is on, or an adjacent one) back to its unflooded side. Some special roles have enhanced shoring up abilities.
    *   **Give a Treasure Card (1 action per card):** If your pawn is on the same tile as another player's pawn, you can give them one or more Treasure cards (not Special Action cards) from your hand.
    *   **Capture a Treasure (1 action):** If your pawn is on one of the two designated tiles for a specific treasure AND you have 4 matching Treasure cards of that type in your hand, you can discard those 4 cards to capture the treasure. The treasure figurine is then taken by the player. The locations are:
        *   **The Earth Stone:** Can be captured at the *Temple of the Moon* or the *Temple of the Sun*.
        *   **The Statue of the Wind:** Can be captured at the *Whispering Garden* or the *Howling Garden*.
        *   **The Crystal of Fire:** Can be captured at the *Cave of Embers* or the *Cave of Shadows*.
        *   **The Ocean's Chalice:** Can be captured at the *Coral Palace* or the *Tidal Palace*.

*   **Special Roles (if any):** 
    *   **Pilot:** Once per turn, for 1 action, may fly to any tile on the island.
    *   **Engineer:** For 1 action, may shore up 2 tiles.
    *   **Explorer:** May move and/or shore up diagonally.
    *   **Messenger:** May give Treasure cards to another player anywhere on the island (pawns do not need to be on the same tile).
    *   **Navigator:** For 1 action, may move another player's pawn up to 2 adjacent tiles.
    *   **Diver:** May move through one or more adjacent flooded and/or missing (sunk) tiles for 1 action. Must end movement on an existing tile.

*   **Island Flooding and Sinking:**
    *   **Flooding:** When a Flood card is drawn for an unflooded tile, that tile is flipped to its "flooded" (blue and white) side. Pawns can still be on flooded tiles.
    *   **Sinking:** When a Flood card is drawn for an *already flooded* tile, that tile sinks and is removed from the game permanently. The corresponding Flood card is also removed. If a pawn is on a tile when it sinks, that player must immediately move their pawn to an adjacent (up, down, left, or right) tile that is still part of the island (even if flooded). If no such tile exists, the pawn sinks with the tile, and all players lose the game.

*   **Water Level:** The game starts at a set water level (difficulty). When a "Waters Rise!" card is drawn from the Treasure deck:
    1.  The Water Level marker on the Water Meter is moved up one tick mark. This may increase the number of Flood cards drawn at the end of each player's turn.
    2.  All cards in the Flood discard pile are shuffled and placed on top of the Flood draw pile.

*   **Winning Conditions:** All players win if they collectively achieve the following:
    1.  Capture all four treasures.
    2.  All player pawns are on the "Fools' Landing" tile.
    3.  Any player discards a "Helicopter Lift" card.

*   **Losing Conditions:** Players lose immediately if any of the following occurs:
    1.  The "Fools' Landing" tile sinks.
    2.  Both island tiles corresponding to a treasure that has not yet been collected sink.
    3.  A player is on an island tile that sinks, and there is no adjacent island tile for their pawn to move to.
    4.  The water level marker reaches the skull and crossbones symbol on the Water Meter.


## Project Structure

```
Forbidden_Island/
├── src/
│   ├── main/
│   │   ├── java/        # Java source code (e.g., game/panel/, game/simulation/)
│   │   └── resources/   # Game assets (e.g., Images/)
├── out/                 # Compiled .class files (or your chosen output directory)
└── README.md            # This file
```

## Important Notes

*   Ensure that image files and other resources are located in the `src/main/resources` directory. 