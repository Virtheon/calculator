// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.math.BigDecimal
import java.math.RoundingMode

data class ButtonValues(val buttonText: String, val onClick: () -> Unit)
data class Operator(val operatorStr: String, val operationFun: (BigDecimal, BigDecimal) -> BigDecimal)

data class CurrentNumberValues(
	val value: BigDecimal,
	val isNegative: Boolean = (value < 0.bd),
	val isDecimal: Boolean = (value.scale() > 0)
)

val Int.bd: BigDecimal
	get() = this.toBigDecimal()

@Composable
@Preview
fun App() {
	MaterialTheme() {
		// FIXME can't do 0.
		var currentNumberInformation by remember { mutableStateOf(CurrentNumberValues(0.bd)) }
		var previousNumber: BigDecimal? by remember { mutableStateOf(null) }
		var operator: Operator? by remember { mutableStateOf(null) }

		fun digitOnClick(x: Int): () -> Unit = {
			val signedX = (if (currentNumberInformation.isNegative) -x else x).bd
			val ten = 10.bd
			val newNumber =
				if (currentNumberInformation.value == 0.bd)
					signedX
				else if (currentNumberInformation.isDecimal)
					currentNumberInformation.value + signedX.scaleByPowerOfTen(-(currentNumberInformation.value.scale() + 1))
				else
					currentNumberInformation.value * ten + signedX
			currentNumberInformation = CurrentNumberValues(newNumber)
		}

		fun operationOnClick(
			operatorSymbol: String,
			operatorFunction: (BigDecimal, BigDecimal) -> BigDecimal
		): (() -> Unit) = {
			// TODO perform operation of operator is selected a second time
			previousNumber = currentNumberInformation.value
			currentNumberInformation = CurrentNumberValues(0.bd)
			operator = Operator(operatorSymbol, operatorFunction)
		}

		Column {
			val possibleExtraNegative =
				if (currentNumberInformation.isNegative && currentNumberInformation.value.compareTo(0.bd) == 0)
					"-"
				else
					""
			val possibleExtraDot =
				if (currentNumberInformation.isDecimal && currentNumberInformation.value.scale() == 0)
					"."
				else
					""
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
				possibleExtraNegative + currentNumberInformation.value.toString() + possibleExtraDot,
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
							currentNumberInformation = CurrentNumberValues(-currentNumberInformation.value)
						},
						ButtonValues("0", digitOnClick(0)),
						ButtonValues(".") {
							currentNumberInformation =
								if (currentNumberInformation.isDecimal)
									CurrentNumberValues(
										currentNumberInformation.value.setScale(0, RoundingMode.DOWN)
									)
								else
									CurrentNumberValues(
										currentNumberInformation.value,
										isDecimal = true
									)
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
								currentNumberInformation = CurrentNumberValues(
									operator!!.operationFun(
										previousNumber!!,
										currentNumberInformation.value
									).stripTrailingZeros()
								)
							}
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
