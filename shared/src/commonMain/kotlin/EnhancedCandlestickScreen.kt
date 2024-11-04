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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


// First, let's modify the CandleData class to include volume
data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float,
    val ma20: Float? = null  // Moving average for line chart
)

data class VisibleRange(
    val startIndex: Int,
    val endIndex: Int,
    val maxPrice: Float,
    val minPrice: Float
)

@Composable
fun EnhancedCandlestickChart(
    candles: List<CandleData>,
    modifier: Modifier = Modifier,
    bullColor: Color = Color(0xFF4CAF50),
    bearColor: Color = Color(0xFFE53935),
    lineColor: Color = Color(0xFFFFEB3B),
    volumeUpColor: Color = Color(0x804CAF50),  // Semi-transparent green
    volumeDownColor: Color = Color(0x80E53935), // Semi-transparent red
    initialCandleWidth: Float = 40f,
    candleSpacing: Float = 4f
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var candleWidth by remember { mutableStateOf(initialCandleWidth) }
    var selectedCandle by remember { mutableStateOf<CandleData?>(null) }
    var visibleRange by remember { mutableStateOf<VisibleRange?>(null) }

    val priceAxisWidth = 80.dp
    val timeAxisHeight = 50.dp
    val chartHeight = 300.dp
    val volumeHeight = 100.dp
    val volumeAxisWidth = 80.dp

    val priceAxisWidthPx = with(density) { priceAxisWidth.toPx() }
    val candleFullWidth = candleWidth + candleSpacing
    val contentWidthPx = candleFullWidth * candles.size + priceAxisWidthPx
    val viewportWidthPx = with(density) { (LocalDensity.current.density * 360f) }

    LaunchedEffect(scrollState.value, candleWidth, candles.size) {
        if (candles.isEmpty()) return@LaunchedEffect

        val visibleCandlesCount = ((viewportWidthPx - priceAxisWidthPx) / candleFullWidth).toInt()
        val scrollOffset = scrollState.value.toFloat()
        val startIndex = max(0, (scrollOffset / candleFullWidth).toInt())
        val endIndex = min(candles.size - 1, startIndex + visibleCandlesCount)

        val visibleCandles = candles.subList(startIndex, endIndex + 1)
        val maxPrice = visibleCandles.maxOf { it.high }
        val minPrice = visibleCandles.minOf { it.low }

        visibleRange = VisibleRange(startIndex, endIndex, maxPrice, minPrice)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + volumeHeight + timeAxisHeight)
        ) {
            // Price and Volume Axes Column
            Column(
                modifier = Modifier
                    .width(priceAxisWidth)
                    .fillMaxHeight()
            ) {
                // Price Axis
                if (candles.isNotEmpty() && visibleRange != null) {
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .padding(end = 4.dp)
                    ) {
                        val maxPrice = visibleRange!!.maxPrice
                        val minPrice = visibleRange!!.minPrice
                        val priceStep = (maxPrice - minPrice) / 8

                        repeat(9) { i ->
                            val price = maxPrice - (i * priceStep)
                            Text(
                                text = price.toFormattedString(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                    color = Color.Blue
                                ),
                                maxLines = 1
                            )
                            if (i < 8) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // Volume Axis
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        if (visibleRange != null) {
                            val visibleCandles = candles.subList(
                                visibleRange!!.startIndex,
                                visibleRange!!.endIndex + 1
                            )
                            val maxVolume = visibleCandles.maxOf { it.volume }
                            Text(
                                text = maxVolume.toInt().toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                    color = Color.Blue
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Time axis spacer
                Spacer(modifier = Modifier.height(timeAxisHeight))
            }

            // Charts Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Main Candlestick Chart
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .horizontalScroll(scrollState)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(with(density) { contentWidthPx.toDp() })
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    candleWidth = (candleWidth * zoom).coerceIn(10f, 100f)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { position ->
                                    val index =
                                        ((position.x - priceAxisWidthPx) / candleFullWidth).toInt()
                                    if (index in candles.indices) {
                                        selectedCandle = candles[index]
                                    }
                                }
                            }
                    ) {
                        if (candles.isNotEmpty() && visibleRange != null) {
                            val maxPrice = visibleRange!!.maxPrice
                            val minPrice = visibleRange!!.minPrice
                            val priceRange = maxPrice - minPrice
                            val heightPerPrice = size.height / priceRange

                            clipRect {
                                // Background
                                drawRect(Color(0xFF1E1E1E))

                                // Grid lines
                                val priceStep = priceRange / 8
                                repeat(9) { i ->
                                    val y = (i * priceStep) * heightPerPrice
                                    drawLine(
                                        color = Color(0xFF2A2A2A),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y)
                                    )
                                }

                                // Draw MA line first (so it appears under the candles)
                                val visibleCandles = candles.subList(
                                    visibleRange!!.startIndex,
                                    visibleRange!!.endIndex + 1
                                )
                                var lastMA: Pair<Float, Float>? = null
                                visibleCandles.forEachIndexed { index, candle ->
                                    candle.ma20?.let { ma ->
                                        val x = index * candleFullWidth + candleWidth / 2
                                        val y = (maxPrice - ma) * heightPerPrice
                                        if (lastMA != null) {
                                            drawLine(
                                                color = lineColor,
                                                start = Offset(lastMA!!.first, lastMA!!.second),
                                                end = Offset(x, y),
                                                strokeWidth = 1f
                                            )
                                        }
                                        lastMA = Pair(x, y)
                                    }
                                }

                                // Draw candles
                                for (i in visibleRange!!.startIndex..visibleRange!!.endIndex) {
                                    val candle = candles[i]
                                    val x =
                                        (i - visibleRange!!.startIndex) * candleFullWidth + candleSpacing
                                    val color =
                                        if (candle.close >= candle.open) bullColor else bearColor

                                    // Candle body
                                    val top = (maxPrice - maxOf(
                                        candle.open,
                                        candle.close
                                    )) * heightPerPrice
                                    val bottom = (maxPrice - minOf(
                                        candle.open,
                                        candle.close
                                    )) * heightPerPrice

                                    drawRect(
                                        color = color,
                                        topLeft = Offset(x, top),
                                        size = Size(candleWidth, (bottom - top).coerceAtLeast(1f))
                                    )

                                    // Wicks
                                    val centerX = x + candleWidth / 2
                                    drawLine(
                                        color = color,
                                        start = Offset(
                                            centerX,
                                            (maxPrice - candle.high) * heightPerPrice
                                        ),
                                        end = Offset(centerX, top)
                                    )
                                    drawLine(
                                        color = color,
                                        start = Offset(centerX, bottom),
                                        end = Offset(
                                            centerX,
                                            (maxPrice - candle.low) * heightPerPrice
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Volume Chart
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(with(density) { contentWidthPx.toDp() })
                            .fillMaxHeight()
                    ) {
                        if (candles.isNotEmpty() && visibleRange != null) {
                            val visibleCandles = candles.subList(
                                visibleRange!!.startIndex,
                                visibleRange!!.endIndex + 1
                            )
                            val maxVolume = visibleCandles.maxOf { it.volume }

                            clipRect {
                                // Background
                                drawRect(Color(0xFF1E1E1E))

                                // Draw volume bars
                                for (i in visibleRange!!.startIndex..visibleRange!!.endIndex) {
                                    val candle = candles[i]
                                    val x =
                                        (i - visibleRange!!.startIndex) * candleFullWidth + candleSpacing
                                    val color =
                                        if (candle.close >= candle.open) volumeUpColor else volumeDownColor

                                    val barHeight = (candle.volume / maxVolume) * size.height
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(x, size.height - barHeight),
                                        size = Size(candleWidth, barHeight)
                                    )
                                }
                            }
                        }
                    }
                }

                // Time Axis
                Box(
                    modifier = Modifier
                        .height(timeAxisHeight)
                        .horizontalScroll(scrollState)
                ) {
                    Row(
                        modifier = Modifier
                            .width(with(density) { contentWidthPx.toDp() })
                            .fillMaxHeight()
                    ) {
                        if (candles.isNotEmpty() && visibleRange != null) {
                            val visibleCandles = candles.subList(
                                visibleRange!!.startIndex,
                                visibleRange!!.endIndex + 1
                            )

                            visibleCandles.forEach { candle ->
                                Box(
                                    modifier = Modifier
                                        .width(with(density) { candleFullWidth.toDp() })
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    val instant = Instant.fromEpochMilliseconds(candle.timestamp)
                                    val localDateTime =
                                        instant.toLocalDateTime(TimeZone.currentSystemDefault())

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}",
                                            style = TextStyle(fontSize = 10.sp, color = Color.Blue)
                                        )
                                        Text(
                                            text = "${localDateTime.hour}:${
                                                localDateTime.minute.toString().padStart(2, '0')
                                            }",
                                            style = TextStyle(fontSize = 10.sp, color = Color.Blue)
                                        )
                                    }
                                }
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
        val initialInstant = now.minus(500, DateTimeUnit.MINUTE)

        // Calculate MA20
        val prices = List(100) { index ->
            val basePrice = 100f + index * 2
            basePrice + (-5f..5f).random()
        }

        val ma20List = prices.windowed(20, 1, false).map { it.average().toFloat() }

        List(100) { index ->
            val timestamp = initialInstant.plus(5 * index, DateTimeUnit.MINUTE)
            val basePrice = 100f + index * 2
            CandleData(
                timestamp = timestamp.toEpochMilliseconds(),
                open = basePrice + (-5f..5f).random(),
                high = basePrice + (5f..15f).random(),
                low = basePrice + (-15f..-5f).random(),
                close = basePrice + (-5f..5f).random(),
                volume = Random.nextFloat() * 10000f + 5000f,
                ma20 = if (index >= 19) ma20List[index - 19] else null
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EnhancedCandlestickChart(
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