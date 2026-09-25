# Arena Challenges

# Arena Challenges

Arena Challenges adds generated, bounded combat arenas to the overworld. Each site has a central
Arena Totem, three shared one-time rewards, player duels, curated solo encounters, and a replay
gallery. Trials provide a solo route to the same reward stock. Duel and trial kits are loaned for
the match and the player's inventory and experience are restored afterward.

The Champion's Blade and Warden's Axe are powerful one-of-a-kind weapons. The Duelist's Sigil
grants Strength II for 30 seconds and recharges after five minutes. A victory earns the right to
claim one reward still available at that arena; replay viewing does not grant rewards.

Arena Challenges stores duel replays through Player Traces in separate append-only files under
`data/player_traces/arena_duels/`, keyed by dimension and arena position. Recordings store motion
only. Player names appear only when both duelists enabled `/arena consent-names` before the duel.
Replays are visual echoes and cannot attack or deal damage.

Player Traces is a required runtime dependency. The arena structure generates in broad, dry
overworld biomes with a gentle slope. Each ring is about 25 blocks across. Players inside the ring
can invite a nearby player or begin one of three solo trials from the totem.
