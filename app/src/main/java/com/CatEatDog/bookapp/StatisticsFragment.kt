package com.CatEatDog.bookapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var statsRef: DatabaseReference
    private lateinit var genresRef: DatabaseReference

    private lateinit var totalBooksReadTextView: TextView
    private lateinit var totalReadingTimeTextView: TextView
    private lateinit var mostPopularGenresTextView: TextView
    private lateinit var timeFrameSpinner: Spinner
    private val TAG = "STATISTIC_VIEW"

    private val processedGenreIds = mutableSetOf<String>()
    private val processedBookIds = mutableSetOf<String>()

    private var selectedTimeFrame = "All"  // Default selection

    private lateinit var readingTimeChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        statsRef = database.getReference("users")
        genresRef = database.getReference("Genres")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        totalBooksReadTextView = view.findViewById(R.id.totalBooksReadTextView)
        totalReadingTimeTextView = view.findViewById(R.id.totalReadingTimeTextView)
        mostPopularGenresTextView = view.findViewById(R.id.mostPopularGenresTextView)
        timeFrameSpinner = view.findViewById(R.id.timeFrameSpinner)

        readingTimeChart = view.findViewById(R.id.readingTimeChart)

        // Set up Spinner for time frame selection
        val timeFrameOptions = listOf("Today", "This Week", "This Year", "All")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeFrameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeFrameSpinner.adapter = adapter

        timeFrameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTimeFrame = timeFrameOptions[position]
                loadStatistics()  // Reload stats based on selected time frame
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadStatistics()  // Initial load of stats

        return view
    }

    private fun loadStatistics() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // Clear state for fresh calculation
            processedBookIds.clear()
            processedGenreIds.clear()

            statsRef.child(userId).child("readingLogs").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalBooksRead = 0
                    var totalReadingTime = 0L
                    val genreCount = mutableMapOf<String, Int>()
                    val genreNames = mutableMapOf<String, String>()
                    var genreProcessingCount = 0
                    var genreProcessedCount = 0

                    val readingTimePerHour = mutableMapOf<Int, Long>()
                    val currentTime = System.currentTimeMillis()

                    snapshot.children.forEach { logSnapshot ->
                        // Filter logs by time frame first
                        val startTime = logSnapshot.child("startTime").getValue(Long::class.java) ?: 0L
                        if (!isWithinTimeFrame(startTime, currentTime)) {
                            return@forEach // Skip this log if it doesn't fit the selected time frame
                        }

                        val duration = logSnapshot.child("duration").getValue(Long::class.java) ?: 0L
                        val calendar = Calendar.getInstance(TimeZone.getDefault()) // Use device timezone
                        calendar.timeInMillis = startTime
                        val hour = calendar.get(Calendar.HOUR_OF_DAY) // Get the hour of the day

                        // Accumulate the reading time per hour
                        readingTimePerHour[hour] = readingTimePerHour.getOrDefault(hour, 0L) + duration
                    }

                    // Update the chart with the reading time data
                    updateReadingTimeChart(readingTimePerHour)

                    snapshot.children.forEach { logSnapshot ->
                        val bookId = logSnapshot.child("bookId").getValue(String::class.java) ?: ""
                        if (bookId.isNotEmpty() && !processedBookIds.contains(bookId)) {
                            processedBookIds.add(bookId)
                            totalBooksRead++  // Count unique books
                        }

                        totalReadingTime += logSnapshot.child("duration").getValue(Long::class.java) ?: 0L

                        val genres = logSnapshot.child("genreIds").children
                        genres.forEach { genreSnapshot ->
                            val genreId = genreSnapshot.getValue(String::class.java) ?: ""
                            if (genreId.isNotEmpty() && !processedGenreIds.contains(genreId)) {
                                processedGenreIds.add(genreId)
                                genreProcessingCount++

                                fetchGenreName(genreId) { genreName ->
                                    genreNames[genreId] = genreName
                                    genreCount[genreName] = genreCount.getOrDefault(genreName, 0) + 1

                                    genreProcessedCount++
                                    if (genreProcessedCount == genreProcessingCount) {
                                        updateUI(totalBooksRead, totalReadingTime, genreCount)
                                    }
                                }
                            }
                        }
                    }

                    // If no genres are processed, update UI immediately
                    if (genreProcessingCount == 0) {
                        updateUI(totalBooksRead, totalReadingTime, genreCount)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading reading logs: ${error.message}")
                }
            })
        }
    }

    private fun updateReadingTimeChart(readingTimePerHour: Map<Int, Long>) {
        val entries = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()

        // Ensure all 24 hours are present in the dataset
        for (hour in 0..23) {
            val totalTime = readingTimePerHour[hour] ?: 0L // Default to 0 if no data for the hour
            entries.add(BarEntry(hour.toFloat(), totalTime.toFloat()))
            xLabels.add(hour.toString()) // Add label for each hour
        }

        // Create a BarDataSet
        val dataSet = BarDataSet(entries, "Reading Time per Hour")
        dataSet.color = resources.getColor(android.R.color.holo_blue_dark) // Bar color
        dataSet.valueTextColor = resources.getColor(android.R.color.transparent) // Remove value labels

        // Set up chart data
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f // Set bar width for proper spacing

        readingTimeChart.data = barData
        readingTimeChart.invalidate() // Refresh the chart

        // Configure X-axis
        val xAxis = readingTimeChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAxis.setLabelCount(24, true) // Ensure all 24 labels are shown
        xAxis.granularity = 1f // One label per hour
        xAxis.isGranularityEnabled = true
        //xAxis.labelRotationAngle = 45f // Rotate labels for better visibility
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = resources.getColor(android.R.color.white) // Set label color
        xAxis.textSize = 8f // Adjust label text size
        xAxis.axisMinimum = 0f // Align the first bar with the X-axis
        xAxis.axisMaximum = 23f // Ensure last bar is fully visible

        // Ensure the space between labels is properly calculated and displayed
        xAxis.setDrawLabels(true) // Make sure labels are drawn

        // Configure Y-axis (Left)
        val yAxisLeft = readingTimeChart.axisLeft
        yAxisLeft.setDrawGridLines(false) // Hide gridlines
        yAxisLeft.textColor = resources.getColor(android.R.color.white) // Set label color
        yAxisLeft.axisMinimum = 0f // Start Y-axis at 0 for proper bar alignment

        // Disable the right Y-axis
        readingTimeChart.axisRight.isEnabled = false

        // Disable chart description
        readingTimeChart.description.isEnabled = false

        // Disable legend if not needed
        readingTimeChart.legend.isEnabled = false

        // Refresh the chart view
        readingTimeChart.invalidate()
    }



    private fun isWithinTimeFrame(startTime: Long, currentTime: Long): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return when (selectedTimeFrame) {
            "Today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime >= calendar.timeInMillis
            }
            "This Week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime >= calendar.timeInMillis
            }
            "This Year" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime >= calendar.timeInMillis
            }
            "All" -> true
            else -> false
        }
    }

    private fun fetchGenreName(genreId: String, callback: (String) -> Unit) {
        genresRef.child(genreId).child("genre").get().addOnSuccessListener { snapshot ->
            val genreName = snapshot.getValue(String::class.java) ?: ""
            callback(genreName)
        }
    }

    private fun updateUI(totalBooksRead: Int, totalReadingTime: Long, genreCount: Map<String, Int>) {
        totalBooksReadTextView.text = "Total Books Read: $totalBooksRead"
        totalReadingTimeTextView.text = "Total Reading Time: ${totalReadingTime / 1000} seconds"

        // Display most popular genres
        val mostPopularGenres = genreCount.entries.sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { it.key }

        mostPopularGenresTextView.text = "Most Popular Genres: $mostPopularGenres"

        // Set text colors to white
        totalBooksReadTextView.setTextColor(resources.getColor(android.R.color.white))
        totalReadingTimeTextView.setTextColor(resources.getColor(android.R.color.white))
        mostPopularGenresTextView.setTextColor(resources.getColor(android.R.color.white))
    }
}
