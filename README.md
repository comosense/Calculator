# Calculator

A simple calculator app for Wear OS.

It is designed with a minimal UI that makes basic calculations easy to perform on a small smartwatch display.

## Features

- Basic arithmetic operations (`+`, `-`, `×`, `÷`)
- Decimal numbers
- Parentheses
- Positive and negative numbers
- Calculation history
- Reuse results from calculation history
- Delete individual history items or clear all history
- Error handling for invalid expressions and division by zero
- Thousands separators for easier reading of large numbers

## Example

The calculator supports expressions such as:

```text
1 + 2 × 3

(1 + 2) × 3

-10 ÷ 4
```

Results are displayed with up to 20 decimal places.

## Usage

### Basic Calculation

Tap the keys on the screen to enter an expression.

- `C` — Clear the current input
- `←` — Delete the last input
- `=` — Calculate the expression
- `+`, `-`, `×`, `÷` — Arithmetic operators
- `(`, `)` — Parentheses
- `.` — Decimal point

> [!TIP]
> Long-pressing the `←` key clears the current input.

The `+`, `-`, and parentheses keys automatically switch between their available functions depending on the current input.

The calculator uses `BigDecimal` for numerical calculations.

It supports standard operator precedence. For example:

```text
1 + 2 × 3
```

produces:

```text
7
```

The calculation precision is set to 50 digits. Results are rounded to a maximum of 20 decimal places for display.

### History

Tap the history button at the top of the calculator to view previous calculations.

Tap a history item to reuse its result as the current input.

Long-press a history item to enter delete mode. You can then delete individual items or use Delete All to clear the entire history.

The history is limited to 50 entries.

## Tech Stack

- Kotlin
- Android
- Wear OS
- Jetpack Compose
- Wear Compose Material 3
- `BigDecimal` / `MathContext`

The calculation engine converts the input expression into tokens and evaluates them using a simple recursive-descent parser.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.

IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

This app is provided for general-purpose calculations and personal use.

While reasonable care has been taken to ensure the correctness of the calculation logic, no guarantee is made regarding the accuracy or suitability of the results for any particular purpose.

Do not rely on this app for critical, financial, medical, scientific, or other high-stakes calculations. Always verify important results using an appropriate and reliable calculator or calculation tool.
