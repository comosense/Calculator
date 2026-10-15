package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.common.Constraints

internal object SettingsDataStoreConstraints {
    const val DEFAULT_PRECISION: Int = Constraints.DEFAULT_PRECISION
    val VALID_PRECISION_RANGE: IntRange =
        Constraints.MIN_PRECISION..Constraints.MAX_PRECISION
}

data class Settings(
    val precision: Int = SettingsDataStoreConstraints.DEFAULT_PRECISION,
)
