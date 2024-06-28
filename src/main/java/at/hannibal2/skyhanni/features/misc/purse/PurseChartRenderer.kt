package at.hannibal2.skyhanni.features.misc.purse

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartFrame
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

object PurseChartRenderer {
    fun render(dataPoints: List<DataPoint>) {
        val chart = createAndFormatChart(dataPoints)
        renderChart(chart)
    }

    private fun renderChart(chart: JFreeChart) {
        ChartFrame("SkyHanni Purse History", chart).apply {
            pack()
            isVisible = true
        }
    }

    private fun createAndFormatChart(dataPoints: List<DataPoint>): JFreeChart {
        val dataset = loadData(dataPoints)

        val chart = ChartFactory.createTimeSeriesChart(
            "Purse History", "Time", "Coins (Millions)", dataset, false, false, false,
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

    private fun loadData(dataPoints: List<DataPoint>): TimeSeriesCollection {
        val series = TimeSeries("Purse History")
        for (point in dataPoints) {
            series.add(Millisecond(Date.from(Instant.ofEpochMilli(point.time))), point.value)
        }
        return TimeSeriesCollection(series)
    }
}
