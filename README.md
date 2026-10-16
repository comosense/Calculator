# [Calculator](https://github.com/comosense/Calculator/)

A simple calculator app designed specifically for Wear OS smartwatches.

Designed for small, round watch displays, with large, easy-to-tap keys and a compact operator overlay.

[![Get it on Google Play](https://img.shields.io/badge/Google_Play-Download-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.gmail.comosense.calculator)

## Features

### Basic arithmetic

* Addition (`+`)
* Subtraction (`-`)
* Multiplication (`×`)
* Division (`÷`)

### Scientific functions

* Square root (`√`)
* Exponentiation (`^`)
* Factorial (`!`)
* Sine (`sin`)
* Cosine (`cos`)
* Tangent (`tan`)
* Common logarithm (`log`, base 10)
* Natural logarithm (`ln`)
* Mathematical constants: pi (`π`) and Euler's number (`e`)

### Expression input

* Follows standard mathematical operator precedence
* Supports grouped expressions such as `(1 + 2) × 3`
* Supports unary `+` and `-`

### Smart keypad

* Keys adapt to the current input context
* The parentheses key switches between `(` and `)`
* The `+` and `-` keys switch between unary signs and binary arithmetic operators
* The delete key switches between Backspace and Clear, depending on the input state
* Long-press the Backspace key to clear the current expression
* Switch between the main keypad and the scientific function keypad

### Display

* Locale-aware thousands and decimal separators
* Text size adjust automatically to fit the available display area
* Long expressions remain accessible through horizontal scrolling

### Calculation history

* Tap a previous calculation to reuse its result
* When an expression is being entered, the selected result is appended to the current input
* Delete individual entries or clear the entire history
* Long-press a history entry to enter deletion mode

### Settings

* Set the number of significant digit used in calculations

## Usage

### Basic calculation

Enter an expression using the keypad and press `=`.

The calculator follows standard mathematical operator precedence, so multiplication is performed before addition.

For example:

```text
1 + 2 × 3 = 7
```

Parentheses and grouped expressions are also supported.

```text
(1 + 2) × 3 = 9
```

### Scientific function calculation

Switch to the scientific keypad to access additional functions.

Examples:

```text
√9 = 3
2^3 = 8
5! = 120
sin(π / 2) = 1
log(100) = 2
ln(e) = 1
```

Trigonometric functions use radians. Results are rounded according to the calculator's configured precision.

### Deleting and clearing

While entering an expression, the delete key works as Backspace.

When no expression is active, the delete key switches to Clear.

You can also long-press the Backspace key to clear the current expression.

### Managing calculation history

Tap the expression display at the top of the calculator to open the history.

From the history screen, you can:

1. Tap a calculation to reuse its result.
2. Long-press a history entry to enter delete mode.
3. Tap individual entries to delete them.
4. Tap **Delete all** to clear the history.
5. Swipe from the left edge or press the back button to return to the calculator.

The history is stored locally on the watch using Jetpack DataStore and is limited to 50 entries.

### Changing settings

Long-press the expression display at the top of the calculator to open the settings.

## Supported devices

Compatible with Wear OS devices running Android API 30 or later.

The UI is optimized for smartwatch displays, including round screens.

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
* Arbitrary-precision decimal arithmetic using `BigDecimal`
* `HALF_UP` rounding
* Custom implementations of square root, exponentiation, factorial, trigonometric functions, logarithms, and mathematical constants

### Calculation limits

To keep calculations and displayed results within defined limits:

* Factorial arguments must be non-negative integers no greater than 146
* Integer exponents must have an absolute value of 1024 or less
* Non-integer exponents must have an absolute value of 1024 or less
* Results are limited to 256 characters when represented in plain decimal nation
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
