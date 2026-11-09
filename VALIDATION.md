# Local validation

## Company design update — November 8, 2026

- Mock version 1.0.1 uses the company application's actual brand resources,
  welcome artwork, fonts, icons, header, drawer, four-tab navigation and
  corresponding responsive storefront, details, cart and management layouts.
- The combined Gradle validation passed Android debug assembly/lint, Windows
  desktop compilation and complete portable app-image creation.
- All 24 tests passed: 21 unchanged mock-commerce tests and 3 desktop window
  sizing tests, with no failures, errors or skipped tests.
- The updated Windows installation and all 417 copied files were verified against
  the built app image. Desktop and Start menu shortcuts target the new version.
- The Windows welcome-to-catalog flow was visually inspected, including the
  company artwork, botanical hero, New arrivals cards and matching navigation.
  A development runtime session reached the catalog without a logged exception.
- Android retains the separate `org.mz.botanicalsmock` ID and `mzbotanicalMock`
  label. The updated APK installed and cold-launched on the connected Android
  emulator. Its matching order-history layout was inspected, and an Android
  system-bar background mismatch was corrected and rebuilt. Physical-phone
  installation and a complete phone walkthrough are still pending connection.
- The existing offline commerce model and data storage locations were retained.
  The company repository's existing working-copy changes were not edited.

## Original offline demo — November 4, 2026

- 21 mock commerce tests passed with no failures or skipped tests. Coverage
  includes exact-cent totals, checkout success/failure, persisted retry safety,
  inventory bounds/restoration, immutable order snapshots, customer approval,
  product edits, persistence failures and corrupt-data recovery.
- Android debug assembly, desktop compilation, Windows app-image creation and
  Android lint completed successfully in the combined Gradle run.
- Installed the debug APK on the Samsung SM-S938U alongside `org.mz.killrs`.
  The mock cold-launched successfully in 669 ms and its process remained running;
  no crash entry was recorded for that process. A complete phone walkthrough is
  not yet recorded.
- Rebuilt and reinstalled with the requested Android launcher label
  `mzbotanicalMock`. APK metadata confirmed the exact name, installation preserved
  app data, and the renamed application cold-launched successfully in 614 ms.
- Windows portable application launched. The storefront, product details, cart,
  checkout, successful simulated order, order confirmation/timeline and admin
  summary were inspected. The $40.00 test item produced $5.00 sample shipping,
  $3.20 sample tax and a $48.20 total. The cart cleared after the simulated order.

The mock is offline and uses fictional information. These checks do not validate
real payments, tax, deliveries, email, cloud access or the separate company app's
launch readiness. Remote CI results are separate from these local checks.
