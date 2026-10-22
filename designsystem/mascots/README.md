# Mascots

Woolly (reading), Lumi (reflection) and Pip (gamification), five poses each, 200 x 200. The SVGs here are the project's `mascot/` drawings with their provenance metadata removed.

`import_mascots.py` turns them into `MascotVectors.kt` (Compose `ImageVector`s). Change an SVG, then run `python3 designsystem/mascots/import_mascots.py`; never edit the Kotlin by hand. Use them through `Mascot(character, pose)`.
