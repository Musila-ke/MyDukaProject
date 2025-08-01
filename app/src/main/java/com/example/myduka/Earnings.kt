package com.example.myduka

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myduka.databinding.ActivityEarningsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class Earnings : AppCompatActivity() {
    private lateinit var binding: ActivityEarningsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedBranchId: String? = null
    private var selectedTimeFilter: TimeFilter = TimeFilter.ALL
    private var branchListener: ListenerRegistration? = null
    private var lightBlue: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEarningsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lightBlue = ContextCompat.getColor(this, R.color.lightblue)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        setupTimeChips()
        setupChart()
        loadChartData()
        loadBranchChips()
    }

    override fun onDestroy() {
        super.onDestroy()
        branchListener?.remove()
    }

    private fun loadBranchChips() {
        val uid = auth.currentUser?.uid ?: return
        branchListener?.remove()
        branchListener = db.collection("users").document(uid)
            .collection("branches")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener

                binding.chipGroupBranches.removeAllViews()
                binding.chipGroupBranches.addView(
                    makeBranchChip("All", null).apply { isChecked = true }
                )
                for (doc in snap.documents) {
                    val name = doc.getString("name").orEmpty()
                    binding.chipGroupBranches.addView(
                        makeBranchChip(name, doc.id)
                    )
                }

                binding.chipGroupBranches.setOnCheckedStateChangeListener { _, ids ->
                    val chip = binding.chipGroupBranches.findViewById<Chip>(ids.first())
                    selectedBranchId = chip.tag as? String
                    loadChartData()
                }
            }
    }

    private fun makeBranchChip(label: String, branchId: String?): Chip {
        return Chip(this).apply {
            text = label
            tag = branchId
            isCheckable = true
            chipCornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
            )
            setChipBackgroundColorResource(
                com.google.android.material.R.color.mtrl_chip_background_color
            )
        }
    }

    private fun setupTimeChips() {
        binding.chipGroupTime.apply {
            check(R.id.chipAll)
            setOnCheckedStateChangeListener { _, ids ->
                selectedTimeFilter = when (ids.firstOrNull()) {
                    R.id.chipToday -> TimeFilter.TODAY
                    R.id.chipWeek  -> TimeFilter.WEEK
                    R.id.chipMonth -> TimeFilter.MONTH
                    R.id.chipYear  -> TimeFilter.YEAR
                    else           -> TimeFilter.ALL
                }
                loadChartData()
            }
        }
    }

    private fun setupChart() {
        binding.barChart.apply {
            setAutoScaleMinMaxEnabled(true)
            setExtraTopOffset(24f)

            description = Description().apply {
                isEnabled = false
            }

            legend.apply {
                isEnabled = false
            }

            axisRight.isEnabled = false

            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                labelRotationAngle = -45f
                textSize = 10f
                textColor = lightBlue
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                textColor = lightBlue
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?) =
                        when {
                            value >= 1_000_000f -> "${(value / 1_000_000).toInt()}M"
                            value >= 1_000f     -> "${(value / 1_000).toInt()}K"
                            else                 -> value.toInt().toString()
                        }
                }
            }
        }
    }

    private fun loadChartData() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            val salesMap = withContext(Dispatchers.IO) {
                val startDate = calculateStartDate(selectedTimeFilter)
                val map = mutableMapOf<LocalDate, Double>()

                val branchIds = selectedBranchId?.let { listOf(it) }
                    ?: db.collection("users").document(uid)
                        .collection("branches").get().await().documents.map { it.id }

                for (bid in branchIds) {
                    var q: Query = db.collection("users").document(uid)
                        .collection("branches").document(bid)
                        .collection("sales")
                        .orderBy("timestamp", Query.Direction.ASCENDING)

                    startDate?.let { q = q.whereGreaterThanOrEqualTo("timestamp", Timestamp(it)) }
                    val snap = q.get().await()
                    for (doc in snap.documents) {
                        doc.getTimestamp("timestamp")?.toDate()?.toLocalDate()?.let { date ->
                            val total = doc.getDouble("grandTotal") ?: 0.0
                            map[date] = (map[date] ?: 0.0) + total
                        }
                    }
                }
                map
            }

            val sortedDates = salesMap.keys.sorted()
            val entries = sortedDates.mapIndexed { i, date ->
                BarEntry(i.toFloat(), salesMap[date]!!.toFloat())
            }
            val labels = sortedDates.map { it.format(DateTimeFormatter.ofPattern("MM/dd")) }

            val dataSet = BarDataSet(entries, "").apply {
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = lightBlue
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(entry: BarEntry) =
                        entry.y.let {
                            when {
                                it >= 1_000_000f -> "${(it / 1_000_000).toInt()}M"
                                it >= 1_000f     -> "${(it / 1_000).toInt()}K"
                                else             -> it.toInt().toString()
                            }
                        }
                }
            }
            val data = BarData(dataSet).apply { barWidth = 0.85f }

            binding.barChart.apply {
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                setFitBars(true)
                this.data = data
                invalidate()
            }
        }
    }

    private fun calculateStartDate(filter: TimeFilter): Date? {
        val cal = Calendar.getInstance()
        return when (filter) {
            TimeFilter.TODAY -> cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
            TimeFilter.WEEK  -> cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.time
            TimeFilter.MONTH -> cal.apply { add(Calendar.MONTH, -1) }.time
            TimeFilter.YEAR  -> cal.apply { add(Calendar.YEAR, -1) }.time
            TimeFilter.ALL   -> null
        }
    }

    private fun Date.toLocalDate(): LocalDate = toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    private enum class TimeFilter { TODAY, WEEK, MONTH, YEAR, ALL }
}
