# Calculator

A simple calculator app designed specifically for Wear OS smartwatches.

Designed for small and round watch displays, with large, easy-to-tap keys and a compact operator
overlay.

[![Get it on Google Play](https://img.shields.io/badge/Google_Play-Download-414141?style=for-the-badge\&logo=google-play\&logoColor=white)](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

## Features

### Basic arithmetic

* Addition `+`
* Subtraction `-`
* Multiplication `×`
* Division `÷`

### Scientific functions

* Square root
* Exponentiation
* Factorial
* Sine
* Cosine
* Tangent
* Common logarithm
* Natural logarithm
* Pi
* Euler's number

Trigonometric functions use radians.

### Function keypad

* Switch between the main keypad and the scientific function keypad
* Access scientific functions and mathematical constants without leaving the calculator screen

### Parentheses support

* Supports grouped expressions such as `(1 + 2) × 3`

### Positive / negative numbers

* Supports unary `+` and `-`

### Smart keypad

* Keys adapt to the current input context
* The parenthesis key switches between `(` and `)`
* The `+` and `-` keys switch between unary signs and binary arithmetic operators
* The delete key switches between Backspace and Clear
* Long-press Backspace to clear the current input
* Switch between the main keypad and the scientific function keypad

### Locale-aware number formatting

* Thousands separators and decimal separators follow the device locale
* Long expressions remain accessible through horizontal scrolling
* Text size adjusts to fit the available display area when possible

### Calculation history

* Up to 50 calculations are saved
* History is preserved across app launches
* Tap a previous calculation to reuse its result
* When an expression is being entered, the selected result is appended to the current input
* Delete individual entries or clear the entire history
* Long-press a history entry to enter delete mode

History is stored locally on the watch using Jetpack DataStore.

## Usage

### Basic calculation

Enter an expression using the keypad and press `=`.

For example:

```text
1 + 2 × 3 = 7
```

The calculator follows standard mathematical operator precedence, so multiplication is performed
before addition.

### Parentheses

The parenthesis button automatically switches between `(` and `)` depending on the current
expression.

For example:

```text
(1 + 2) × 3
```

### Scientific function calculation

Switch to the scientific function keypad to access additional operations.

Examples:

```text
√9 = 3
2^3 = 8
5! = 120
sin(π / 2) = 1
log(100) = 2
ln(e) = 1
```

Trigonometric functions use radians. Results are rounded according to the calculator's configured precision and display scale.

### Deleting and clearing

While entering an expression, the delete button works as Backspace `←`.

When there is no active expression, it becomes Clear.

You can also long-press the backspace button to clear the current input.

### Managing calculation history

Tap the expression display at the top of the calculator to open the history.

From the history screen, you can:

1. Tap a calculation to reuse its result.
2. Long-press a history entry to enter delete mode.
3. Tap individual entries to delete them.
4. Tap **Delete All** to clear the history.
5. Swipe from the left edge or press the back button to return to the calculator.

The history is stored locally on the watch using Jetpack DataStore and is limited to 50 entries.

## Supported devices

The app targets Wear OS devices running Android API 30 or later.

The UI is designed specifically for smartwatch displays, including round screens.

## Technology stack

* Language: Kotlin
* Platform: Wear OS
* Minimum SDK: Android API 30
* Target SDK: Android API 37
* UI: Jetpack Compose
* Wear UI: Compose for Wear OS / Wear Compose Material 3
* State management: ViewModel, StateFlow, SharedFlow
* Concurrency: Kotlin Coroutines
* Persistence: Jetpack DataStore
* Serialization: Protocol Buffers

## Calculation engine

The calculator uses a custom expression parser and mathematical calculation engine implemented in Kotlin.

It does not rely on a general-purpose expression evaluation library.

The engine includes:

* Tokenization and recursive-descent parsing
* Operator precedence for arithmetic expressions
* Unary `+` and `-`
* Parentheses and grouped expressions
* Arbitrary-precision decimal arithmetic using BigDecimal
* A calculation precision of 32 significant digits
* `HALF_UP` rounding
* A display scale of up to 24 digits after the decimal point, with trailing zeros removed
* Custom implementations of square root, exponentiation, factorial, trigonometric functions, logarithms, and mathematical constants

### Calculation limits

To keep calculations and displayed results within defined limits:

* Factorial arguments must be non-negative integers no greater than 146
* Integer exponents are limited to an absolute value of 1024
* Non-integer exponents are limited to an absolute value of 1024
* Results are limited to 256 characters in their plain decimal representation
* Expressions are limited to 1,024 symbols

Invalid operations, including division by zero and out-of-domain function arguments, are reported as calculation errors.

All calculations are performed locally by the app.

## Privacy

The calculator stores calculation history locally on the watch.

The calculator does not require an account, network connection, or external service.

See [Privacy Policy](privacy-policy.md) for details.

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
