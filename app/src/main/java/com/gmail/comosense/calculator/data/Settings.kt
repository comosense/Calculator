package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.common.Constraints

internal object SettingsDataStoreConstraints {
    const val DEFAULT_PRECISION: Int = Constraints.DEFAULT_PRECISION
    val VALID_PRECISIONS: List<Int> = Constraints.VALID_PRECISIONS
}

data class Settings(
    val precision: Int = SettingsDataStoreConstraints.DEFAULT_PRECISION,
)
