package com.example.yourappname

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Data classes to structure our data
    data class Piece(val name: String, val value: Double)
    data class CheckIn(val pieceName: String, val percentage: Double, val timestamp: String)

    // Storage for our data (in a real app, you'd use a database)
    private var goal: Double = 0.0
    private val pieces = mutableListOf<Piece>()
    private val checkIns = mutableListOf<CheckIn>()

    // UI components
    private lateinit var goalEditText: EditText
    private lateinit var goalTextView: TextView
    private lateinit var setGoalButton: Button
    private lateinit var addPieceButton: Button
    private lateinit var checkInButton: Button
    private lateinit var piecesListView: ListView
    private lateinit var checkInsListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the layout programmatically (normally you'd use XML)
        createLayout()

        // Set up button listeners
        setupListeners()

        // Update the display
        updateDisplay()
    }

    private fun createLayout() {
        // Create a vertical LinearLayout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Goal section
        mainLayout.addView(TextView(this).apply {
            text = "Set Your Goal:"
            textSize = 18f
        })

        goalEditText = EditText(this).apply {
            hint = "Enter goal value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        mainLayout.addView(goalEditText)

        setGoalButton = Button(this).apply { text = "Set Goal" }
        mainLayout.addView(setGoalButton)

        goalTextView = TextView(this).apply {
            text = "Current Goal: $goal"
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        mainLayout.addView(goalTextView)

        // Buttons section
        addPieceButton = Button(this).apply { text = "Add New Piece" }
        mainLayout.addView(addPieceButton)

        checkInButton = Button(this).apply { text = "Check In Piece" }
        mainLayout.addView(checkInButton)

        // Pieces list
        mainLayout.addView(TextView(this).apply {
            text = "Your Pieces:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        })

        piecesListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
            )
        }
        mainLayout.addView(piecesListView)

        // Check-ins list
        mainLayout.addView(TextView(this).apply {
            text = "Recent Check-ins:"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        })

        checkInsListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
            )
        }
        mainLayout.addView(checkInsListView)

        setContentView(mainLayout)
    }

    private fun setupListeners() {
        setGoalButton.setOnClickListener {
            val goalText = goalEditText.text.toString()
            if (goalText.isNotEmpty()) {
                goal = goalText.toDoubleOrNull() ?: 0.0
                updateDisplay()
                goalEditText.text.clear()
                Toast.makeText(this, "Goal set to $goal", Toast.LENGTH_SHORT).show()
            }
        }

        addPieceButton.setOnClickListener {
            showAddPieceDialog()
        }

        checkInButton.setOnClickListener {
            if (pieces.isEmpty()) {
                Toast.makeText(this, "Add some pieces first!", Toast.LENGTH_SHORT).show()
            } else {
                showCheckInDialog()
            }
        }
    }

    private fun showAddPieceDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val nameEditText = EditText(this).apply {
            hint = "Piece name"
        }
        dialogLayout.addView(nameEditText)

        val valueEditText = EditText(this).apply {
            hint = "Piece value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        dialogLayout.addView(valueEditText)

        AlertDialog.Builder(this)
            .setTitle("Add New Piece")
            .setView(dialogLayout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val valueText = valueEditText.text.toString()

                if (name.isNotEmpty() && valueText.isNotEmpty()) {
                    val value = valueText.toDoubleOrNull() ?: 0.0
                    pieces.add(Piece(name, value))
                    updateDisplay()
                    Toast.makeText(this, "Added $name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCheckInDialog() {
        // First, show piece selection
        val pieceNames = pieces.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Piece to Check In")
            .setItems(pieceNames) { _, which ->
                val selectedPiece = pieces[which]
                showPercentageDialog(selectedPiece)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPercentageDialog(piece: Piece) {
        val percentageEditText = EditText(this).apply {
            hint = "Enter percentage (0-100)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Check in ${piece.name}")
            .setMessage("Current value: ${piece.value}")
            .setView(percentageEditText)
            .setPositiveButton("Check In") { _, _ ->
                val percentageText = percentageEditText.text.toString()
                if (percentageText.isNotEmpty()) {
                    val percentage = percentageText.toDoubleOrNull() ?: 0.0
                    if (percentage in 0.0..100.0) {
                        performCheckIn(piece, percentage)
                    } else {
                        Toast.makeText(this, "Please enter a valid percentage (0-100)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCheckIn(piece: Piece, percentage: Double) {
        val checkInValue = piece.value * (percentage / 100.0)
        goal -= checkInValue

        val timestamp = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
        checkIns.add(CheckIn(piece.name, percentage, timestamp))

        updateDisplay()
        Toast.makeText(this, "Checked in ${percentage}% of ${piece.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateDisplay() {
        goalTextView.text = "Current Goal: ${String.format("%.2f", goal)}"

        // Update pieces list
        val piecesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            pieces.map { "${it.name}: ${it.value}" }
        )
        piecesListView.adapter = piecesAdapter

        // Update check-ins list
        val checkInsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            checkIns.takeLast(5).map { "${it.timestamp}: ${it.percentage}% of ${it.pieceName}" }
        )
        checkInsListView.adapter = checkInsAdapter
    }
}