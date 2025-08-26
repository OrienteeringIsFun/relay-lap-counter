package com.example.timedbuttonlogger

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
    private lateinit var logText: TextView
    private lateinit var buttonContainer: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var sessionStart = 0L
    private val logEntries = mutableListOf<String>()

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
        logText = findViewById(R.id.logText)
        buttonContainer = findViewById(R.id.buttonContainer)

        findViewById<Button>(R.id.startButton).setOnClickListener { startSession() }
        findViewById<Button>(R.id.endButton).setOnClickListener { confirmEnd() }
        findViewById<Button>(R.id.restartButton).setOnClickListener { showConfig() }
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
        logEntries.clear()
        sessionStart = SystemClock.elapsedRealtime()
        handler.post(timerRunnable)

        numbers.forEach { num ->
            val btn = Button(this).apply {
                text = num.toString()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(Color.GREEN)
            }
            val animator = createColorAnimator(btn)
            btn.setOnClickListener {
                animator.cancel()
                btn.setBackgroundColor(Color.GREEN)
                animator.start()
                val elapsed = SystemClock.elapsedRealtime() - sessionStart
                logEntries.add("$num, ${formatTime(elapsed)}")
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
        logText.text = logEntries.joinToString("\n")
    }

    private fun showConfig() {
        reviewLayout.visibility = View.GONE
        activeLayout.visibility = View.GONE
        configLayout.visibility = View.VISIBLE
        inputNumbers.setText("")
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
