package com.music.vivi.ui.player

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnLayout
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.R
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class BlendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val isMiniPlayer: Boolean = false
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, scaleToScreen: Boolean) : this(
        context = context,
        isMiniPlayer = !scaleToScreen
    )

    // Inner 100dp x 100dp core mesh container (matches Accord's 100dp architecture)
    private val coreMesh: CoreMeshLayout = CoreMeshLayout(context)

    private val imageViewTS: ImageSwitcher
    private val imageViewBE: ImageSwitcher
    private val imageViewBG: ImageSwitcher
    private val rotateFrame: FrameLayout

    private var isAnimationOngoing: Boolean = true
    private val handler = Handler(Looper.getMainLooper())
    private val overlayColor = ContextCompat.getColor(context, R.color.contrast_blendOverlayColor)
    private var previousBitmap: Bitmap? = null
    private var currentUrl: String? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CORE_SIZE_DP: Float = 100F
        const val VIEW_TRANSIT_DURATION: Long = 400
        const val FULL_BLUR_RADIUS: Float = 80F
        const val SHALLOW_BLUR_RADIUS: Float = 60F
        const val UPDATE_RUNNABLE_INTERVAL: Long = 34
        const val CYCLE: Int = 360
        const val SATURATION_FACTOR: Float = 2.5F
        const val PICTURE_SIZE: Int = 60
    }

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.contrast_colorBackgroundContainer))
        isClickable = false
        isFocusable = false
        clipChildren = false
        clipToPadding = false

        // Center the 100dp core mesh inside this view
        val coreSizePx = (CORE_SIZE_DP * resources.displayMetrics.density).toInt()
        val coreParams = LayoutParams(coreSizePx, coreSizePx, Gravity.CENTER)
        addView(coreMesh, coreParams)

        // Inflate blend_background into coreMesh
        inflate(context, R.layout.blend_background, coreMesh)

        imageViewTS = coreMesh.findViewById(R.id.type1)
        imageViewBE = coreMesh.findViewById(R.id.type3)
        imageViewBG = coreMesh.findViewById(R.id.bg)
        rotateFrame = coreMesh.findViewById(R.id.rotate_frame)

        initializeImageSwitchers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            coreMesh.setRenderEffect(
                RenderEffect.createBlurEffect(FULL_BLUR_RADIUS, FULL_BLUR_RADIUS, Shader.TileMode.MIRROR)
            )
        }
    }

    private fun initializeImageSwitchers() {
        val animationIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in).apply {
            duration = VIEW_TRANSIT_DURATION
        }
        val animationOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out).apply {
            duration = VIEW_TRANSIT_DURATION
        }
        val factoryList = listOf(imageViewTS, imageViewBE, imageViewBG)

        factoryList.forEach { switcher ->
            switcher.setFactory {
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            }
            switcher.inAnimation = animationIn
            switcher.outAnimation = animationOut
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onTouchEvent(event: MotionEvent?): Boolean = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var p = parent
        while (p is ViewGroup) {
            p.clipChildren = false
            p.clipToPadding = false
            p = p.parent
        }
        adjustViewScale()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustViewScale()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRotationAnimation()
        job?.cancel()
    }

    private fun adjustViewScale() {
        doOnLayout {
            val windowMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.currentWindowMetrics
            } else null
            val screenHeight = (windowMetrics?.bounds?.height() ?: resources.displayMetrics.heightPixels).toFloat()
            val screenWidth = (windowMetrics?.bounds?.width() ?: resources.displayMetrics.widthPixels).toFloat()

            val coreSizePx = (CORE_SIZE_DP * resources.displayMetrics.density).coerceAtLeast(1f)

            val targetW = if (isMiniPlayer) {
                (parent as? View)?.width?.toFloat()?.takeIf { it > 0f } ?: width.toFloat().coerceAtLeast(1f)
            } else {
                maxOf(width.toFloat(), screenWidth)
            }
            val targetH = if (isMiniPlayer) {
                (parent as? View)?.height?.toFloat()?.takeIf { it > 0f } ?: height.toFloat().coerceAtLeast(1f)
            } else {
                maxOf(height.toFloat(), screenHeight)
            }

            // Scale coreMesh matching Accord's exact scale calculation without artificial magnification
            val finalScale = ceil((targetW / coreSizePx).coerceAtLeast(targetH / coreSizePx))

            coreMesh.scaleX = finalScale
            coreMesh.scaleY = finalScale
        }
    }

    fun setImageUrl(url: String?) {
        if (url == currentUrl && (job?.isActive == true || previousBitmap != null)) return
        currentUrl = url
        job?.cancel()

        if (url.isNullOrEmpty()) {
            clearImageViews()
            previousBitmap = null
            return
        }

        job = scope.launch(Dispatchers.IO) {
            val rawBitmap = loadBitmap(url)
            if (rawBitmap != null && !areBitmapsSame(rawBitmap, previousBitmap)) {
                enhanceBitmap(rawBitmap).let { enhancedBitmap ->
                    withContext(Dispatchers.Main) {
                        updateImageViews(enhancedBitmap)
                    }
                }
                previousBitmap = rawBitmap
            } else if (rawBitmap == null) {
                withContext(Dispatchers.Main) {
                    clearImageViews()
                }
                previousBitmap = null
            }
        }
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        val normalizedUrl = runCatching { url.resize(544, 544) }.getOrDefault(url)
        val bmpFromCoil = runCatching {
            val request = ImageRequest.Builder(context)
                .data(normalizedUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            result.image?.toBitmap()
        }.getOrNull()

        val fullBmp = bmpFromCoil ?: try {
            val connection = java.net.URL(normalizedUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val inputStream = connection.inputStream
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            bmp
        } catch (_: Throwable) {
            null
        }

        if (fullBmp == null) return null

        // Center-crop to square preserving the full artwork and its natural dark surrounding atmosphere
        val square = if (fullBmp.width != fullBmp.height) {
            val size = minOf(fullBmp.width, fullBmp.height)
            Bitmap.createBitmap(fullBmp, (fullBmp.width - size) / 2, (fullBmp.height - size) / 2, size, size)
        } else {
            fullBmp
        }

        return Bitmap.createScaledBitmap(square, PICTURE_SIZE, PICTURE_SIZE, true)
    }

    private fun updateImageViews(bitmap: Bitmap) {
        imageViewTS.setImageDrawable(cropTopLeftQuarter(bitmap).toDrawable(resources))
        imageViewBE.setImageDrawable(cropBottomRightQuarter(bitmap).toDrawable(resources))
        imageViewBG.setImageDrawable(bitmap.toDrawable(resources))
    }

    private fun clearImageViews() {
        imageViewTS.setImageDrawable(null)
        imageViewBE.setImageDrawable(null)
        imageViewBG.setImageDrawable(null)
    }

    fun setBlurRadius(radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            coreMesh.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR))
        }
    }

    fun animateBlurRadius(enlarge: Boolean, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val fromVal = if (enlarge) SHALLOW_BLUR_RADIUS else FULL_BLUR_RADIUS
            val toVal = if (enlarge) FULL_BLUR_RADIUS else SHALLOW_BLUR_RADIUS
            ValueAnimator.ofFloat(fromVal, toVal).apply {
                this.duration = duration
                addUpdateListener { animator ->
                    val radius = animator.animatedValue as Float
                    val renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                    post { coreMesh.setRenderEffect(renderEffect) }
                }
                start()
            }
        }
    }

    fun startRotationAnimation() {
        if (isAnimationOngoing) return
        if (this.alpha > 0) {
            handler.removeCallbacks(rotationRunnable)
            isAnimationOngoing = true
            handler.postDelayed(rotationRunnable, UPDATE_RUNNABLE_INTERVAL)
        }
    }

    fun stopRotationAnimation() {
        handler.removeCallbacks(rotationRunnable)
        isAnimationOngoing = false
    }

    private val rotationRunnable = object : Runnable {
        override fun run() {
            // Calm, elegant ambient drift matching the premium Accord feel
            imageViewTS.rotation = (imageViewTS.rotation + 0.6f) % CYCLE
            imageViewBE.rotation = (imageViewBE.rotation + 0.35f) % CYCLE
            rotateFrame.rotation = (rotateFrame.rotation - 0.3f) % CYCLE
            if (isAnimationOngoing) {
                handler.postDelayed(this, UPDATE_RUNNABLE_INTERVAL)
            }
        }
    }

    /**
     * Exact Accord bitmap enhancement: boosts saturation by SATURATION_FACTOR (2.5f)
     * using Android's native ColorMatrix without any artificial color or luminance shifts.
     */
    private fun enhanceBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val enhancedBitmap = createBitmap(width, height)
        enhancedBitmap.density = bitmap.density

        val enhancePaint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(SATURATION_FACTOR) }
        enhancePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        val canvas = Canvas(enhancedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, enhancePaint)

        return enhancedBitmap
    }

    private fun cropTopLeftQuarter(bitmap: Bitmap): Bitmap {
        val quarterWidth = bitmap.width / 2
        val quarterHeight = bitmap.height / 2
        return Bitmap.createBitmap(bitmap, 0, 0, quarterWidth, quarterHeight)
    }

    private fun cropBottomRightQuarter(bitmap: Bitmap): Bitmap {
        val quarterWidth = bitmap.width / 2
        val quarterHeight = bitmap.height / 2
        return Bitmap.createBitmap(bitmap, quarterWidth, quarterHeight, quarterWidth, quarterHeight)
    }

    private fun areBitmapsSame(b1: Bitmap?, b2: Bitmap?): Boolean {
        return b1 != null && b2 != null && b1.width == b2.width && b1.height == b2.height && b1.sameAs(b2)
    }

    /**
     * The 100dp x 100dp core mesh view that renders the children and contrast overlay,
     * which are blurred together by RenderEffect before being hardware-scaled across the screen.
     */
    private inner class CoreMeshLayout(context: Context) : FrameLayout(context) {
        init {
            isClickable = false
            isFocusable = false
            clipChildren = false
            clipToPadding = false
        }

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            canvas.drawColor(overlayColor)
        }
    }
}
