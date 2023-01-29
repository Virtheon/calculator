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
import java.math.MathContext
import java.math.RoundingMode

data class ButtonValues(val buttonText: String, val onClick: () -> Unit)

@Composable
@Preview
fun App() {
	MaterialTheme() {
		var mathContext by remember { mutableStateOf(MathContext(0)) }
		var currentNumber by remember { mutableStateOf(0.toBigDecimal(mathContext)) }
		var isDecimal by remember { mutableStateOf(false) }
		var isNegative by remember { mutableStateOf(false) }
		fun generateNumberOnClick(x: Int): () -> Unit = {
			// FIXME negative decimals
			// FIXME -0?
			val signedX = (if (isNegative) -x else x).toBigDecimal()
			val ten = 10.toBigDecimal()
			currentNumber =
				if (currentNumber == 0.toBigDecimal())
					signedX
				else if (isDecimal)
					currentNumber + signedX.scaleByPowerOfTen(-(currentNumber.scale() + 1))
				else
					currentNumber * ten + signedX
		}
		Column {
			val possibleExtraNegative = if (isNegative && currentNumber.compareTo(0.toBigDecimal()) == 0) "-" else ""
			val possibleExtraDot = if (isDecimal && currentNumber.scale() == 0) "." else ""
			Text(possibleExtraNegative + currentNumber.toString() + possibleExtraDot, fontSize = 50.sp)
			NumberPad(
				listOf(
					ButtonValues("1", generateNumberOnClick(1)),
					ButtonValues("2", generateNumberOnClick(2)),
					ButtonValues("3", generateNumberOnClick(3)),
					ButtonValues("4", generateNumberOnClick(4)),
					ButtonValues("5", generateNumberOnClick(5)),
					ButtonValues("6", generateNumberOnClick(6)),
					ButtonValues("7", generateNumberOnClick(7)),
					ButtonValues("8", generateNumberOnClick(8)),
					ButtonValues("9", generateNumberOnClick(9)),
					ButtonValues("+/-") {
						currentNumber = -currentNumber
						isNegative = !isNegative
					},
					ButtonValues("0", generateNumberOnClick(0)),
					ButtonValues(".") {
						isDecimal = !isDecimal
						if (!isDecimal) {
							currentNumber = currentNumber.setScale(0, RoundingMode.DOWN)
						}
					},
				)
			)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPad(buttons: List<ButtonValues>) {
	LazyVerticalGrid(
		modifier = Modifier.fillMaxWidth(0.5f),
		horizontalArrangement = Arrangement.SpaceEvenly,
		cells = GridCells.Fixed(3)
	) {
		items(buttons.size) { index ->
			Button(
				modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp).aspectRatio(1f),
				colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
				onClick = buttons[index].onClick
			) {
				Text(buttons[index].buttonText)
			}
		}
	}
}

fun main() = application {
	Window(onCloseRequest = ::exitApplication) {
		App()
	}
}
