# Accordeur guitare classique (Android, Kotlin)

Application Android en Kotlin + Jetpack Compose qui écoute le micro, détecte la
hauteur de la note jouée et indique l'écart en cents par rapport à la corde visée
(accordage standard, La = 440 Hz) : E2 82,41 · A2 110 · D3 146,83 · G3 196 · B3 246,94 · E4 329,63 Hz.

## Ouvrir et lancer
1. Ouvrir le dossier `GuitarTuner` dans Android Studio (Koala ou plus récent).
2. Laisser Gradle synchroniser (AGP 8.5, Kotlin 2.0, compileSdk 34, minSdk 24).
3. Brancher un téléphone (ou un émulateur avec micro) et cliquer sur *Run*.
4. Accepter la permission micro, puis pincer une corde.

## Fonctionnement
- `audio/TunerEngine.kt` : lecture du micro avec `AudioRecord` (44,1 kHz, mono, blocs de 4096 échantillons recouverts à 50 %), seuil de silence.
- `audio/YinPitchDetector.kt` : algorithme YIN avec interpolation parabolique (précision < 1 cent sur signal de test).
- `audio/GuitarString.kt` : les six cordes, calcul des cents, nom de la note.
- `TunerViewModel.kt` : lissage par médiane glissante, mode automatique ou corde imposée.
- `ui/TunerScreen.kt` : grande note, cadran à aiguille ±50 cents (vert à ±5 cents), consigne « tendez / détendez », sélecteur de corde.

## Tests
`./gradlew test` lance les tests unitaires de l'algorithme (`app/src/test`).

## Release
Chaque tag `v*` (ex. `v1.0`), ou un lancement manuel du workflow avec une version, déclenche le workflow `.github/workflows/release.yml`,
qui lance les tests, compile l'APK et le publie dans la release GitHub correspondante.
