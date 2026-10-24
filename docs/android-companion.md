<p align="right"><a href="android-companion.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# Android companion

The public 0.6.0 APK uses Android platform APIs and a native OpenStreetMap tile view, without a proprietary map SDK, map key, Google service or cloud account. Source: https://github.com/qfx1118-oss/iDea-Tape . Contact: qfx1118@qq.com .

## Install and pair

Install the release APK on Android 8 or later. Keep the same app/signature when updating so private recordings survive; uninstalling removes them. With matching BLE firmware, leave the device in recording standby, tap Connect on the phone, grant nearby-device/Bluetooth permission, select iDea Tape and enter the six-digit pairing code displayed on the recorder. Older Android versions need location permission for BLE discovery. The receiving foreground service displays a persistent notification; Stop receiving disconnects it. No Wi-Fi provisioning is needed for Bluetooth transfer.

## Record and receive

Use the original device controls. The phone accepts blocks during recording; when recording ends it requests missing ranges, verifies the full encoded audio, writes/syncs the WAV and metadata, then acknowledges. Only an identity/length/checksum-matched acknowledgement permits device deletion. Disconnects, failed saving or missing acknowledgement retain device recordings. Backfill is implemented but needs further interruption testing. One device recording remains bounded by device cache capacity; live transfer is not unlimited streaming or live monitoring of the unfinished WAV.

The phone's raised red-circle key records its microphone independently. During recording it displays a square to stop/save. Device and phone recordings share the same private library. Favorites persist, search matches titles/source/dates, and time filters cover today, 7/30 days and inclusive custom ranges. Date-grouped native recycled rows cover the entire library. Filled gold stars identify favorites. Swipe a row left and tap the delete icon; a 5-second countdown Undo restores it. The details and More menu also offer deletion. Deleting stops its playback and removes its map marker. New default titles include source and local date/time; legacy defaults are migrated without changing custom titles. Unknown start times are labeled as import times. Shared WAV/JSON display names derive from titles with a stable suffix; internal IDs remain stable.

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

## Place names and device display and deletion

On the recording page, enable recording-start location, then Place names and device display and accept its separate disclosure. Grant location and keep Bluetooth connected. Android Geocoder resolves the current road (district/city only when no road is returned), without a map SDK or application API key. The system provider may use a network and may be unavailable. The request runs away from audio/BLE handling, at most once per 30 seconds, with a 60-second/80-meter cache. Only a fix within ten seconds is sent; device updates expire after 25 seconds without refresh and clear on disconnect. The current device label is transient. Separately, the library resolves each saved recording coordinate and stores its road name with that recording; the current phone place is never substituted for an old recording. Disable the switch to clear the device label.

Swipe left then tap the trash icon, or delete from details/More. Undo shows a five-second countdown while this screen remains alive; leaving the app may finalize deletion. Files are moved privately during the undo window and then removed. On restart, any stale undo files are purged after 60 seconds. Only recording identity/block/checksum receipts remain, without audio or GPS, so a late retransmission of an intentionally deleted, previously durable recording can be acknowledged safely. Unreceived or mismatched recordings are never acknowledged by that receipt.

## 0.6.0 playback and power management

The cassette uses a charcoal shell, cream Fraunces italic lettering and restrained orange details. The dot of the lowercase i is cream when idle or playing and red while recording. Library playback opens the cassette player; tap Play/Pause, drag the timeline, skip ten seconds backward/forward, or choose 1, 1.25, 1.5, 1.75 or 2 times speed. Hold the cassette for temporary 2-times playback and release to restore the selected speed. The tape coasts and recoils after pausing. Map playback remains compact.

Idle BLE uses balanced connection priority; active audio receiving requests high priority and returns to balanced after saving. Idle list checks are 15 seconds instead of five, reconnect delays back off to 30 seconds, and durable receive checkpoints batch 64 blocks instead of 16. A crash may require retransmitting more uncheckpointed blocks; final full-audio verification, durable WAV/metadata and matched acknowledgement remain mandatory before device deletion. GPS requests are eight seconds apart and network fixes thirty seconds apart; the ten-second freshness requirement is unchanged. Phone-only recording stops its foreground service after saving. Offscreen animation does not continuously schedule frames; visible reels are capped at about 30 fps.

These changes reduce avoidable work, but do not establish battery or temperature savings on an OPPO Find X8 Ultra with ColorOS 17. Compare the same recording/receiving duration with the screen off, first without location and then with it; check Settings > Battery usage. Do not globally disable battery protection. If vendor restrictions interrupt locked-screen receiving, inspect this app's background permission specifically. Android guidance recommends returning BLE to balanced priority after transfer: https://developer.android.com/reference/android/bluetooth/BluetoothGatt . OPPO battery usage guidance: https://support.oppo.com/en/answer/?aid=2171043 .
