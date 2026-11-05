# MZ Botanicals Mock

A fully offline Android and Windows demonstration of the MZ Botanicals shopping
and administration experience. All products, customers, payments, shipping, tax,
tracking and orders are fictional. No real money is charged and nothing is sent
to a business, payment processor or delivery carrier.

The separate company setup application lives at
<https://github.com/MajiinZ/MzBotanicals>. This mock is for presentations and
training, and is not a live commerce deployment.

## Try the demo

1. Open **Shop**, search/filter the collection and view a product.
2. Add packs to the cart and adjust quantities.
3. Open checkout. Use the prefilled fictional address and choose a simulated
   payment result: approved, declined or timed out. Only approved orders deduct
   stock and clear the cart.
4. Open **Orders** to inspect line items, totals, status, tracking and the timeline.
5. In **Admin**, progress an order through packing, shipping and delivery. Try
   cancellation or refund on eligible orders; inventory is restored once.
6. Edit product prices, pack sizes, publication and stock in the admin catalog.
   Approve/reject fictional customer profiles in the customer tab.
7. Edit your fictional shipping profile. Restart to confirm saved changes.
8. Use **Profile → Reset demo data** to restore all samples after a presentation.

Sample shipping is $5.00 and sample tax is 8% of merchandise, rounded to the
nearest cent. These are demonstration assumptions, not tax advice or a real
shipping quote. No actual payment account or identity verification is required.

## Separate installation and storage

- Android app: **mzbotanicalMock**, ID `org.mz.botanicalsmock`.
- Windows launcher: `MzbotanicalsMock.exe`.
- Windows local data: `%USERPROFILE%\.mzbotanicals-mock\demo-state-v1.json`.
- Android local data: private application preferences, backed up nowhere by this app.

It can coexist with the company app. Data stays on each device and does not sync.
No Firebase configuration, cloud credentials or Internet permission is included.
Enter fictional information only; reset removes local mock data, not company data.
If saved data is unreadable, it is preserved until an explicit reset.

## Build and test

Requirements: JDK 21, Android SDK 35 and network access to download the locked
Gradle/Maven dependencies. Set `ANDROID_HOME` or a private `local.properties` SDK
path. No Google services file is required.

```powershell
.\gradlew.bat :mockEngine:desktopTest :composeApp:compileKotlinDesktop :composeApp:assembleDebug :composeApp:lintDebug --console=plain
.\gradlew.bat :composeApp:run
.\gradlew.bat :composeApp:createDistributable
```

On macOS/Linux use `./gradlew` instead. The current tested targets are Android and
Windows; this repository does not implement iOS or web. Windows app-image output
is under `composeApp/build/compose/binaries/main/app/MzbotanicalsMock/`. Copy the
entire directory when distributing it: the EXE relies on the bundled runtime and
application files. The Android development APK is under
`composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

## Structure and maintenance

- `mockEngine/`: serializable local commerce model, guarded state changes and tests.
- `composeApp/`: shared Compose UI, Android/Windows entry points and local storage.
- `.github/workflows/verify.yml`: engine tests and platform compilation.

Successful orders capture immutable prices and address snapshots. Payment retries
use persisted request IDs, failed saves preserve the previous state, and order
transitions prevent repeated inventory restoration. The UI and engine contain no
real payment, email, shipping-label or cloud behavior. The visible demo label is
intentional and must remain when presenting simulated outcomes.
