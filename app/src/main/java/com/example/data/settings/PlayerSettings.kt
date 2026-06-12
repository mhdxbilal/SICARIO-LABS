package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences

object PlayerSettings {
    private const val PREFS_NAME = "mhdx_player_prefs"

    // Keys
    private const val KEY_SEEK_BUTTONS = "seek_buttons"
    private const val KEY_FWD_BWD_DELAY = "fwd_bwd_delay"
    private const val KEY_LONG_TAP_DELAY = "long_tap_delay"
    private const val KEY_CONTROLS_HIDING_DELAY = "controls_hiding_delay"
    private const val KEY_VIDEOS_TRANSITION = "videos_transition"
    private const val KEY_LOCK_WITH_SENSOR = "lock_with_sensor"
    private const val KEY_DOUBLE_TAP_DELAY = "double_tap_delay"
    private const val KEY_DOUBLE_TAP_PLAY_PAUSE = "double_tap_play_pause"
    private const val KEY_TAKE_SCREENSHOT = "take_screenshot"
    private const val KEY_ENABLE_FASTPLAY = "enable_fastplay"
    private const val KEY_FASTPLAY_SPEED = "fastplay_speed"

    // Gestures Keys
    private const val KEY_VOLUME_GESTURE = "volume_gesture"
    private const val KEY_BRIGHTNESS_GESTURE = "brightness_gesture"
    private const val KEY_SAVE_BRIGHTNESS = "save_brightness"
    private const val KEY_SAVED_BRIGHTNESS_VAL = "saved_brightness_val"
    private const val KEY_SWIPE_TO_SEEK = "swipe_to_seek"
    private const val KEY_TWO_FINGER_ZOOM = "two_finger_zoom"
    private const val KEY_DOUBLE_TAP_TO_SEEK = "double_tap_to_seek"

    // Dashboard display and sorting keys
    private const val KEY_VIEW_MODE = "display_view_mode"
    private const val KEY_SHOW_ONLY_FAVORITES = "show_only_favorites"
    private const val KEY_PLAYBACK_ACTION = "playback_action"
    private const val KEY_SORT_FIELD = "sort_field"
    private const val KEY_SORT_ORDER = "sort_order"
    private const val KEY_GROUP_BY = "group_by_preference"
    private const val KEY_AI_SUMMARIZATION = "enable_ai_summaries"
    private const val KEY_EQUALIZER_ENABLED = "equalizer_enabled"
    private const val KEY_EQUALIZER_BAND_PREFIX = "equalizer_band_"
    private const val KEY_DECODER_MODE = "decoder_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGroupBySetting(context: Context): String = getPrefs(context).getString(KEY_GROUP_BY, "none") ?: "none"
    fun setGroupBySetting(context: Context, value: String) = getPrefs(context).edit().putString(KEY_GROUP_BY, value).apply()

    fun getAiSummariesEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_AI_SUMMARIZATION, true)
    fun setAiSummariesEnabled(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_AI_SUMMARIZATION, value).apply()

    fun getEqualizerEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_EQUALIZER_ENABLED, false)
    fun setEqualizerEnabled(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_EQUALIZER_ENABLED, value).apply()

    fun getEqualizerBand(context: Context, bandIndex: Int): Float = getPrefs(context).getFloat(KEY_EQUALIZER_BAND_PREFIX + bandIndex, 0.0f)
    fun setEqualizerBand(context: Context, bandIndex: Int, value: Float) = getPrefs(context).edit().putFloat(KEY_EQUALIZER_BAND_PREFIX + bandIndex, value).apply()

    fun getSeekButtons(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SEEK_BUTTONS, true)
    fun setSeekButtons(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SEEK_BUTTONS, value).apply()

    fun getFwdBwdDelay(context: Context): Int = getPrefs(context).getInt(KEY_FWD_BWD_DELAY, 3)
    fun setFwdBwdDelay(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_FWD_BWD_DELAY, value).apply()

    fun getLongTapDelay(context: Context): Int = getPrefs(context).getInt(KEY_LONG_TAP_DELAY, 4)
    fun setLongTapDelay(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_LONG_TAP_DELAY, value).apply()

    fun getControlsHidingDelay(context: Context): Int = getPrefs(context).getInt(KEY_CONTROLS_HIDING_DELAY, 3)
    fun setControlsHidingDelay(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_CONTROLS_HIDING_DELAY, value).apply()

    fun getVideosTransition(context: Context): Boolean = getPrefs(context).getBoolean(KEY_VIDEOS_TRANSITION, true)
    fun setVideosTransition(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_VIDEOS_TRANSITION, value).apply()

    fun getLockWithSensor(context: Context): Boolean = getPrefs(context).getBoolean(KEY_LOCK_WITH_SENSOR, true)
    fun setLockWithSensor(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_LOCK_WITH_SENSOR, value).apply()

    fun getDoubleTapDelay(context: Context): Int = getPrefs(context).getInt(KEY_DOUBLE_TAP_DELAY, 3)
    fun setDoubleTapDelay(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_DOUBLE_TAP_DELAY, value).apply()

    fun getDoubleTapPlayPause(context: Context): Boolean = getPrefs(context).getBoolean(KEY_DOUBLE_TAP_PLAY_PAUSE, true)
    fun setDoubleTapPlayPause(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_DOUBLE_TAP_PLAY_PAUSE, value).apply()

    fun getTakeScreenshot(context: Context): String = getPrefs(context).getString(KEY_TAKE_SCREENSHOT, "Disabled") ?: "Disabled"
    fun setTakeScreenshot(context: Context, value: String) = getPrefs(context).edit().putString(KEY_TAKE_SCREENSHOT, value).apply()

    fun getEnableFastplay(context: Context): Boolean = getPrefs(context).getBoolean(KEY_ENABLE_FASTPLAY, false)
    fun setEnableFastplay(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_ENABLE_FASTPLAY, value).apply()

    fun getFastplaySpeed(context: Context): Float = getPrefs(context).getFloat(KEY_FASTPLAY_SPEED, 2.0f)
    fun setFastplaySpeed(context: Context, value: Float) = getPrefs(context).edit().putFloat(KEY_FASTPLAY_SPEED, value).apply()

    // Gestures Getters/Setters
    fun getVolumeGesture(context: Context): Boolean = getPrefs(context).getBoolean(KEY_VOLUME_GESTURE, true)
    fun setVolumeGesture(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_VOLUME_GESTURE, value).apply()

    fun getBrightnessGesture(context: Context): Boolean = getPrefs(context).getBoolean(KEY_BRIGHTNESS_GESTURE, true)
    fun setBrightnessGesture(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_BRIGHTNESS_GESTURE, value).apply()

    fun getSaveBrightness(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SAVE_BRIGHTNESS, false)
    fun setSaveBrightness(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SAVE_BRIGHTNESS, value).apply()

    fun getSavedBrightnessVal(context: Context): Float = getPrefs(context).getFloat(KEY_SAVED_BRIGHTNESS_VAL, 0.5f)
    fun setSavedBrightnessVal(context: Context, value: Float) = getPrefs(context).edit().putFloat(KEY_SAVED_BRIGHTNESS_VAL, value).apply()

    fun getSwipeToSeek(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SWIPE_TO_SEEK, true)
    fun setSwipeToSeek(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SWIPE_TO_SEEK, value).apply()

    fun getTwoFingerZoom(context: Context): Boolean = getPrefs(context).getBoolean(KEY_TWO_FINGER_ZOOM, true)
    fun setTwoFingerZoom(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_TWO_FINGER_ZOOM, value).apply()

    fun getDoubleTapToSeek(context: Context): Boolean = getPrefs(context).getBoolean(KEY_DOUBLE_TAP_TO_SEEK, true)
    fun setDoubleTapToSeek(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_DOUBLE_TAP_TO_SEEK, value).apply()

    // Dashboard display and sorting getters/setters
    fun getViewMode(context: Context): String = getPrefs(context).getString(KEY_VIEW_MODE, "list") ?: "list"
    fun setViewMode(context: Context, value: String) = getPrefs(context).edit().putString(KEY_VIEW_MODE, value).apply()

    fun getShowOnlyFavorites(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_ONLY_FAVORITES, false)
    fun setShowOnlyFavorites(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_ONLY_FAVORITES, value).apply()

    fun getPlaybackAction(context: Context): String = getPrefs(context).getString(KEY_PLAYBACK_ACTION, "play_all") ?: "play_all"
    fun setPlaybackAction(context: Context, value: String) = getPrefs(context).edit().putString(KEY_PLAYBACK_ACTION, value).apply()

    fun getSortField(context: Context): String = getPrefs(context).getString(KEY_SORT_FIELD, "name") ?: "name"
    fun setSortField(context: Context, value: String) = getPrefs(context).edit().putString(KEY_SORT_FIELD, value).apply()

    fun getSortOrder(context: Context): String = getPrefs(context).getString(KEY_SORT_ORDER, "asc") ?: "asc"
    fun setSortOrder(context: Context, value: String) = getPrefs(context).edit().putString(KEY_SORT_ORDER, value).apply()

    fun getDecoderMode(context: Context): String = getPrefs(context).getString(KEY_DECODER_MODE, "hardware") ?: "hardware"
    fun setDecoderMode(context: Context, value: String) = getPrefs(context).edit().putString(KEY_DECODER_MODE, value).apply()
}
