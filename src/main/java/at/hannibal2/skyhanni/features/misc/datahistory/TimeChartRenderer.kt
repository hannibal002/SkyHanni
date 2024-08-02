package at.hannibal2.skyhanni.features.misc.datahistory

import ChartScreen
import at.hannibal2.skyhanni.SkyHanniMod
import kotlinx.coroutines.launch
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import java.awt.BasicStroke
import java.awt.Color
import java.text.NumberFormat
import java.time.Instant
import java.util.Date
import kotlin.time.Duration

object TimeChartRenderer {
    fun openTimeChart(dataPoints: List<DataPoint>, duration: Duration, title: String, label: String) {
        val begin = System.currentTimeMillis() - duration.inWholeMilliseconds
        val filteredData = dataPoints.filter { it.time > begin }
        SkyHanniMod.coroutineScope.launch {
            val chart = createAndFormatChart(title, filteredData, label)
            val screen = ChartScreen(chart)
            SkyHanniMod.screenToOpen = screen
        }
    }

    private fun createAndFormatChart(title: String, dataPoints: List<DataPoint>, label: String): JFreeChart {
        val dataset = loadData(title, dataPoints)

        val chart = ChartFactory.createTimeSeriesChart(
            title, "Time", label, dataset, false, false, false,
        )

        val plot = chart.plot as XYPlot
        val renderer = plot.renderer as XYLineAndShapeRenderer
        renderer.setSeriesPaint(0, Color.BLUE)
        renderer.setSeriesStroke(0, BasicStroke(2.5f))

        (plot.rangeAxis as NumberAxis).apply {
            numberFormatOverride = NumberFormat.getNumberInstance().apply {
                maximumFractionDigits = 2
                isGroupingUsed = false
            }
        }

        return chart
    }

    private fun loadData(title: String, dataPoints: List<DataPoint>): TimeSeriesCollection {
        val series = TimeSeries(title)
        for (point in dataPoints) {
            series.add(Millisecond(Date.from(Instant.ofEpochMilli(point.time))), point.value)
        }
        return TimeSeriesCollection(series)
    }
}
