package com.example.yadra

import androidx.annotation.StringRes

data class ObdCodeInfo(
    val code: String,
    @StringRes val typeRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val categoryRes: Int,
    val isCngRelated: Boolean = false
)