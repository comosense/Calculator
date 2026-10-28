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
* Squaring (`^2`)
* Exponentiation (`^`)
* Power of ten (`10^`)
* Exponential function (`exp`)
* Factorial (`!`)
* Trigonometric functions: Sine (`sin`), Cosine (`cos`), Tangent (`tan`)
* Inverse trigonometric functions: Arcsine (`asin`), Arccosine (`acos`), Arctangent (`atan`)
* Angle modes: Switchable between Radians (`RAD`) and Degrees (`DEG`)
* Logarithm functions: Common logarithm (`log`, base 10), Natural logarithm (`ln`)
* Mathematical constants: pi (`π`) and Euler's number (`e`)

### Expression input

* Follows standard mathematical operator precedence
* Supports grouped expressions such as `(1 + 2) × 3`
* Supports unary `+` and `-`
* **Continuous calculation**: Pressing an operator or special operator (`+`, `-`, `×`, `÷`, `^`, `!`) immediately after evaluating an expression continues calculation using the previous result
* **Implicit multiplication**: Automatically inserts `×` when entering constants, functions, or opening parentheses after numbers (e.g., entering `π` after `2` becomes `2×π`)
* **Auto-completion**: Unclosed parentheses are displayed with a semi-transparent preview and automatically closed when evaluating
* **Smart decimal point**: Automatically prefixes with `0` when `.` is entered at the beginning of an input or after an operator

### Smart keypad

* Keys adapt to the current input context
* The parentheses key switches between `(` and `)`
* Long-press the closing parenthesis key `)` to input an opening parenthesis `(`
* The `+` and `-` keys switch between unary signs and binary arithmetic operators
* The delete key switches between Backspace and Clear, depending on the input state
* Long-press the Backspace key to clear the current expression
* Switch between the main keypad and the scientific function keypad
* Long-press scientific function keys to access corresponding alternative functions, such as switching between `sin` and `asin`, `cos` and `acos`, or `tan` and `atan`

### Display

* Locale-aware thousands and decimal separators
* Text size adjusts automatically to fit the available display area
* Long expressions remain accessible through horizontal scrolling
* A calculation progress indicator is displayed while calculating, with an option to cancel the calculation

### Calculation history

* Tap a previous calculation to reuse its result
* When an expression is being entered, the selected result is appended to the current input
* Delete individual entries or clear the entire history
* Long-press a history entry to enter deletion mode
* Independent horizontal scrolling for lengthy expressions and results within history items

### Settings

* Adjust calculation precision from 8 to 64 significant digits

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

The scientific keypad also provides squaring, powers of ten, the exponential function, and inverse trigonometric functions.

* Tap the `RAD` / `DEG` key on the scientific keypad to toggle between radian and degree angle modes.
* Long-press a scientific function key to access its alternative function where available. For example, long-press `sin` to enter `asin`.
* **Precision and rounding:** Calculations involving irrational constants and transcendental functions (such as `ln(e)`) are subject to the configured precision. At certain settings (such as 8 or 16 digits), results may display slight rounding discrepancies (e.g., `0.999999...` instead of an exact `1`) due to finite series approximations.

### Deleting and clearing

* While entering an expression, the delete key works as Backspace.
* When no expression is active, the delete key switches to Clear.
* Long-press the Backspace key at any time to instantly clear the entire expression.

### Managing calculation history

Tap the expression display at the top of the calculator to open calculation history.

From the history screen, you can:

* **Reuse result:** Tap a history item to paste its result into the current input.
* **Deletion mode:** Long-press any item to enter deletion mode. Tap individual items to delete them, or tap Delete all at the bottom.
* **Return:** Swipe from the left edge, or press the undo button.

The history is stored locally on the watch using Jetpack DataStore and is limited to 50 entries.

### Changing settings

Long-press the expression display at the top of the calculator to open settings.

## Supported devices

* Platform: Wear OS
* Minimum SDK: Android API 30 (Wear OS 3.0+)
* Target SDK: Android API 37
* Form factor: Optimized for circular smartwatch displays, including rotary/crown hardware controls.

## Technology stack

* Language: Kotlin
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
* Arbitrary-precision decimal arithmetic via `BigDecimal` using `HALF_UP` rounding
* Custom implementations of square root, exponentiation, factorial, trigonometric functions, logarithms, and mathematical constants

### Calculation limits

To keep calculations and displayed results within defined limits:

* Factorial arguments: Non-negative integers up to 253
* Integer exponents: Absolute value of 1024 or less
* Non-integer exponents: Absolute value of 1024 or less
* Nesting depth: Up to 64 nested levels of parentheses and function calls
* Result size: Limited to 512 characters when represented in plain decimal notation
* Expression length: Up to 1024 input symbols

Invalid operations (such as division by zero or negative arguments for square roots/logarithms) are gracefully handled and reported via subtle tactile and visual error messages.

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
