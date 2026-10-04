# Constants in `CalculatorScreen.kt`

This document describes how the following constants used in `CalculatorScreen.kt` are derived:

* `INSCRIBED_LENGTH_RATIO`
* `PANEL_WIDTH_RATIO`
* `KEY_SWITCHER_LENGTH_RATIO`

## Assumptions

![Geometry used to derive the display and panel ratios](fig/calculator-screen-constants.png)

* The display area is a circle with diameter `d`.
* Quadrilateral `ABCD` is a square inscribed in the display area.
* Quadrilateral `EFGH` has vertices `E` and `H` on the circumference of the display area.
* Sides `GF` and `BC` lie on the same line.
* The ratio of the side length of square `ABCD` to `EF` (or `GH`) is 5:4.
* Point `P` is the foot of the perpendicular from `E` to the horizontal line passing through the center `O`.
* Quadrilateral `IJKL` is a square and has vertices `J` and `K` on the circumference of the display area.
* Sides `IL` and `BC` lie on the same line.
* Point `Q` and `R` are the feet of the perpendiculars from `I` and `J`, respectively, to the horizontal line passing through the center `O`.

`INSCRIBED_LENGTH_RATIO` represents the ratio of the side length `AB` of the inscribed square `ABCD` to the diameter `d` of the display area.

`PANEL_WIDTH_RATIO` represents the ratio of the side length `EH` to the diameter `d` of the display area.

`KEY_SWITCHER_LENGTH_RATIO` represents the ratio of the side length `IL` of the square `IJKL` to the diameter `d` of the display area.

## Quadrilateral ABCD

`ABCD` is a square, and its diagonal `BD` is equal to the diameter `d` of the display area.

For a square, the diagonal is $\sqrt{2}$ times the side length:

$$
\begin{aligned}
BD = \sqrt{ 2 } \cdot AD\\
\end{aligned}
$$

Therefore,

$$
\begin{aligned}
AD = \frac{ 1 } { \sqrt{ 2 } } d\\
\end{aligned}
$$

Since all sides of `ABCD` have the same length,

$$
\begin{aligned}
AB = BC = CD = AD = \frac{ 1 } { \sqrt{ 2 } } d\\
\end{aligned}
$$

Thus,

$$
\begin{aligned}
INSCRIBED\\_LENGTH\\_RATIO = \frac{ 1 } { \sqrt{ 2 } } \approx 0.70710678118654752440084436210485...\\
\end{aligned}
$$

## Quadrilateral EFGH

The side lengths of `ABCD` and `EFGH` satisfy

$$
\begin{aligned}
EF = \frac{ 4 }{ 5 }AB\\
\end{aligned}
$$

First, calculate the vertical distance `EP`.

From the geometry shown in the diagram,

$$
\begin{aligned}
EP = \frac{ 1 }{ 2 } AB - ( AB - EF )\\
\end{aligned}
$$

Therefore,

$$
\begin{aligned}
EP &= \frac{ 1 }{ 2 } AB - AB + EF\\
&= -\frac{ 1 }{ 2 } AB + \frac{ 4 }{ 5 } AB\\
&= \frac{ 3 }{ 10 } AB\\
&= \frac{ 3 }{ 10 } \cdot \frac{ 1 } { \sqrt{ 2 } } d\\
&= \frac{ 3 }{ 10 \sqrt{ 2 } } d\\
\end{aligned}
$$

Since `E` lies on the circumference of the display area,

$$
\begin{aligned}
OE = \frac{ 1 }{ 2 } d\\
\end{aligned}
$$

Triangle `OPE` is a right triangle. Applying the Pythagorean theorem,

$$
\begin{aligned}
OP &= \sqrt{ OE^2 - EP^2 }\\
&= \sqrt{ ( \frac{ 1 }{ 2 } d )^2 - ( \frac{ 3 }{ 10 \sqrt{ 2 } } d )^2 }\\
&= \sqrt{ \frac{ 1 }{ 4 } d^2 - \frac{ 9 }{ 200 } d^2 }\\
&= \sqrt{ \frac{ 41 }{ 200 } } d\\
\end{aligned}
$$

Since `O` is the center of the circle and `OP` is perpendicular to chord `EH`, `OP` bisects `EH`, so

$$
\begin{aligned}
EH = 2 \cdot OP\\
\end{aligned}
$$

Therefore,

$$
\begin{aligned}
EH &= 2\sqrt{ \frac{ 41 }{ 200 } } d\\
&= \sqrt{ \frac{ 41 }{ 50 } } d\\
\end{aligned}
$$

Thus,

$$
\begin{aligned}
PANEL\\_WIDTH\\_RATIO = \sqrt{ \frac{ 41 }{ 50 } } \approx 0.90553851381374166265738081669841...\\
\end{aligned}
$$

## Quadrilateral IJKL

Triangle `ORJ` is a right triangle. Applying the Pythagorean theorem,

$$
\begin{aligned}
OR^2 + JR^2 &= OJ^2\\
(OQ + QR)^2 + JR^2 &= OJ^2\\
\end{aligned}
$$

Since `J` lies on the circumference of the display area,

$$
\begin{aligned}
OJ = \frac{ 1 }{ 2 } d\\
\end{aligned}
$$

From the calculation above, `OQ` is given by

$$
\begin{aligned}
OQ &= \frac{ 1 }{ 2 } AB\\
 &= \frac{ 1 } { 2 \sqrt{ 2 } } d\\
\end{aligned}
$$

Also, since `JR` is half the length of `QR`, we have

$$
\begin{aligned}
(\frac{ 1 } { 2 \sqrt{ 2 } } d + QR)^2 + (\frac{ 1 }{ 2 } QR)^2 &= (\frac{ 1 }{ 2 } d)^2\\
\end{aligned}
$$

Solving this equation for `QR` gives

$$
\begin{aligned}
QR = \frac { -2 \sqrt{ 2 } \pm 3 \sqrt{ 2 } }{ 10 } d\\
\end{aligned}
$$

Since `QR` is positive,

$$
\begin{aligned}
QR &= \frac { -2 \sqrt{ 2 } + 3 \sqrt{ 2 } }{ 10 } d\\
 &= \frac { \sqrt{ 2 } }{ 10 } d\\
\end{aligned}
$$

`QR` has the same length as `IJ`, and all sides of `IJKL` have the same length,

Therefore,

$$
\begin{aligned}
QR = IJ = JK = KL = IL = \frac { \sqrt{ 2 } }{ 10 }d\\
\end{aligned}
$$

Thus,

$$
\begin{aligned}
KEY\\_SWITCHER\\_LENGTH\\_RATIO = \frac { \sqrt{ 2 } }{ 10 } \approx 0.14142135623730950488016887242097...\\
\end{aligned}
$$


## Summary

The mathematical values of the ratios are:

$$
\begin{aligned}
\text{INSCRIBED\_LENGTH\_RATIO} &= \frac{ 1 } { \sqrt{ 2 } } \approx 0.70710678118654752440084436210485...\\
PANEL\\_WIDTH\\_RATIO &= \sqrt{ \frac{ 41 }{ 50 } } \approx 0.90553851381374166265738081669841...\\
KEY\\_SWITCHER\\_LENGTH\\_RATIO &= \frac { \sqrt{ 2 } }{ 10 } \approx 0.14142135623730950488016887242097...\\
\end{aligned}
$$

The application stores these values as `Float`, so the values are rounded to `Float` precision in
the source code:

```kotlin
const val INSCRIBED_LENGTH_RATIO: Float = 0.70710677f
const val PANEL_WIDTH_RATIO: Float = 0.9055385f
const val KEY_SWITCHER_LENGTH_RATIO: Float = 0.14142136f
```
