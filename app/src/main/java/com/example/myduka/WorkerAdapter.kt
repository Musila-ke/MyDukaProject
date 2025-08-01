package com.example.myduka

<<<<<<< HEAD
import android.location.Address
import android.location.Geocoder
=======
>>>>>>> 0199aa4cd00cbe71791ed8d2b830cb9e88d1463a
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.WorkerrecyclerviewBinding
import com.google.firebase.firestore.DocumentSnapshot
<<<<<<< HEAD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
=======
>>>>>>> 0199aa4cd00cbe71791ed8d2b830cb9e88d1463a
import java.text.SimpleDateFormat
import java.util.Locale

class WorkerAdapter(
    private var workerSnapshots: List<DocumentSnapshot>
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    inner class WorkerViewHolder(val binding: WorkerrecyclerviewBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = WorkerrecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkerViewHolder(binding)
    }

<<<<<<< HEAD
    override fun getItemCount(): Int = workerSnapshots.size

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val doc = workerSnapshots[position]
        val ctx = holder.binding.root.context

        // Basic fields
=======
    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val doc = workerSnapshots[position]

        // Basic info
>>>>>>> 0199aa4cd00cbe71791ed8d2b830cb9e88d1463a
        holder.binding.workerEmail.text    = doc.getString("workerEmail") ?: "No Email"
        holder.binding.workerName.text     = doc.getString("workerName")  ?: "No Name"
        holder.binding.branchPosition.text = doc.getString("branchName")  ?: "No Branch"

<<<<<<< HEAD
        // Timestamps & GeoPoints
        val inTime  = doc.getTimestamp("lastCheckInTime")?.toDate()
        val outTime = doc.getTimestamp("lastCheckOutTime")?.toDate()
        val inLoc   = doc.getGeoPoint("lastCheckInLocation")
        val outLoc  = doc.getGeoPoint("lastCheckOutLocation")
        val fmt     = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault())

        // Decide which event to show
        val (label, tsText, geoPoint) = when {
            inTime == null && outTime == null ->
                Triple("No check‑in/out yet", null, null)
            outTime == null || (inTime != null && inTime.after(outTime)) ->
                Triple("Last check‑in:", fmt.format(inTime!!), inLoc)
            else ->
                Triple("Last check‑out:", fmt.format(outTime!!), outLoc)
        }

        // Style status text
        val colorRes = when {
            geoPoint == null                  -> R.color.lightgrey
            label.startsWith("Last check‑in") -> R.color.green
            else                              -> R.color.red
        }
        holder.binding.workerStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))
        holder.binding.workerStatus.text = tsText?.let { "$label $it" } ?: label

        // Reverse‑geocode if we have both time and location
        if (geoPoint != null && tsText != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val addresses: List<Address>? = try {
                    Geocoder(ctx, Locale.getDefault())
                        .getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                } catch (e: Exception) {
                    null
                }

                val address = addresses
                    ?.firstOrNull()
                    ?.getAddressLine(0)
                    ?: "Unknown location"

                CoroutineScope(Dispatchers.Main).launch {
                    holder.binding.workerStatus.text = "$label $tsText at $address"
                }
            }
        }
    }

=======
        // Pull both timestamps
        val inDate  = doc.getTimestamp("lastCheckInTime")?.toDate()
        val outDate = doc.getTimestamp("lastCheckOutTime")?.toDate()
        val fmt     = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault())

        // Decide which to show: if outDate is null or inDate is newer, show check‑in; else check‑out
        val displayText: String
        val isCheckedIn: Boolean
        when {
            inDate == null && outDate == null -> {
                displayText  = "No check‑in/out yet"
                isCheckedIn  = false
            }
            outDate == null -> {
                displayText  = "Last check‑in: ${fmt.format(inDate)}"
                isCheckedIn  = true
            }
            inDate == null -> {
                displayText  = "Last check‑out: ${fmt.format(outDate)}"
                isCheckedIn  = false
            }
            inDate.after(outDate) -> {
                displayText  = "Last check‑in: ${fmt.format(inDate)}"
                isCheckedIn  = true
            }
            else -> {
                displayText  = "Last check‑out: ${fmt.format(outDate)}"
                isCheckedIn  = false
            }
        }

        // Set text + color
        holder.binding.workerStatus.text = displayText
        val colorRes = if (isCheckedIn) R.color.green else R.color.red
        holder.binding.workerStatus.setTextColor(
            ContextCompat.getColor(holder.binding.root.context, colorRes)
        )
    }

    override fun getItemCount(): Int = workerSnapshots.size

    fun getSnapshot(position: Int): DocumentSnapshot = workerSnapshots[position]

>>>>>>> 0199aa4cd00cbe71791ed8d2b830cb9e88d1463a
    fun updateSnapshots(newSnapshots: List<DocumentSnapshot>) {
        workerSnapshots = newSnapshots
        notifyDataSetChanged()
    }
<<<<<<< HEAD

    fun getSnapshot(position: Int): DocumentSnapshot = workerSnapshots[position]
=======
>>>>>>> 0199aa4cd00cbe71791ed8d2b830cb9e88d1463a
}
