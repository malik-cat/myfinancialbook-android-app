# Implementation Plan - Build Fix and Offline Robustness

The user reported a build failure related to the `foojay` plugin being unable to download JDK 21 due to network/DNS issues. Additionally, the project recently migrated to using Firestore auto-generated IDs (`firestoreId`) as the primary key for syncing. This plan addresses the build blocker and improves the robustness of the ID system for offline usage.

## User Review Required

> [!IMPORTANT]
> I will be removing the `foojay-resolver-convention` plugin from `settings.gradle.kts`. This means Gradle will no longer attempt to download JDKs from `api.foojay.io`. You will need to ensure a JDK (version 17 or higher) is available in your environment (Android Studio's bundled JDK is usually sufficient).

> [!WARNING]
> The current sync logic uses `firestoreId` for `partyId`. If a user is offline or not logged in, `firestoreId` may be empty, causing collisions between different parties' entries. I propose generating a local UUID if Firestore is unavailable to ensure unique identification.

## Proposed Changes

### Build Configuration

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/MALIK-CE/MyFinancialBook/settings.gradle.kts)
- Comment out or remove the `foojay-resolver-convention` plugin to avoid network-dependent JDK downloads.

### Data & Sync Robustness

#### [MODIFY] [FirestoreSync.kt](file:///C:/Users/MALIK-CE/MyFinancialBook/app/src/main/java/com/myfinancialbook/app/sync/FirestoreSync.kt)
- Add a helper method to generate a document ID locally using `UUID` or `db.collection(...).document().id` (which works offline) to ensure every entity has a unique `firestoreId` even before it's synced.

#### [MODIFY] [LedgerRepository.kt](file:///C:/Users/MALIK-CE/MyFinancialBook/app/src/main/java/com/myfinancialbook/app/data/LedgerRepository.kt)
- Update `addPartyWithOpening` and other `add*` methods to ensure a unique `firestoreId` is always generated, even if the `FirestoreSync` instance is not available or the user is offline.

## Verification Plan

### Automated Tests
- I will run `gradle_sync` to ensure the project structure is valid without the foojay plugin.
- I will run `gradle_build` to verify the build completes (assuming a local JDK is found).

### Manual Verification
- Verify that `partyId` remains unique even if created while offline.
- Ensure navigation to `PartyDetailScreen` works correctly with the new IDs.
