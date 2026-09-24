# Calculator

A simple calculator app designed specifically for Wear OS smartwatches.

Built with Wear Compose Material 3, it offers easy-to-tap keys and an operator overlay optimized for
round and small smartwatch screens.

[![Get it on Google Play](https://img.shields.io/badge/Google_Play-Download-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

Available on Google Play:  
[Download on Google Play Store](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

## Features

- **Basic Arithmetic Operations**: Addition (`+`), subtraction (`-`), multiplication (`×`), and
  division (`÷`)
- **Smart Keys**:
  - Keys automatically switch between their available states depending on input context (`+` /
      positive sign, `-` / negative sign, `(` / `)`, and `←` / `Clear`).
  - An operator overlay panel (operator key) keeps keys large and easy to hit on small displays.
- **Parentheses & Signs**: Support for grouped expressions and negative/positive numbers.
- **Locale-Aware Formatting**: Automatically formats numbers with grouping (thousands) separators
  and locale-specific decimal separators (e.g., `,` or `.`).
- **Calculation History**:
  - Automatically saves up to 50 calculations across sessions using Jetpack DataStore (Protobuf).
  - Tap any past result to insert or reuse it in your current calculation.
  - Interactive delete mode: remove single entries or clear all history.

## Usage

### Basic Calculation

Tap the keypad buttons to construct your expression:

- **Digits (`0`–`9`) & Decimal Point (`.` / `,`)**: Enter numbers.
- **Operators Key**: Opens a quick overlay containing `+`, `-`, `×`, and `÷`.
- **Dynamic Parenthesis Key**: Toggles between `(` and `)` depending on whether an open parenthesis
  needs to be closed.
- **Delete / Clear Key**:
  - Shows `←` (Backspace) when an expression is being entered.
  - Shows Clear when idle.
  - *Tip*: Long-press `←` at any time to clear the entire input.
- **`=`**: Calculates the expression.

Expressions follow standard mathematical operator precedence (e.g., `1 + 2 × 3 = 7`).

### Calculation History

1. **Open History**: Tap the expression display area at the top of the main screen.
2. **Reuse a Result**: Tap any history item to append its result to your current expression (or use it as the starting value).
3. **Delete History**:
  Long-press any history entry to enter **Delete Mode**.
  Tap individual items to delete them.
  Tap the bottom **Delete All** button to clear the entire history.
4. **Exit History**: Swipe from the left edge (Swipe-to-Dismiss) or press the back button.

## Architecture & Tech Stack

- **Platform**: Android Wear OS (Wear OS 4 / 5+)
- **Language**: Kotlin (100%)
- **UI Toolkit**: Jetpack Compose for Wear OS, Wear Compose Material 3
- **Data Persistence**: Jetpack DataStore (Protocol Buffers)
- **State Management & Concurrency**: Android Architecture Components (`ViewModel`, Kotlin
  Coroutines, `StateFlow`, `SharedFlow`)
- **Calculation Engine**:
  - Lexer/Tokenizer converting raw symbols into structured tokens.
  - Recursive-descent parser supporting operator precedence and unary signs.
  - `BigDecimal` with custom `MathContext` (50-digit precision, `HALF_UP` rounding).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT.

IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

This app is provided for general-purpose calculations and personal use.

While reasonable care has been taken to ensure the correctness of the calculation logic, no
guarantee is made regarding the accuracy or suitability of the results for any particular purpose.

Do not rely on this app for critical, financial, medical, scientific, or other high-stakes
calculations. Always verify important results using an appropriate and reliable calculator or
calculation tool.
