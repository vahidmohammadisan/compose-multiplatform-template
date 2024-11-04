import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random

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
    bullColor: Color = Color(0xFF4CAF50),
    bearColor: Color = Color(0xFFE53935),
    initialCandleWidth: Float = 40f,
    candleSpacing: Float = 4f
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var candleWidth by remember { mutableStateOf(initialCandleWidth) }
    var selectedCandle by remember { mutableStateOf<CandleData?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    val priceAxisWidth = 60.dp
    val timeAxisHeight = 30.dp
    val chartHeight = 300.dp

    val contentWidth = with(density) {
        (candleWidth + candleSpacing) * candles.size + priceAxisWidth.toPx()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + timeAxisHeight)
    ) {
        // Price axis
        if (candles.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .width(priceAxisWidth)
                    .height(chartHeight)
                    .padding(end = 4.dp)
            ) {
                val maxPrice = candles.maxOf { it.high }
                val minPrice = candles.minOf { it.low }
                val priceStep = (maxPrice - minPrice) / 5

                repeat(6) { i ->
                    val price = maxPrice - (i * priceStep)
                    Text(
                        text = price.toFormattedString(1),
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

        // Chart container
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
                                val index = ((position.x - candleSpacing) / (candleWidth + candleSpacing)).toInt()
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

                        // Background
                        drawRect(Color(0xFF1E1E1E))

                        // Draw candles
                        candles.forEachIndexed { index, candle ->
                            val x = index * (candleWidth + candleSpacing) + candleSpacing
                            val color = if (candle.close >= candle.open) bullColor else bearColor

                            // Candle body
                            val top = (maxPrice - maxOf(candle.open, candle.close)) * heightPerPrice
                            val bottom = (maxPrice - minOf(candle.open, candle.close)) * heightPerPrice

                            drawRect(
                                color = color,
                                topLeft = Offset(x, top),
                                size = Size(
                                    candleWidth,
                                    (bottom - top).coerceAtLeast(1f)
                                )
                            )

                            // Wicks
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

            // Time axis with Box layout instead of Canvas for text
            Box(
                modifier = Modifier
                    .height(timeAxisHeight)
                    .horizontalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .width(with(density) { contentWidth.toDp() })
                        .fillMaxHeight()
                ) {
                    if (candles.isNotEmpty()) {
                        candles.forEach { candle ->
                            Box(
                                modifier = Modifier
                                    .width(with(density) { (candleWidth + candleSpacing).toDp() })
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val instant = Instant.fromEpochMilliseconds(candle.timestamp)
                                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                val timeText = "${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"

                                Text(
                                    text = timeText,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
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
        val now = Clock.System.now()
        // Use Duration.minutes from kotlin.time
        val initialInstant = now.minus(500,DateTimeUnit.MINUTE)

        List(100) { index ->
            // Use Instant.plus with DateTimeUnit
            val timestamp = initialInstant.plus(5 * index, DateTimeUnit.MINUTE)
            val basePrice = 100f + index * 2
            CandleData(
                timestamp = timestamp.toEpochMilliseconds(),
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

// Extension function for random float generation
fun ClosedRange<Float>.random(): Float =
    Random.nextFloat() * (endInclusive - start) + start

// Extension function to calculate power of a Float
fun Float.pow(exp: Int): Float {
    var result = 1.0f
    repeat(exp) {
        result *= this
    }
    return result
}

fun Float.toFormattedString(digits: Int): String {
    val factor = 10.0f.pow(digits) // Use extension function to calculate the power
    return ((this * factor).toInt() / factor).toString() // Truncate instead of rounding
}