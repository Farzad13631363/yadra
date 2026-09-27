package com.example.yadra

data class ObdSensor(
    val id: String,
    val name: String,
    val pid: String,
    val unit: String,
    val minimum: Float,
    val maximum: Float
)