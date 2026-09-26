# Calculator

A simple calculator app designed specifically for Wear OS smartwatches.

Designed for small and round watch displays, with large, easy-to-tap keys and a compact operator overlay.

[![Get it on Google Play](https://img.shields.io/badge/Google_Play-Download-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

Available on Google Play:
[Download on Google Play Store](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

## Features

- **Basic arithmetic**
  - Addition `+`
  - Subtraction `-`
  - Multiplication `×`
  - Division `÷`
- Parentheses
  - Supports grouped expressions such as `(1 + 2) × 3`
- Positive / negative numbers
  Supports unary `+` and `-`
- Smart keypad
  - Keys change depending on the current input context
  - Parenthesis key switches between `(` and `)`
  - Delete key switches between backspace `←` and clear
- Operator overlay
  - Quickly access `+`, `-`, `×`, and `÷`
  - Keeps the main keypad large and easy to use on a small screen
- Locale-aware number formatting
  - Thousands separators and decimal separators follow the device locale
- Calculation history
  - Up to 50 calculations are saved
  - History is preserved across app launches
  - Tap a previous result to reuse it
  - Delete individual entries or clear the entire history

## Usage

### Basic calculation

Enter an expression using the keypad and press `=`.

For example:

```text
1 + 2 × 3 = 7
```

The calculator follows standard mathematical operator precedence, so multiplication is performed before addition.

### Operators

Tap the operator button to open the operator overlay:

```text
+  -  ×  ÷
```

### Parentheses

The parenthesis button automatically switches between `(` and `)` depending on the current expression.

For example:

```text
(1 + 2) × 3
```

### Delete and clear

While entering an expression, the delete button works as Backspace `←`.
When there is no active expression, it becomes Clear.

You can also long-press the backspace button to clear the current input.

### Calculation history

Tap the expression display at the top of the calculator to open the history.

From the history screen you can:

1. Tap a calculation to reuse its result.
2. Long-press a history entry to enter delete mode.
3. Tap individual entries to delete them.
4. Tap Delete All to clear the history.
5. Swipe from the left edge or press the back button to return to the calculator.

The history is stored locally on the watch using Jetpack DataStore and is limited to 50 entries.

## Supported devices

The app targets Wear OS devices running Android API 30 or later.
The UI is designed specifically for smartwatch displays, including round screens.

## Privacy

Calculator stores calculation history locally on the watch.
No account or server connection is required for the calculator itself.
See [Privacy Policy](privacy-policy.md) for details.

## Technology stack

- Language: Kotlin
- Platform: Wear OS
- Minimum SDK: Android API 30
- Target SDK: Android API 37
- UI: Jetpack Compose
- Wear UI: Compose for Wear OS / Wear Compose Material 3
- State management: ViewModel, StateFlow, SharedFlow
- Concurrency: Kotlin Coroutines
- Persistence: Jetpack DataStore
- Serialization: Protocol Buffers

## Calculation engine

The calculator does not rely on a general-purpose expression evaluation library.

The expression engine consists of:

- Lexer / tokenizer
- Recursive-descent parser
- Operator precedence handling
- Unary + / -
- BigDecimal-based calculation
- Custom MathContext with 50-digit precision
- HALF_UP rounding

This allows expressions to be parsed and calculated locally on the watch.

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
