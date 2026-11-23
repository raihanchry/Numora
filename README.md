# Numora

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**Numora** is a free, open-source, privacy-first Android calculator built in pure **Java** with Material 3 design. It combines a fast everyday & scientific calculator, a multi-category unit converter, and an exact age calculator in one lightweight, completely offline app.

## Features

### Basic & Scientific Calculator
- **Dual Mode Toggle**: Seamlessly switch between Basic and Scientific modes (with an adaptive side-by-side landscape layout).
- **Scientific Functions**: `sin`, `cos`, `tan`, inverse trigonometry (`sin⁻¹`, `cos⁻¹`, `tan⁻¹`) via `2nd` mode, `DEG / RAD` angle unit switcher, base-10 `log`, natural `ln`, `10ˣ`, `eˣ`, right-associative exponentiation (`xʸ`), square root (`√`), square (`x²`), `π`, `e`, factorial (`n!`), percent (`%`), and `ANS`.
- **Live Inline History Tape**: View recent calculations directly above the active expression, or open the full searchable history sheet to reuse past results or expressions.
- **Thoughtful Everyday UX**: Live auto-preview while typing, floating-point precision normalization (no `0.1 + 0.2 = 0.30000000000000004` artifacts), long-press anywhere on the display to copy with Toast feedback, and long-press backspace to clear all.

### Unit Converter
- **8 Measurement Categories**: Length, Weight, Temperature, Area, Volume, Speed, Time, and Data.
- **Fast Conversion**: Instant live output, one-tap unit swap, copy result action, and a built-in numeric keypad with sign toggle (`±`) for negative temperatures.

### Age Calculator
- **3-Wheel Vertical Scroll Date Picker**: Quickly scroll Day, Month, and Year without navigating a slow calendar grid, with automatic leap-year and month-length validation.
- **Complete Breakdown**: Exact age in years, months, and days; birth weekday; next birthday countdown and weekday; plus total months, weeks, days, hours, and minutes lived.

### Privacy & Design
- **100% Offline & Zero Permissions**: No internet access, no ads, no analytics, and no tracking SDKs.
- **Light & Dark Themes**: One-tap contrast toggle between clean Alabaster Light and Charcoal Dark themes.

## Tech Stack
- **Language**: Java (Java 11 compatibility)
- **UI**: AndroidX AppCompat, Material Components 3, ConstraintLayout, RecyclerView, ViewBinding
- **Architecture**: MVVM (`AndroidViewModel` + `LiveData`) with pure Java math, conversion, and calendar engines
- **Persistence**: On-device `SharedPreferences` (local calculation history and UI preferences only)

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/numora.git
   cd numora
   ```
2. Open the project in **Android Studio** (AGP 9.x / SDK 36) or build via Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run the pure Java unit tests:
   ```bash
   gradle :app:testDebugUnitTest --tests "com.example.ExpressionEvaluatorTest" --tests "com.example.ConverterAndAgeEngineTest"
   ```

## Privacy Policy
Numora does not collect, store, or transmit any personal data. See [PRIVACY.md](PRIVACY.md) for full details.

## Contributing
Contributions, bug reports, and translations are welcome! Please open an issue or submit a pull request.

## License
Numora is Free and Open Source Software licensed under the **GNU General Public License v3.0 (GPL-3.0-or-later)**. See [LICENSE](LICENSE) for details.
