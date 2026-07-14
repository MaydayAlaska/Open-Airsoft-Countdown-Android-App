<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Open Airsoft Countdown" width="140">

# Open Airsoft Countdown — Android App

Applicazione Android per controllare tramite Bluetooth Low Energy il dispositivo **Open Airsoft Countdown**.

Android application for controlling the **Open Airsoft Countdown** device over Bluetooth Low Energy.

[Italiano](#italiano) · [English](#english)

</div>

---

# Italiano

## Descrizione

**Open Airsoft Countdown** è l'applicazione Android ufficiale del progetto open source Open Airsoft Countdown.

Permette di collegarsi tramite **Bluetooth Low Energy (BLE)** al dispositivo basato su ESP32-S3, controllare il timer, modificare la configurazione e gestire gli utenti autorizzati.

Il progetto è pensato per scenari come:

- softair e airsoft;
- laser tag;
- escape room;
- giochi a obiettivi;
- scenografie e prop interattivi.

> Questo progetto è un dispositivo di gioco e non è progettato per essere utilizzato con esplosivi o sistemi reali.

## Funzionalità

- ricerca e connessione ai dispositivi BLE compatibili;
- autenticazione amministratore;
- impostazione della durata nel formato `HHMMSS`;
- avvio, arresto e reset del timer;
- visualizzazione in tempo reale di tempo, stato ed errori;
- modifica del PIN amministratore e del nome BLE;
- selezione della lingua del firmware: italiano o inglese;
- attivazione o disattivazione di suono, RFID e impronta digitale;
- configurazione del numero massimo di errori;
- configurazione del countdown di penalità;
- gestione completa degli utenti;
- aggiunta, modifica ed eliminazione di PIN e UID NFC;
- configurazione degli utenti autorizzati al disarmo;
- tema chiaro, scuro o di sistema;
- interfaccia completamente disponibile in italiano e inglese.

## Utenti autorizzati

Il campo `authorizedUserIds` stabilisce quali utenti possono disattivare il timer.

Esempi:

```text
1;2;3
```

È sufficiente che si autentichi uno qualsiasi degli utenti indicati.

```text
1,2,3
```

Devono autenticarsi tutti gli utenti indicati, in qualsiasi ordine. In questa modalità sono consentiti al massimo quattro utenti.

Virgola e punto e virgola non possono essere utilizzati contemporaneamente.

## Requisiti

- Android 6.0 o successivo, API 23+;
- dispositivo Android con Bluetooth Low Energy;
- firmware Open Airsoft Countdown compatibile;
- autorizzazioni Bluetooth richieste da Android.

## Compilazione

### Android Studio

1. Clona o scarica questa repository.
2. Apri la cartella del progetto con Android Studio.
3. Attendi la sincronizzazione di Gradle.
4. Seleziona **Build > Build APK(s)**.

L'APK di debug verrà generato in:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Gradle Wrapper

Su Windows:

```powershell
.\gradlew.bat assembleDebug
```

Su Linux o macOS:

```bash
./gradlew assembleDebug
```

## Repository collegate

- **Applicazione Android:**  
  https://github.com/MaydayAlaska/Open-Airsoft-Countdown-Android-App

- **Firmware ESP32 e progetto principale:**  
  https://github.com/MaydayAlaska/Open-Airsoft-Countdown

## Segnalazione bug

Per segnalare malfunzionamenti, errori o proporre miglioramenti, utilizza la sezione **Issues** della repository interessata.

Quando apri una segnalazione, indica possibilmente:

- versione dell'app;
- versione del firmware;
- modello e versione Android del telefono;
- passaggi necessari per riprodurre il problema;
- eventuali log, screenshot o messaggi di errore.

## Contribuire

Contributi, correzioni e nuove funzionalità sono benvenuti.

Puoi:

1. creare un fork della repository;
2. creare un nuovo branch;
3. effettuare le modifiche;
4. aprire una Pull Request con una descrizione chiara.

## Supporta il progetto

Puoi sostenere Open Airsoft Countdown in diversi modi:

- metti una **Star** alle repository GitHub;
- lascia un **Like**, salva il progetto e usa un **Boost** su MakerWorld;
- segnala bug e suggerimenti tramite GitHub Issues;
- contribuisci con una donazione PayPal.

- MakerWorld: https://makerworld.com/it/@maydayalaska
- PayPal: https://paypal.me/lorisgennarini

## Licenza

Questo progetto è distribuito con licenza **GNU Affero General Public License v3.0**.

Consulta il file [LICENSE](LICENSE) per i dettagli.

---

# English

## Description

**Open Airsoft Countdown** is the official Android application for the open-source Open Airsoft Countdown project.

It connects through **Bluetooth Low Energy (BLE)** to the ESP32-S3 device and allows you to control the timer, edit the configuration, and manage authorized users.

The project is intended for scenarios such as:

- airsoft;
- laser tag;
- escape rooms;
- objective-based games;
- interactive props and scenery.

> This project is a game device and is not designed for use with explosives or real-world detonation systems.

## Features

- scan for and connect to compatible BLE devices;
- administrator authentication;
- timer duration input in `HHMMSS` format;
- start, stop, and reset controls;
- real-time display of remaining time, state, and errors;
- administrator PIN and BLE name configuration;
- firmware language selection: Italian or English;
- enable or disable sound, RFID, and fingerprint authentication;
- maximum error count configuration;
- penalty countdown configuration;
- complete user management;
- add, edit, and delete PINs and NFC UIDs;
- authorized disarm-user configuration;
- light, dark, or system theme;
- fully translated Italian and English interface.

## Authorized users

The `authorizedUserIds` field defines which users are allowed to disarm the timer.

Examples:

```text
1;2;3
```

Any one of the listed users may authenticate and disarm the timer.

```text
1,2,3
```

Every listed user must authenticate, in any order. A maximum of four users is supported in this mode.

Commas and semicolons cannot be used together.

## Requirements

- Android 6.0 or later, API 23+;
- an Android device with Bluetooth Low Energy;
- a compatible Open Airsoft Countdown firmware version;
- the Bluetooth permissions required by Android.

## Building

### Android Studio

1. Clone or download this repository.
2. Open the project folder in Android Studio.
3. Wait for Gradle synchronization to complete.
4. Select **Build > Build APK(s)**.

The debug APK will be generated in:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Gradle Wrapper

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew assembleDebug
```

## Related repositories

- **Android application:**  
  https://github.com/MaydayAlaska/Open-Airsoft-Countdown-Android-App

- **ESP32 firmware and main project:**  
  https://github.com/MaydayAlaska/Open-Airsoft-Countdown

## Reporting bugs

To report a malfunction, bug, or suggest an improvement, use the **Issues** section of the relevant repository.

When opening an issue, please include when possible:

- app version;
- firmware version;
- phone model and Android version;
- steps required to reproduce the problem;
- relevant logs, screenshots, or error messages.

## Contributing

Contributions, fixes, and new features are welcome.

You can:

1. fork the repository;
2. create a new branch;
3. make your changes;
4. open a Pull Request with a clear description.

## Support the project

You can support Open Airsoft Countdown in several ways:

- give the GitHub repositories a **Star**;
- leave a **Like**, save the project, and use a **Boost** on MakerWorld;
- report bugs and suggestions through GitHub Issues;
- support development with a PayPal donation.

- MakerWorld: https://makerworld.com/it/@maydayalaska
- PayPal: https://paypal.me/lorisgennarini

## License

This project is released under the **GNU Affero General Public License v3.0**.

See the [LICENSE](LICENSE) file for details.
