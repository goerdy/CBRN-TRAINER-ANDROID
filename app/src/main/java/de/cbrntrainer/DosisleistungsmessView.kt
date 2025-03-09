package de.cbrntrainer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.log10

class DosisleistungsmessView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentValue = 0.0
    private var sessionId = ""
    
    fun setSessionId(id: String) {
        sessionId = id
        invalidate()
    }
    
    fun updateValue(value: Double) {
        currentValue = value
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDevice(canvas)
    }
    
    private fun drawDevice(canvas: Canvas) {
        drawBaseDevice(canvas)
        drawHeader(canvas)
        drawDisplay(canvas)
        drawButtons(canvas)
        drawTypeplate(canvas)
    }
    
    private fun drawBaseDevice(canvas: Canvas) {
        // Grundform des Geräts
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        roundRect(canvas, 20f, 20f, 260f, 460f, 10f)
    }
    
    private fun drawHeader(canvas: Canvas) {
        // CBRN-TRAINER Box
        paint.color = Color.WHITE
        roundRect(canvas, 35f, 35f, 80f, 25f, 3f)
        
        // CBRN-TRAINER Text
        paint.color = Color.BLACK
        paint.textSize = 11f * resources.displayMetrics.density
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("CBRN-TRAINER", 75f, 52f, paint)
        
        // Seriennummer Box
        paint.color = Color.WHITE
        roundRect(canvas, 185f, 35f, 80f, 25f, 3f)
        
        // Seriennummer
        paint.color = Color.BLACK
        paint.textSize = 12f * resources.displayMetrics.density
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("S/N: $sessionId", 190f, 52f, paint)
        
        // Radioaktivitäts-Symbol
        drawRadiationSymbol(canvas)
    }
    
    private fun drawRadiationSymbol(canvas: Canvas) {
        paint.color = Color.parseColor("#FFD700") // Gold
        
        // Dreieck
        val path = Path()
        val centerX = 150f
        val centerY = 47f
        val size = 15f
        val radius = 3f
        
        // Dreieckspunkte
        val point1 = Pair(centerX - size, centerY - size) // oben links
        val point2 = Pair(centerX + size, centerY - size) // oben rechts
        val point3 = Pair(centerX, centerY + size)        // unten mitte
        
        path.moveTo((point1.first + point2.first) / 2, point1.second)
        
        // Obere rechte Ecke
        path.arcTo(
            RectF(point2.first - radius, point2.second - radius,
                  point2.first + radius, point2.second + radius),
            -90f, 90f, false)
        
        // Untere rechte Ecke
        path.arcTo(
            RectF(point3.first - radius, point3.second - radius,
                  point3.first + radius, point3.second + radius),
            30f, 90f, false)
        
        // Untere linke Ecke
        path.arcTo(
            RectF(point1.first - radius, point1.second - radius,
                  point1.first + radius, point1.second + radius),
            150f, 90f, false)
        
        path.close()
        canvas.drawPath(path, paint)
        
        // Kreis im Symbol
        canvas.drawCircle(centerX, centerY, 6f, paint)
    }
    
    private fun drawDisplay(canvas: Canvas) {
        // LCD Rahmen
        paint.color = Color.parseColor("#333333")
        roundRect(canvas, 40f, 90f, 220f, 100f, 5f)
        
        // LCD Hintergrund
        paint.color = Color.parseColor("#90EE90")
        canvas.drawRect(45f, 95f, 255f, 185f, paint)
        
        // Messwerte
        val formattedValue = formatValue(currentValue)
        
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        
        // Einheit
        paint.textSize = 20f * resources.displayMetrics.density
        canvas.drawText(formattedValue.second, 150f, 120f, paint)
        
        // Wert
        paint.textSize = 36f * resources.displayMetrics.density
        canvas.drawText(formattedValue.first, 150f, 165f, paint)
    }
    
    private fun drawButtons(canvas: Canvas) {
        for (i in 0..3) {
            val row = i / 2
            val col = i % 2
            val centerX = 60f + col * 180f
            val centerY = 230f + row * 90f
            val radius = 25f
            
            // Button Hintergrund
            paint.color = Color.parseColor("#444444")
            canvas.drawCircle(centerX, centerY, radius, paint)
            
            // Button Rand
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.parseColor("#666666")
            canvas.drawCircle(centerX, centerY, radius, paint)
            
            // Button Symbole
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FFD700")
            drawButtonSymbol(canvas, i, centerX, centerY + 38f)
        }
    }
    
    private fun drawButtonSymbol(canvas: Canvas, buttonIndex: Int, x: Float, y: Float) {
        when (buttonIndex) {
            0 -> { // Kreis mit Punkt
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(x, y, 10f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x, y, 3f, paint)
            }
            1 -> { // Glühbirne
                canvas.drawCircle(x, y - 4f, 6f, paint)
                canvas.drawRect(x - 3f, y + 2f, x + 3f, y + 5f, paint)
            }
            2 -> { // Haus
                val path = Path()
                path.moveTo(x, y - 8f)
                path.lineTo(x - 8f, y)
                path.lineTo(x + 8f, y)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawRect(x - 5f, y, x + 5f, y + 8f, paint)
            }
            3 -> { // Lautsprecher
                val path = Path()
                path.moveTo(x - 5f, y)
                path.lineTo(x + 3f, y - 7f)
                path.lineTo(x + 3f, y + 7f)
                path.close()
                canvas.drawPath(path, paint)
                
                paint.style = Paint.Style.STROKE
                canvas.drawArc(RectF(x + 1f, y - 5f, x + 11f, y + 5f), -60f, 120f, false, paint)
                canvas.drawArc(RectF(x + 1f, y - 8f, x + 17f, y + 8f), -60f, 120f, false, paint)
            }
        }
    }
    
    private fun drawTypeplate(canvas: Canvas) {
        // Typenschild Hintergrund
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        roundRect(canvas, 40f, 380f, 220f, 85f, 5f)
        
        // Text
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        
        // Modell und H*(10)
        paint.textSize = 14f * resources.displayMetrics.density
        paint.isFakeBoldText = true
        canvas.drawText("6150 AD 5/E", 45f, 400f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 12f * resources.displayMetrics.density
        canvas.drawText("H*(10)", 160f, 400f, paint)
        
        // Technische Daten
        paint.textSize = 11f * resources.displayMetrics.density
        
        // Linke Spalte
        canvas.drawText("Anzeigebereich", 45f, 415f, paint)
        canvas.drawText("Messbereich", 45f, 430f, paint)
        canvas.drawText("Energiebereich", 45f, 445f, paint)
        canvas.drawText("Winkelbereich", 45f, 460f, paint)
        
        // Rechte Spalte
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("0,1µSv/h - 1Sv/h", 240f, 415f, paint)
        canvas.drawText("0.5µSv/h - 999mSv/h", 240f, 430f, paint)
        canvas.drawText("60keV - 1,3MeV", 240f, 445f, paint)
        canvas.drawText("±45°", 240f, 460f, paint)
        
        // CE Symbol
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f * resources.displayMetrics.density
        canvas.drawText("CE", 230f, 400f, paint)
    }
    
    private fun roundRect(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
    
    private fun formatValue(value: Double): Pair<String, String> {
        // Begrenze den Maximalwert auf 1 Sv/h
        val limitedValue = value.coerceAtMost(1.0)
        
        // Konvertiere in µSv/h für die Berechnung
        val microSv = limitedValue * 1000000
        
        return when {
            microSv < 999 -> {
                Pair(String.format("%.2f", microSv), "µSv/h")
            }
            microSv < 999000 -> {
                Pair(String.format("%.2f", microSv / 1000), "mSv/h")
            }
            else -> {
                Pair(String.format("%.2f", microSv / 1000000), "Sv/h")
            }
        }
    }
} 