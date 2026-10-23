<p align="right"><a href="android-companion.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# Android companion

The public 0.4.0 APK uses Android platform APIs and a native OpenStreetMap tile view, without a proprietary map SDK, map key, Google service or cloud account. Source: https://github.com/qfx1118-oss/iDea-Tape . Contact: qfx1118@qq.com .

## Install and pair

Install the release APK on Android 8 or later. Keep the same app/signature when updating so private recordings survive; uninstalling removes them. With matching BLE firmware, leave the device in recording standby, tap Connect on the phone, grant nearby-device/Bluetooth permission, select iDea Tape and enter the six-digit pairing code displayed on the recorder. Older Android versions need location permission for BLE discovery. The receiving foreground service displays a persistent notification; Stop receiving disconnects it. No Wi-Fi provisioning is needed for Bluetooth transfer.

## Record and receive

Use the original device controls. The phone accepts blocks during recording; when recording ends it requests missing ranges, verifies the full encoded audio, writes/syncs the WAV and metadata, then acknowledges. Only an identity/length/checksum-matched acknowledgement permits device deletion. Disconnects, failed saving or missing acknowledgement retain device recordings. Backfill is implemented but needs further interruption testing. One device recording remains bounded by device cache capacity; live transfer is not unlimited streaming or live monitoring of the unfinished WAV.

The phone's raised red-circle key records its microphone independently. During recording it displays a square to stop/save. Device and phone recordings share the same private library. Favorites persist, search matches titles/source/dates, and time filters cover today, 7/30 days and inclusive custom ranges. Fifty rows are drawn at a time while all metadata participates in filters. New default titles include source and local date/time; legacy defaults are migrated without changing custom titles. Unknown start times are labeled as import times. Shared WAV/JSON display names derive from titles with a stable suffix; internal IDs remain stable.

## Optional positions and map

Enable location only if wanted. A position must be no more than ten seconds old at the actual recording START event, otherwise it stays absent. Saved data includes WGS84 latitude/longitude, accuracy, provider, observation time and age. Only a starting point is recorded. An old import or a phone joining midway never receives a fabricated start position. The app displays readiness and accuracy; approximate-location permission may give coarse coordinates.

Map filters share search/favorite/date state with the library. Pan/pinch or use zoom buttons; current-area-only limits matching markers and the results list. View all resets area filtering and fits matching positions. Tap a marker, choose among overlapping recordings, and play inside the app. The results list offers a non-gesture alternative; details can still open another map application. Recordings without valid positions stay in the library.

Online tiles are off until enabled explicitly. The native renderer requests only visible tiles over HTTPS from tile.openstreetmap.org, identifies the app with a stable user agent/contact, caches for at least seven days, revalidates stale tiles and visibly attributes OpenStreetMap. It has no map account or key and does not offer offline-region prefetch. Normal tile access is best effort, not a service guarantee. If tiles fail, local markers and playback remain. See [tile policy](https://operations.osmfoundation.org/policies/tiles/) and [data attribution/license](https://www.openstreetmap.org/copyright).

## Privacy and preservation

Recordings, metadata and favorites are app-private; export important recordings before uninstalling. Share defaults to audio only; the user must separately select audio plus location JSON. Only viewed tiles are sent to the map server, which can observe IP and approximate viewed region; audio, titles and marker datasets are not sent by the app. Location is optional and no route or cloud backup is created. Playback and BLE transfer do not need map networking. Phone permission denial, storage-full recovery, reconnect interruptions and vendor background limits need additional phone testing.

## Build

Use JDK 17, Android platform 35 and build-tools 35.0.0. Run:

```sh
python3 tools/build_android.py --jdk /path/to/jdk17 --platform /path/to/android-35 --tools /path/to/build-tools/35.0.0 --keystore /outside/repository/testing.keystore
python3 tools/check_android_package.py build/android/idea-tape-android.apk
```

No Gradle or third-party Java dependencies are required. The script creates a development key when the supplied external path is missing; retain the same signing key for upgrades. Keep signing keys outside version control. SDK tools, generated classes, DEX and APK files belong under ignored build directories. Protocol/audio recovery and library/date/projection rules have pure Java host tests in tests/android. A successful build is not phone rendering or live-map acceptance.
