package com.example.deepseekwidget.ui

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Glance 小组件 DataStore 状态定义键
 * 注意：根据 Zero-Plaintext Policy，API Key 严禁放入此类明文键中。
 */
object WidgetPrefsKeys {
    val TOTAL_BALANCE = stringPreferencesKey("total_balance")
    val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    val GRANTED_BALANCE = stringPreferencesKey("granted_balance")
    val TOPPED_UP_BALANCE = stringPreferencesKey("topped_up_balance")
    val UPDATED_AT = stringPreferencesKey("updated_at")
    val IS_LOADING = booleanPreferencesKey("is_loading")
    val HAS_ERROR = booleanPreferencesKey("has_error")
    val ERROR_MSG = stringPreferencesKey("error_msg")
    val IS_LOW_BALANCE = booleanPreferencesKey("is_low_balance")
    val LAST_REFRESH_TIMESTAMP = longPreferencesKey("last_refresh_timestamp")
}
