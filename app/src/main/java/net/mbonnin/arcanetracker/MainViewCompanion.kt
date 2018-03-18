package net.mbonnin.arcanetracker

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Handler
import android.support.v7.content.res.AppCompatResources
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import timber.log.Timber

class MainViewCompanion(v: View) : ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private val mHandler: Handler
    private var mButtonWidth: Int = 0
    private val mParams: ViewManager.Params
    private val shadow: View
    private var mWidth = 0
    private val frameLayout: View
    private val mPadding: Int

    private var mRefY: Float = 0f
    private var mRefX: Float = 0f
    private val mTouchSlop = ViewConfiguration.get(v.context).scaledTouchSlop
    private var mDownY: Float = 0f
    private var mDownX: Float = 0f

    private val mViewManager: ViewManager

    private val playerView: View
    private val legacyView: View
    private val opponentView: View

    val handlesView = LayoutInflater.from(v.context).inflate(R.layout.handles_view, null) as HandlesView

    private val mAnimator: ValueAnimator

    var mainView: View
        internal set
    private var state: Int = 0
    private var mX: Int = 0

    private var mHandlesMovement: Int = 0

    private var direction = 1
    private var velocityRefX: Float = 0f
    private var velocityLastX: Float = 0f
    private var velocityRefTime: Long = 0

    var alphaSetting: Int
        get() = Settings.get(Settings.ALPHA, 100)
        set(progress) {
            val a = 0.5f + progress / 200f
            mainView.alpha = a
            handlesView.alpha = a
            Settings.set(Settings.ALPHA, progress)
        }

    private val mHandlesViewTouchListener = View.OnTouchListener { v, ev ->
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getRawX()
            mDownY = ev.getRawY()
            mRefX = handlesView.params.x.toFloat()
            mRefY = handlesView.params.y.toFloat()
            mHandlesMovement = 0

        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mHandlesMovement == 0) {
                if (Math.abs(ev.getRawX() - mDownX) > mTouchSlop) {
                    prepareAnimation()

                    velocityRefX = ev.getRawX()
                    velocityLastX = ev.getRawX()
                    velocityRefTime = System.nanoTime()

                    mHandlesMovement = HANDLES_MOVEMENT_X
                } else if (Math.abs(ev.getRawY() - mDownY) > mTouchSlop) {
                    mHandlesMovement = HANDLES_MOVEMENT_Y
                }
            }

            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                if ((ev.getRawX() - velocityLastX) * direction > 0) {
                    velocityLastX = ev.getRawX()
                } else {
                    direction = -direction
                    velocityRefX = ev.getRawX()
                    velocityLastX = ev.getRawX()
                    velocityRefTime = System.nanoTime()
                }
                var newX = (mRefX + ev.getRawX() - mDownX).toInt()
                if (newX > mWidth) {
                    newX = mWidth
                } else if (newX < 0) {
                    newX = 0
                }
                setX(newX)
            } else if (mHandlesMovement == HANDLES_MOVEMENT_Y) {
                handlesView.params.y = (mRefY + ev.getRawY() - mDownY).toInt()
                handlesView.update()
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL || ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                var velocity = 0f
                val timeDiff = System.nanoTime() - velocityRefTime
                if (timeDiff > 0) {
                    velocity = 1000f * 1000f * (ev.getRawX() - velocityRefX) / timeDiff
                }
                Timber.w("velocity: %f", velocity)
                if (mX < mWidth) {
                    if (velocity <= 0) {
                        animateXTo(0, velocity)
                    } else if (velocity > 0) {
                        animateXTo(mWidth, velocity)
                    }
                }
            }
        }

        mHandlesMovement != 0
    }

    init {
        mainView = v
        mViewManager = ViewManager.Companion.get()

        mHandler = Handler()

        mAnimator = ValueAnimator()
        mAnimator.addUpdateListener(this)
        mAnimator.addListener(this)
        frameLayout = v.findViewById(R.id.frameLayout)
        legacyView = v.findViewById(R.id.legacyView)
        opponentView = v.findViewById(R.id.opponentView)
        playerView = v.findViewById(R.id.playerView)
        shadow = v.findViewById(R.id.shadow)

        mWidth = Settings.get(Settings.DRAWER_WIDTH, 0)
        if (mWidth < minDrawerWidth || mWidth >= maxDrawerWidth) {
            mWidth = (0.33 * 0.5 * mViewManager.width.toDouble()).toInt()
        }
        mX = 0
        mPadding = Utils.dpToPx(5)

        mParams = ViewManager.Params()
        mParams.x = 0
        mParams.y = 0
        mParams.w = 0
        mParams.h = mViewManager.height

        sLegacyCompanion = LegacyDeckCompanion(legacyView)
        sOpponentCompanion = OpponentDeckCompanion(opponentView)
        sPlayerCompanion = PlayerDeckCompanion(playerView)

        handlesView.setListener(mHandlesViewTouchListener)

        mButtonWidth = Settings.get(Settings.BUTTON_WIDTH, 0)
        if (mButtonWidth < minButtonWidth || mButtonWidth >= maxButtonWidth) {
            val dp = if (Utils.is7InchesOrHigher) 50 else 30
            mButtonWidth = Utils.dpToPx(dp)
        }

        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(mButtonWidth, View.MeasureSpec.EXACTLY)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        handlesView.measure(wMeasureSpec, hMeasureSpec)
        handlesView.params.w = handlesView.measuredWidth
        handlesView.params.h = handlesView.measuredHeight
        handlesView.params.x = mPadding
        handlesView.params.y = ViewManager.Companion.get().height - handlesView.params.h - Utils.dpToPx(50)
        configureHandles(handlesView)

        setState(STATE_PLAYER, false)

        alphaSetting = alphaSetting
    }


    private val mHideViewRunnable = {
        mParams.w = 0
        mViewManager.updateView(mainView, mParams)
    }

    var isOpen: Boolean
        get() = mX != 0
        set(open) {
            setX(if (open) mWidth else 0)
            if (!open) {
                mParams.w = 1
                mViewManager.updateView(mainView, mParams)
            } else {
                mParams.w = mWidth
                mViewManager.updateView(mainView, mParams)
            }
        }

    val minDrawerWidth: Int
        get() = Utils.dpToPx(50)

    val maxDrawerWidth: Int
        get() = (0.4 * mViewManager.width).toInt()

    var drawerWidth: Int
        get() = mWidth
        set(width) {
            mWidth = width
            Settings.set(Settings.DRAWER_WIDTH, width)

            mAnimator.cancel()
            mParams.w = width
            mViewManager.updateView(mainView, mParams)
            setX(mWidth)
        }

    var buttonWidth: Int
        get() = mButtonWidth
        set(width) {
            Settings.set(Settings.BUTTON_WIDTH, width)
            mButtonWidth = width
            val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(mButtonWidth, View.MeasureSpec.EXACTLY)
            val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            handlesView.measure(wMeasureSpec, hMeasureSpec)

            handlesView.params.w = handlesView.measuredWidth
            handlesView.params.h = handlesView.measuredHeight
            handlesView.update()
        }

    val maxButtonWidth: Int
        get() = Utils.dpToPx(75)

    val minButtonWidth: Int
        get() = Utils.dpToPx(20)


    private fun prepareAnimation() {
        mHandler.removeCallbacks(mHideViewRunnable)
        mParams.w = mWidth
        mViewManager.updateView(mainView, mParams)
    }

    private fun animateXTo(targetX: Int, pixelPerMillisecond: Float) {
        var pixelPerMillisecond = pixelPerMillisecond
        pixelPerMillisecond = Math.abs(pixelPerMillisecond)
        if (pixelPerMillisecond < 0.6) {
            pixelPerMillisecond = 0.6f
        }
        mAnimator.cancel()

        mAnimator.interpolator = LinearInterpolator()
        if (pixelPerMillisecond > 0) {
            mAnimator.duration = (Math.abs(mX!! - targetX) / pixelPerMillisecond).toLong()
        } else {
            mAnimator.duration = 300
        }

        prepareAnimation()
        mAnimator.setIntValues(mX, targetX)
        mAnimator.start()
    }

    private fun animateXTo(targetX: Int) {
        mAnimator.cancel()
        mAnimator.interpolator = AccelerateDecelerateInterpolator()
        mAnimator.duration = 300

        prepareAnimation()
        mAnimator.setIntValues(mX, targetX)
        mAnimator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        setX(animation.animatedValue as Int)
    }

    private fun setX(x: Int) {
        //Timber.w("setX: %d", mX);
        mX = x
        mainView.translationX = (-mWidth + mX!!).toFloat()
        handlesView.params.x = mX!! + mPadding
        handlesView.update()
    }

    override fun onAnimationStart(animation: Animator) {

    }

    override fun onAnimationEnd(animation: Animator) {
        //Timber.w("onAnimationEnd: %d", mX);
        if (mX == 0) {
            /**
             * XXX: somehow if I do this too early, there a small glitch on screen...
             */
            mHandler.postDelayed(mHideViewRunnable, 300)
        }
    }

    override fun onAnimationCancel(animation: Animator) {

    }

    override fun onAnimationRepeat(animation: Animator) {

    }

    internal inner class ClickListener(private val newState: Int) : View.OnClickListener {

        override fun onClick(v: View) {
            if (state == newState && mX == mWidth) {
                setState(state, false)
            } else {
                setState(newState, true)
            }
        }
    }

    fun setState(newState: Int, newOpen: Boolean) {
        if (newOpen) {
            opponentView.visibility = View.GONE
            playerView.visibility = View.GONE
            legacyView.visibility = View.GONE
            when (newState) {
                STATE_PLAYER -> {
                    playerView.visibility = View.VISIBLE
                    Onboarding.playerHandleClicked()
                }
                STATE_OPPONENT -> {
                    opponentView.visibility = View.VISIBLE
                    Onboarding.opponentHandleClicked()
                }
                STATE_LEGACY -> {
                    legacyView.visibility = View.VISIBLE
                    Onboarding.legacyHandleClicked()
                }
            }
        }

        animateXTo(if (newOpen) mWidth else 0)

        state = newState
    }


    fun show(show: Boolean) {
        if (show) {
            mViewManager.addView(mainView, mParams)
        } else {
            mViewManager.removeView(mainView)
        }
        handlesView.show(show)
    }

    private fun configureHandles(v: View) {
        var handleView = v.findViewById<HandleView>(R.id.settingsHandle)
        var drawable = v.context.resources.getDrawable(R.drawable.settings_handle)
        handleView.init(drawable, v.context.resources.getColor(R.color.gray))
        handleView.setOnClickListener { v2 ->
            val view = LayoutInflater.from(v.context).inflate(R.layout.more_view, null)

            val d = AppCompatResources.getDrawable(v.context, R.drawable.heart)
            d!!.setBounds(0, 0, Utils.dpToPx(26), Utils.dpToPx(24))
            (view.findViewById<View>(R.id.donate) as TextView).setCompoundDrawables(null, null, d, null)
            (view.findViewById<View>(R.id.donate) as TextView).compoundDrawablePadding = Utils.dpToPx(13)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            view.findViewById<View>(R.id.settings).setOnClickListener { v3 ->
                mViewManager.removeView(view)
                SettingsCompanion.show()
            }
            view.findViewById<View>(R.id.hsReplayHistory).setOnClickListener { v3 ->
                mViewManager.removeView(view)
                HistoryCompanion.show()
            }
            val donateView = view.findViewById<View>(R.id.donate)
            if (true) {
                donateView.setOnClickListener { v3 ->
                    mViewManager.removeView(view)
                    val intent = Intent()
                    intent.setClass(ArcaneTrackerApplication.context, DonateActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    ArcaneTrackerApplication.context.startActivity(intent)
                }
            } else {
                donateView.visibility = View.GONE
            }
            view.findViewById<View>(R.id.quit).setOnClickListener { v3 -> Utils.exitApp() }

            val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(wMeasureSpec, wMeasureSpec)

            val a = IntArray(2)
            v2.getLocationOnScreen(a)

            val params = ViewManager.Params()
            params.x = a[0] + v2.width / 2
            params.y = a[1] + v2.height / 2 - view.measuredHeight
            params.w = view.measuredWidth
            params.h = view.measuredHeight

            mViewManager.addModalView(view, params)
        }

        handleView = v.findViewById(R.id.legacyHandle)
        drawable = v.context.resources.getDrawable(R.drawable.box)
        handleView.init(drawable, v.context.resources.getColor(R.color.gray))
        handleView.setOnClickListener(ClickListener(STATE_LEGACY))
        if (!DeckList.hasValidDeck()) {
            handleView.visibility = View.GONE
        }

        handleView = v.findViewById(R.id.opponentHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.opponentColor))
        handleView.setOnClickListener(ClickListener(STATE_OPPONENT))

        handleView = v.findViewById(R.id.playerHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.colorPrimary))
        handleView.setOnClickListener(ClickListener(STATE_PLAYER))
    }


    companion object {
        private var sOpponentCompanion: DeckCompanion? = null
        private var sLegacyCompanion: DeckCompanion? = null
        private var sPlayerCompanion: DeckCompanion? = null

        val STATE_PLAYER = 0
        val STATE_OPPONENT = 1
        val STATE_LEGACY = 2

        private val HANDLES_MOVEMENT_X = 1
        private val HANDLES_MOVEMENT_Y = 2

        val legacyCompanion: DeckCompanion
            get() {
                MainViewCompanion.get()
                return sLegacyCompanion!!
            }

        val opponentCompanion: DeckCompanion
            get() {
                MainViewCompanion.get()
                return sOpponentCompanion!!
            }

        val playerCompanion: DeckCompanion
            get() {
                MainViewCompanion.get()
                return sPlayerCompanion!!
            }

        private var sMainCompanion: MainViewCompanion? = null

        fun get(): MainViewCompanion {
            if (sMainCompanion == null) {
                val view = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.main_view, null)
                sMainCompanion = MainViewCompanion(view)
            }

            return sMainCompanion!!
        }
    }
}
