// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class ButtonValues(val buttonText: String, val onClick: () -> Unit)
data class Operator(val operatorStr: String, val operationFun: (BigDecimal, BigDecimal) -> BigDecimal)

val Int.bd: BigDecimal
	get() = this.toBigDecimal()

@Composable
@Preview
fun App() {
	MaterialTheme() {
		var mathContext by remember { mutableStateOf(MathContext(0)) }
		// TODO pack all this into one variable?
		var currentNumber by remember { mutableStateOf(0.toBigDecimal(mathContext)) }
		var isDecimal by remember { mutableStateOf(false) }
		var isNegative by remember { mutableStateOf(false) }
		var previousNumber: BigDecimal? by remember { mutableStateOf(null) }
		var operator: Operator? by remember { mutableStateOf(null) }

		fun digitOnClick(x: Int): () -> Unit = {
			val signedX = (if (isNegative) -x else x).bd
			val ten = 10.bd
			currentNumber =
				if (currentNumber == 0.bd)
					signedX
				else if (isDecimal)
					currentNumber + signedX.scaleByPowerOfTen(-(currentNumber.scale() + 1))
				else
					currentNumber * ten + signedX
		}

		fun operationOnClick(
			operatorSymbol: String,
			operatorFunction: (BigDecimal, BigDecimal) -> BigDecimal
		): (() -> Unit) = {
			// TODO perform operation of operator is selected a second time
			previousNumber = currentNumber
			currentNumber = 0.bd
			operator = Operator(operatorSymbol, operatorFunction)
			isDecimal = false
			isNegative = false
		}

		Column {
			val possibleExtraNegative = if (isNegative && currentNumber.compareTo(0.bd) == 0) "-" else ""
			val possibleExtraDot = if (isDecimal && currentNumber.scale() == 0) "." else ""
			val topText =
				if (previousNumber != null && operator != null)
					previousNumber.toString() + " " + operator!!.operatorStr
				else
					""

			Text(
				topText,
				color = Color.Gray,
				fontSize = 30.sp
			)
			Text(
				possibleExtraNegative + currentNumber.toString() + possibleExtraDot,
				fontSize = 50.sp,
				modifier = Modifier.fillMaxWidth()
			)
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
							currentNumber = -currentNumber
							isNegative = !isNegative
						},
						ButtonValues("0", digitOnClick(0)),
						ButtonValues(".") {
							isDecimal = !isDecimal
							if (!isDecimal) {
								currentNumber = currentNumber.setScale(0, RoundingMode.DOWN)
							}
						},
					)
				)
				Operators(
					Modifier.weight(1f),
					listOf(
						ButtonValues("+", operationOnClick("+") { x, y -> x + y }),
						ButtonValues("-", operationOnClick("-") { x, y -> x - y }),
						ButtonValues("×", operationOnClick("×") { x, y -> x * y }),
						ButtonValues("÷", operationOnClick("÷") { x, y -> x / y }),
						ButtonValues("=") {
							if (previousNumber != null && operator != null) {
								currentNumber = operator!!.operationFun(previousNumber!!, currentNumber)
							}
							previousNumber = null
							operator = null
							isDecimal = false
							isNegative = false
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
fun Operators(modifier: Modifier, buttons: List<ButtonValues>) {
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
