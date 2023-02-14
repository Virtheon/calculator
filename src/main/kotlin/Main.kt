/*
Copyright 2023 Henrique Aguiar

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.math.BigDecimal
import java.math.RoundingMode

data class ButtonValues(val buttonText: String, val onClick: () -> Unit)
data class Operator(val operatorStr: String, val operationFun: (BigDecimal, BigDecimal) -> BigDecimal)
// TODO add flag for "= " before number
data class FocusNumberAndFlags(
	val number: BigDecimal,
	val isNegative: Boolean = (number < 0.bd),
	val isDecimal: Boolean = (number.scale() > 0)
)

val Int.bd: BigDecimal
	get() = this.toBigDecimal()

// TODO dynamically calculate based on how much screen is available or how many digits the number has total
const val MAX_DECIMAL_DIGITS = 11 // based on how many fit on the screen

@Composable
@Preview
fun App() {
	MaterialTheme() {
		var focusNumAndFlags by remember { mutableStateOf(FocusNumberAndFlags(0.bd)) }
		// TODO change previous number and operator to expression
		var previousNumber: BigDecimal? by remember { mutableStateOf(null) }
		var operator: Operator? by remember { mutableStateOf(null) }

		fun evaluateExpression() =
			if (previousNumber != null && operator != null)
				operator!!.operationFun(
					previousNumber!!,
					focusNumAndFlags.number
				).stripTrailingZeros() // to prevent for example 5.1 + 0.9 = 6.0
			else
				focusNumAndFlags.number

		fun digitOnClick(x: Int): () -> Unit = {
			val signedX = (if (focusNumAndFlags.isNegative) -x else x).bd
			val ten = 10.bd
			val newNumber =
				if (focusNumAndFlags.number == 0.bd && !focusNumAndFlags.isDecimal)
					signedX
				else if (focusNumAndFlags.isDecimal && focusNumAndFlags.number.scale() < MAX_DECIMAL_DIGITS)
					focusNumAndFlags.number + signedX.scaleByPowerOfTen(-(focusNumAndFlags.number.scale() + 1))
				else
					focusNumAndFlags.number * ten + signedX
			focusNumAndFlags = FocusNumberAndFlags(newNumber)
		}

		fun operationOnClick(
			operatorSymbol: String,
			operatorFunction: (BigDecimal, BigDecimal) -> BigDecimal
		): (() -> Unit) = {
			previousNumber = evaluateExpression()
			focusNumAndFlags = FocusNumberAndFlags(0.bd)
			operator = Operator(operatorSymbol, operatorFunction)
		}

		Column {
			val possibleExtraNegative =
				if (focusNumAndFlags.isNegative && focusNumAndFlags.number.compareTo(0.bd) == 0)
					"-"
				else
					""
			val possibleExtraDot =
				if (focusNumAndFlags.isDecimal && focusNumAndFlags.number.scale() == 0)
					"."
				else
					""
			val previousNumberAndOperatorStr =
				if (previousNumber != null && operator != null)
					previousNumber.toString() + " " + operator!!.operatorStr
				else
					""
			val currentNumberStr = possibleExtraNegative + focusNumAndFlags.number.toString() + possibleExtraDot

			Text(
				previousNumberAndOperatorStr,
				color = Color.Gray,
				fontSize = 30.sp,
				modifier = Modifier.padding(start = 10.dp)
			)
			Text(
				currentNumberStr,
				fontSize = 50.sp,
				textAlign = TextAlign.End,
				modifier = Modifier.fillMaxWidth().padding(end = 15.dp)
			)
			// TODO add new row with exponent, root, factorial and delete button
			Row {
				NumberPad(
					Modifier.weight(3f),
					listOf(
						ButtonValues("1", digitOnClick(1)),
						ButtonValues("2", digitOnClick(2)),
						ButtonValues("3", digitOnClick(3)),
						ButtonValues("4", digitOnClick(4)),
						ButtonValues("5", digitOnClick(5)),
						ButtonValues("6", digitOnClick(6)),
						ButtonValues("7", digitOnClick(7)),
						ButtonValues("8", digitOnClick(8)),
						ButtonValues("9", digitOnClick(9)),
						ButtonValues("+/-") {
							focusNumAndFlags = FocusNumberAndFlags(
								-focusNumAndFlags.number,
								isNegative = !focusNumAndFlags.isNegative,
								isDecimal = focusNumAndFlags.isDecimal
							)
						},
						ButtonValues("0", digitOnClick(0)),
						ButtonValues(".") {
							focusNumAndFlags =
								if (focusNumAndFlags.isDecimal)
									FocusNumberAndFlags(
										focusNumAndFlags.number.setScale(0, RoundingMode.DOWN),
										isNegative = focusNumAndFlags.isNegative
									)
								else
									FocusNumberAndFlags(
										focusNumAndFlags.number,
										isNegative = focusNumAndFlags.isNegative,
										isDecimal = true
									)
						},
					)
				)
				OperatorColumn(
					Modifier.weight(1f),
					listOf(
						ButtonValues("+", operationOnClick("+") { x, y -> x + y }),
						ButtonValues("-", operationOnClick("-") { x, y -> x - y }),
						ButtonValues("×", operationOnClick("×") { x, y -> x * y }),
						ButtonValues("÷",
							operationOnClick("÷") { x, y ->
								x.divide(y, MAX_DECIMAL_DIGITS, RoundingMode.HALF_UP).stripTrailingZeros()
							}
						),
						ButtonValues("=") {
							focusNumAndFlags = FocusNumberAndFlags(evaluateExpression())
							previousNumber = null
							operator = null
						}
					)
				)
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPad(modifier: Modifier, buttons: List<ButtonValues>) {
	LazyVerticalGrid(
		modifier = modifier.fillMaxHeight(),
		cells = GridCells.Fixed(3),
	) {
		items(buttons.size) { index ->
			CalculatorButton(
				buttons[index].buttonText,
				buttons[index].onClick,
				// only way i could find to make buttons take up all the screen
				Modifier.fillParentMaxHeight(1f / (buttons.size / 3))
			)
		}
	}
}

@Composable
fun OperatorColumn(modifier: Modifier, buttons: List<ButtonValues>) {
	Column(
		modifier = modifier,
	) {
		buttons.forEach {
			CalculatorButton(it.buttonText, it.onClick, Modifier.weight(1f))
		}
	}
}

@Composable
fun CalculatorButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
	Button(
		modifier = modifier.fillMaxSize().padding(7.dp),
		colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
		onClick = onClick
	) {
		Text(text, fontSize = 30.sp)
	}
}

fun main() = application {
	Window(
		onCloseRequest = ::exitApplication,
		state = rememberWindowState(width = 400.dp, height = 600.dp),
		title = "Calculator",
	) {
		App()
	}
}
