package com.friday.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * F.R.I.D.A.Y logosunu canvas uzerinde cizen ozel View.
 * Windows surumundeki Tkinter canvas cizimine benzer mantik: ic ice
 * halkalar + saat isareti gibi kesik cizgiler + ortada isim.
 *
 * Durum rengine gore (yesil/turuncu/kirmizi) renklenir, setStatus()
 * cagrildiginda otomatik yeniden cizilir (invalidate).
 */
class FridayLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Status { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

    private var currentStatus = Status.IDLE

    private val ringPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val tickPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#EAFFF5")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
    }

    /** Disaridan cagrilarak logonun durumunu (ve rengini) degistirir. */
    fun setStatus(status: Status) {
        currentStatus = status
        invalidate()
    }

    private fun colorForStatus(): Int = when (currentStatus) {
        Status.IDLE, Status.LISTENING -> Color.parseColor("#1D9E75")
        Status.THINKING, Status.SPEAKING -> Color.parseColor("#EF9F27")
        Status.ERROR -> Color.parseColor("#E24B4A")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val renk = colorForStatus()
        ringPaint.color = renk
        tickPaint.color = renk

        val disYaricap = minOf(width, height) * 0.38f
        val ortaYaricap = disYaricap * 0.64f
        val icYaricap = disYaricap * 0.38f

        // Saat isareti gibi kesik cizgiler (36 adet, dis halka)
        for (i in 0 until 36) {
            val aci = Math.toRadians((i * 10).toDouble())
            val r1 = disYaricap * 1.08f
            val r2 = if (i % 3 == 0) disYaricap * 1.18f else disYaricap * 1.14f
            val x1 = cx + r1 * cos(aci).toFloat()
            val y1 = cy + r1 * sin(aci).toFloat()
            val x2 = cx + r2 * cos(aci).toFloat()
            val y2 = cy + r2 * sin(aci).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        canvas.drawCircle(cx, cy, disYaricap, ringPaint)
        canvas.drawCircle(cx, cy, ortaYaricap, ringPaint)
        canvas.drawCircle(cx, cy, icYaricap, ringPaint)

        canvas.drawText("F.R.I.D.A.Y", cx, cy + (textPaint.textSize / 3), textPaint)
    }
}
