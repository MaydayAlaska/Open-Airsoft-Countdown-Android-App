<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="app/src/main/res/drawable-nodpi/info_logo_dark.png">
  <source media="(prefers-color-scheme: light)" srcset="app/src/main/res/drawable-nodpi/info_logo_light.png">
  <img src="app/src/main/res/drawable-nodpi/info_logo_light.png" alt="Open Airsoft Countdown" width="180">
</picture>

# Open Airsoft Countdown — Android App

Applicazione Android per controllare tramite Bluetooth Low Energy il dispositivo Open Airsoft Countdown.
Android application for controlling the Open Airsoft Countdown device over Bluetooth Low Energy.

**Versione corrente / Current version: v1.30**

[Italiano](#italiano) · [English](#english)

</div>

---

# Italiano

## Descrizione

L'app ufficiale del progetto [Open Airsoft Countdown](https://github.com/MaydayAlaska/Open-Airsoft-Countdown) collega un telefono Android al controller ESP32-S3 tramite BLE. Riunisce in un'unica interfaccia il controllo del timer, l'autenticazione amministrativa, la configurazione e la gestione degli utenti.

È progettata per il firmware corrente v1.9.1 e per le versioni che mantengono lo stesso protocollo BLE.

> Il progetto è destinato esclusivamente all'intrattenimento e non va usato con esplosivi o sistemi reali di detonazione.

## Funzionalità

- scansione dei dispositivi BLE compatibili, connessione e disconnessione;
- login amministrativo con PIN a 6 cifre e sincronizzazione automatica di configurazione, utenti e stato;
- elenco dei dispositivi già autenticati, identificati da nome e MAC;
- salvataggio locale del PIN dei dispositivi autenticati, protetto con una chiave nell'Android Keystore e cifratura AES-GCM;
- invio automatico del PIN salvato alla riconnessione; se non è più valido, richiesta del nuovo PIN;
- schermata timer con stato in tempo reale, errori, avvio, pausa, stop e reset;
- selettori di ore, minuti e secondi per impostare il tempo senza perdere modifiche non ancora salvate;
- lettura e modifica di `config.json`, inclusi PIN amministratore, nome BLE, lingua del dispositivo, suono, RFID/NFC, errori e penalità;
- gestione completa di `users.json`: elenco, aggiunta, modifica ed eliminazione utenti;
- controlli lato app per PIN di 6 cifre, UID NFC esadecimale e lista degli utenti autorizzati;
- navigazione a sezioni: Comandi, Dispositivo, Configurazione, Utenti e Impostazioni;
- tema chiaro, scuro o di sistema e interfaccia dell'app in italiano o inglese.

## Come si usa

1. Attiva Bluetooth e concedi le autorizzazioni richieste da Android.
2. Apri **Dispositivo**, tocca **Scansiona dispositivo** e seleziona il controller trovato.
3. Al primo collegamento inserisci il PIN amministratore. Dopo un login riuscito, l'app legge automaticamente configurazione e utenti e memorizza il dispositivo in modo sicuro.
4. Da **Comandi** imposta ore, minuti e secondi, salva la durata e usa Start, Pausa, Stop o Reset secondo lo stato corrente del timer.
5. Da **Configurazione** o **Utenti** modifica i dati sul controller. Il firmware può richiedere il riavvio dopo il salvataggio della configurazione per applicare tutti i cambiamenti.

I comandi amministrativi restano disabilitati finché connessione e login non sono pronti. Se il dispositivo si disconnette, la sessione viene chiusa.

## Utenti autorizzati

Il campo `authorizedUserIds` definisce chi può disarmare il timer:

```text
1;2;3
```

uno qualunque degli utenti indicati è sufficiente.

```text
1,2,3
```

tutti gli utenti indicati devono autenticarsi, in qualsiasi ordine; questa modalità accetta fino a quattro utenti. Non mescolare virgole e punti e virgola. Ogni ID deve esistere sul dispositivo.

Quando l'RFID/NFC è abilitato nel firmware, la tessera e il PIN devono appartenere allo stesso utente.

## Requisiti

- Android 6.0 o successivo (API 23+);
- telefono o tablet con Bluetooth Low Energy;
- autorizzazioni Bluetooth richieste dalla versione di Android;
- firmware Open Airsoft Countdown compatibile (consigliato v1.9.1).

## Compilazione

### Android Studio

1. Clona o scarica la repository.
2. Apri la cartella del progetto con Android Studio.
3. Attendi la sincronizzazione Gradle.
4. Seleziona **Build > Build APK(s)**.

### Gradle Wrapper

Su Windows:

```powershell
.\gradlew.bat assembleDebug
```

Su Linux o macOS:

```bash
./gradlew assembleDebug
```

Il progetto richiede JDK 17. L'APK debug è prodotto in `app/build/outputs/apk/debug/`.

## Repository collegate

- [Firmware ESP32 e Web Installer](https://github.com/MaydayAlaska/Open-Airsoft-Countdown)
- [Applicazione Android](https://github.com/MaydayAlaska/Open-Airsoft-Countdown-Android-App)

## Segnalazioni, contributi e licenza

Per una Issue, indica versione dell'app e del firmware, modello/versione Android, passaggi per riprodurre il problema e, se possibile, screenshot o log. Pull Request e contributi sono benvenuti.

Il progetto è distribuito con licenza [GNU Affero General Public License v3.0](LICENSE).

Puoi sostenere il progetto con una Star su GitHub, un Like/Save/Boost su [MakerWorld](https://makerworld.com/it/@maydayalaska) o una [donazione PayPal](https://paypal.me/lorisgennarini).

---

# English

## Description

The official [Open Airsoft Countdown](https://github.com/MaydayAlaska/Open-Airsoft-Countdown) app connects an Android phone to the ESP32-S3 controller over BLE. It brings timer control, administrator authentication, configuration, and user management together in one interface.

It is designed for current firmware v1.9.1 and versions that keep the same BLE protocol.

> This project is intended exclusively for entertainment and must not be used with explosives or real-world detonation systems.

## Features

- scan, connect to, and disconnect from compatible BLE devices;
- 6-digit administrator-PIN login with automatic configuration, user, and status synchronization;
- list of previously authenticated devices, identified by name and MAC address;
- local storage of authenticated-device PINs, protected by an Android Keystore key and AES-GCM encryption;
- automatic submission of the saved PIN when reconnecting; if it is no longer valid, a new PIN is requested;
- live timer screen with status, errors, start, pause, stop, and reset;
- hour, minute, and second pickers for setting the duration without losing unsaved edits;
- read and edit `config.json`, including administrator PIN, BLE name, device language, sound, RFID/NFC, errors, and penalty;
- complete `users.json` management: list, add, edit, and delete users;
- app-side validation for 6-digit PINs, hexadecimal NFC UIDs, and authorized-user lists;
- section-based navigation: Controls, Device, Configuration, Users, and Settings;
- light, dark, or system theme, plus an Italian or English app interface.

## How to use it

1. Enable Bluetooth and grant the permissions required by Android.
2. Open **Device**, tap **Scan for device**, then select the discovered controller.
3. On the first connection, enter the administrator PIN. After a successful login, the app automatically loads configuration and users and securely saves the device.
4. From **Controls**, set hours, minutes, and seconds, save the duration, then use Start, Pause, Stop, or Reset according to the current timer state.
5. From **Configuration** or **Users**, edit the controller data. The firmware may request a restart after configuration is saved so every change takes effect.

Administrative controls remain disabled until the connection and login are ready. The session is closed when the device disconnects.

## Authorized users

The `authorizedUserIds` field controls who may disarm the timer:

```text
1;2;3
```

any one of the listed users is sufficient.

```text
1,2,3
```

all listed users must authenticate, in any order; this mode supports up to four users. Do not mix commas and semicolons. Every ID must exist on the device.

When RFID/NFC is enabled in the firmware, the card and PIN must belong to the same user.

## Requirements

- Android 6.0 or later (API 23+);
- phone or tablet with Bluetooth Low Energy;
- Bluetooth permissions required by the installed Android version;
- compatible Open Airsoft Countdown firmware (v1.9.1 recommended).

## Building

### Android Studio

1. Clone or download the repository.
2. Open the project folder in Android Studio.
3. Wait for Gradle synchronization.
4. Select **Build > Build APK(s)**.

### Gradle Wrapper

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew assembleDebug
```

The project requires JDK 17. The debug APK is written to `app/build/outputs/apk/debug/`.

## Related repositories

- [ESP32 firmware and Web Installer](https://github.com/MaydayAlaska/Open-Airsoft-Countdown)
- [Android application](https://github.com/MaydayAlaska/Open-Airsoft-Countdown-Android-App)

## Issues, contributions, and license

For an Issue, include app and firmware versions, Android model/version, reproduction steps, and screenshots or logs where possible. Pull Requests and contributions are welcome.

This project is released under the [GNU Affero General Public License v3.0](LICENSE).

You can support the project with a GitHub Star, a Like/Save/Boost on [MakerWorld](https://makerworld.com/it/@maydayalaska), or a [PayPal donation](https://paypal.me/lorisgennarini).
