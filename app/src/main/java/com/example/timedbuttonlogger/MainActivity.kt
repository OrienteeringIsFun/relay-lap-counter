package com.example.timedbuttonlogger

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var configLayout: View
    private lateinit var activeLayout: View
    private lateinit var reviewLayout: View
    private lateinit var inputNumbers: EditText
    private lateinit var timerText: TextView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var reviewContainer: LinearLayout
    private lateinit var analyzeButton: Button
    private lateinit var exportButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var sessionStart = 0L
    private val runners = mutableMapOf<Int, Runner>()

    data class Runner(val number: Int) {
        val lapTimes = mutableListOf<Long>()
        var lastTime = 0L
        var restMeters = 0
        fun totalDistance(): Int = lapTimes.size * 400 + restMeters
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - sessionStart
            timerText.text = formatTime(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configLayout = findViewById(R.id.configLayout)
        activeLayout = findViewById(R.id.activeLayout)
        reviewLayout = findViewById(R.id.reviewLayout)
        inputNumbers = findViewById(R.id.inputNumbers)
        timerText = findViewById(R.id.timerText)
        buttonContainer = findViewById(R.id.buttonContainer)
        reviewContainer = findViewById(R.id.reviewContainer)
        analyzeButton = findViewById(R.id.analyzeButton)
        exportButton = findViewById(R.id.exportButton)

        findViewById<Button>(R.id.startButton).setOnClickListener { startSession() }
        findViewById<Button>(R.id.endButton).setOnClickListener { confirmEnd() }
        findViewById<Button>(R.id.restartButton).setOnClickListener { showConfig() }
        analyzeButton.setOnClickListener { analyzeResults() }
        exportButton.setOnClickListener { exportResults() }
    }

    private fun startSession() {
        val numbers = inputNumbers.text.toString()
            .split(',', ' ', '\n')
            .mapNotNull { it.trim().toIntOrNull() }
        if (numbers.isEmpty()) return

        configLayout.visibility = View.GONE
        reviewLayout.visibility = View.GONE
        activeLayout.visibility = View.VISIBLE

        buttonContainer.removeAllViews()
        runners.clear()
        sessionStart = SystemClock.elapsedRealtime()
        handler.post(timerRunnable)

        numbers.forEach { num ->
            val runner = Runner(num)
            runners[num] = runner
            val btn = Button(this).apply {
                text = num.toString()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setBackgroundColor(Color.GREEN)
            }
            val animator = createColorAnimator(btn)
            btn.setOnClickListener {
                animator.cancel()
                btn.setBackgroundColor(Color.GREEN)
                animator.start()
                val elapsed = SystemClock.elapsedRealtime() - sessionStart
                val lap = elapsed - runner.lastTime
                runner.lastTime = elapsed
                runner.lapTimes.add(lap)
            }
            buttonContainer.addView(btn)
            animator.start()
        }
    }

    private fun createColorAnimator(view: View): ValueAnimator {
        return ValueAnimator.ofObject(ArgbEvaluator(), Color.GREEN, Color.RED).apply {
            duration = 90_000
            addUpdateListener { anim ->
                view.setBackgroundColor(anim.animatedValue as Int)
            }
        }
    }

    private fun confirmEnd() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to end the session?")
            .setPositiveButton("Yes") { _, _ -> endSession() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun endSession() {
        handler.removeCallbacks(timerRunnable)
        activeLayout.visibility = View.GONE
        reviewLayout.visibility = View.VISIBLE
        reviewContainer.removeAllViews()
        runners.values.forEach { runner ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
            }
            val numberText = TextView(this).apply {
                text = runner.number.toString()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val restInput = EditText(this).apply {
                hint = "Rest m"
                inputType = InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val totalText = TextView(this).apply {
                text = "Total: ${runner.totalDistance()} m"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            restInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    runner.restMeters = s.toString().toIntOrNull() ?: 0
                    totalText.text = "Total: ${runner.totalDistance()} m"
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            row.addView(numberText)
            row.addView(restInput)
            row.addView(totalText)
            reviewContainer.addView(row)
        }
    }

    private fun showConfig() {
        reviewLayout.visibility = View.GONE
        activeLayout.visibility = View.GONE
        configLayout.visibility = View.VISIBLE
        inputNumbers.setText("")
        reviewContainer.removeAllViews()
        runners.clear()
    }

    private fun analyzeResults() {
        val sorted = runners.values.sortedByDescending { it.totalDistance() }
        reviewContainer.removeAllViews()
        sorted.forEach { runner ->
            val tv = TextView(this).apply {
                text = "Start ${runner.number}: ${runner.totalDistance()} m"
                setPadding(8, 8, 8, 8)
            }
            tv.setOnClickListener { showLapTimes(runner) }
            reviewContainer.addView(tv)
        }
    }

    private fun exportResults() {
        val sb = StringBuilder()
        runners.values.forEach { runner ->
            sb.append("Start ${runner.number}: ${runner.totalDistance()} m\n")
            runner.lapTimes.forEachIndexed { index, t ->
                sb.append("  Runde ${index + 1}: ${formatTime(t)}\n")
            }
            sb.append("  Rest: ${runner.restMeters} m\n\n")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(sendIntent, "Exportieren"))
    }

    private fun showLapTimes(runner: Runner) {
        val msg = runner.lapTimes.mapIndexed { index, t ->
            "Runde ${index + 1}: ${formatTime(t)}"
        }.joinToString("\n")
        AlertDialog.Builder(this)
            .setTitle("Start ${runner.number}")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
