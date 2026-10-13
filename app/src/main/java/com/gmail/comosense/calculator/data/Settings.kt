package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.common.Constraints

data class Settings(
    val calculationPrecision: Int =
        SettingsDataStoreConstraints.DEFAULT_CALCULATION_PRECISION,
)

internal object SettingsDataStoreConstraints {
    const val DEFAULT_CALCULATION_PRECISION: Int =
        Constraints.DEFAULT_CALCULATION_PRECISION
}
