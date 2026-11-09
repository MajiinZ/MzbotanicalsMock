# Company design parity

The mock's Android and Windows UI follows the company application in
`MajiinZ/MzBotanicals`, using its November 8, 2026 working-copy visuals as the
reference. The mock remains a standalone repository and does not require that
checkout to build or run.

## Shared visual references

| Mock element | Company source reference |
| --- | --- |
| Brand colors and serif/sans font roles | `shared/.../Colors.kt`, `Fonts.kt` |
| Material color scheme and typography | `shared/.../StorefrontTheme.kt` |
| Primary action shape, sizing and states | `shared/.../component/PrimaryButton.kt` |
| Card imagery and image fallback | `shared/.../component/ProductCard.kt` |
| Logo and welcome artwork | `shared/src/commonMain/composeResources/drawable/mz_brand.png`, `mz_welcome.png` |
| Header, drawer and navigation vectors | Company `home`, `leaf`, `shopping_cart`, `profile`, `hamburger_menu`, `payments`, `settings`, `manage_account`, `back_arrow`, `close` resources |
| Legacy form typography | The company's bundled `exo2_regular.otf` font |
| Android native launch | Company `MainActivity.kt` splash exit, launcher densities, green splash surface and native artwork |
| Windows native window | Company `DesktopWindowSizing.kt` work-area bounds and branded window icon |

The matching assets are copied into this repository and loaded using Compose
resources on both platforms. The mock product image uses the same brand-logo
fallback as the company product card. The previous imaginary packet illustration
has been removed. Live catalog photography is intentionally not downloaded.

`BrandComponents.kt` defines the common tokens and controls; `MockTheme.kt`
retains the company Material theme values. Primary buttons use a minimum height
of 56 dp, a 16 dp radius, 16 sp semibold text and the same enabled/disabled colors.
Selection and summary cards use the company's 24 dp surface corners and outlined
paper styling.

The Android splash uses the same native green and icon, followed by the same
650 ms artwork fade. Light/dark system icon contrast follows the displayed mock
screen using only its own Activity window. The mock's name and application ID stay
distinct. Windows uses the company window-size bounds, centered positioning and
brand icon, including bounds that keep the minimum size inside small work areas.

## Intentional demo differences

- A visible demo identifier distinguishes simulated results from real orders.
- A local demo entry action replaces Google authentication and identity checks.
- Product, customer, payment, shipping, tax, refund and tracking data remain
  fictional and are persisted only on the current device.
- All cart, order and administrative actions still use the existing mock engine.
- No Firebase configuration, merchant credentials, live catalog requests or
  payment calls are introduced. The Android app has no Internet permission.
- The Android ID, Windows upgrade ID and storage locations are unchanged, so the
  appearance update can preserve existing demo data and coexist with the company
  application.

When updating the company design, review the corresponding mock screens and
copied resources together. Appearance parity does not establish that the company
store is configured or approved for live sales.
