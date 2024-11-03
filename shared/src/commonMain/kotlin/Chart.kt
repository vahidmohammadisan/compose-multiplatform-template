import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
)

@Composable
fun CandlestickChart(
    candles: List<CandleData>,
    modifier: Modifier = Modifier,
    bullColor: Color = Color.Green,
    bearColor: Color = Color.Red,
    initialCandleWidth: Float = 40f,
    candleSpacing: Float = 4f
) {
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val density = LocalDensity.current

    var candleWidth by remember { mutableStateOf(initialCandleWidth) }
    var selectedCandle by remember { mutableStateOf<CandleData?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    // Fixed dimensions
    val priceAxisWidth = 60.dp
    val timeAxisHeight = 30.dp
    val chartHeight = 300.dp

    // Calculate total content width in pixels first
    val contentWidth = with(density) {
        (candleWidth + candleSpacing) * candles.size + priceAxisWidth.toPx()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + timeAxisHeight)
    ) {
        // Price axis (vertical)
        if (candles.isNotEmpty()) {
            val maxPrice = candles.maxOf { it.high }
            val minPrice = candles.minOf { it.low }
            val priceStep = (maxPrice - minPrice) / 5

            Column(
                modifier = Modifier
                    .width(priceAxisWidth)
                    .height(chartHeight)
                    .padding(end = 4.dp)
            ) {
                repeat(6) { i ->
                    val price = maxPrice - (i * priceStep)
                    Text(
                        text = "%.1f".format(price),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        style = TextStyle(
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        ),
                        maxLines = 1
                    )
                    if (i < 5) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Scrollable chart container
        Column(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight + timeAxisHeight)
        ) {
            // Chart area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(with(density) { contentWidth.toDp() })
                        .height(chartHeight)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                candleWidth = (candleWidth * zoom).coerceIn(10f, 100f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { position ->
                                val index =
                                    ((position.x - candleSpacing) / (candleWidth + candleSpacing)).toInt()
                                if (index in candles.indices) {
                                    selectedCandle = candles[index]
                                    touchPosition = position
                                }
                            }
                        }
                ) {
                    if (candles.isNotEmpty()) {
                        val maxPrice = candles.maxOf { it.high }
                        val minPrice = candles.minOf { it.low }
                        val priceRange = maxPrice - minPrice
                        val heightPerPrice = size.height / priceRange

                        // Draw background grid (optional)
                        drawRect(Color(0xFF1E1E1E))

                        candles.forEachIndexed { index, candle ->
                            val x = index * (candleWidth + candleSpacing) + candleSpacing
                            val color = if (candle.close >= candle.open) bullColor else bearColor

                            // Draw candle body
                            val top = (maxPrice - maxOf(candle.open, candle.close)) * heightPerPrice
                            val bottom =
                                (maxPrice - minOf(candle.open, candle.close)) * heightPerPrice

                            drawRect(
                                color = color,
                                topLeft = Offset(x, top),
                                size = androidx.compose.ui.geometry.Size(
                                    candleWidth,
                                    (bottom - top).coerceAtLeast(1f)
                                )
                            )

                            // Draw wicks
                            val centerX = x + candleWidth / 2
                            drawLine(
                                color = color,
                                start = Offset(centerX, (maxPrice - candle.high) * heightPerPrice),
                                end = Offset(centerX, top)
                            )
                            drawLine(
                                color = color,
                                start = Offset(centerX, bottom),
                                end = Offset(centerX, (maxPrice - candle.low) * heightPerPrice)
                            )
                        }
                    }
                }
            }

            // Time axis
            Box(
                modifier = Modifier
                    .height(timeAxisHeight)
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(with(density) { contentWidth.toDp() })
                        .fillMaxHeight()
                ) {
                    if (candles.isNotEmpty()) {
                        val textLayoutResult = textMeasurer.measure(
                            text = dateFormatter.format(Date()),
                            style = TextStyle(fontSize = 10.sp)
                        )
                        val timeStep = maxOf(
                            1,
                            (candles.size / (size.width / (textLayoutResult.size.width + 20))).toInt()
                        )

                        candles.forEachIndexed { index, candle ->
                            if (index % timeStep == 0) {
                                val x = index * (candleWidth + candleSpacing) + candleSpacing
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(x + candleWidth / 2, 0f),
                                    end = Offset(x + candleWidth / 2, 5f)
                                )

                                val text = dateFormatter.format(Date(candle.timestamp))
                                val textLayout = textMeasurer.measure(
                                    text = text,
                                    style = TextStyle(fontSize = 10.sp)
                                )
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(
                                        x + (candleWidth - textLayout.size.width) / 2,
                                        10f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCandlestickScreen() {
    val sampleData = remember {
        val calendar = Calendar.getInstance()
        List(100) { index ->
            calendar.add(Calendar.MINUTE, 5)
            val basePrice = 100f + index * 2
            CandleData(
                timestamp = calendar.timeInMillis,
                open = basePrice + (-5f..5f).random(),
                high = basePrice + (5f..15f).random(),
                low = basePrice + (-15f..-5f).random(),
                close = basePrice + (-5f..5f).random()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CandlestickChart(
            candles = sampleData,
            modifier = Modifier.fillMaxWidth(),
            initialCandleWidth = 20f,
            candleSpacing = 4f
        )
    }
}

fun ClosedRange<Float>.random() =
    Random().nextFloat() * (endInclusive - start) + start