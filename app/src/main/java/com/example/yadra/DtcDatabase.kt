package com.example.yadra

import android.content.Context
import com.dtcdatabase.DTCDatabase

data class DtcInfo(
    val code: String,
    val description: String,
    val type: String,
    val typeName: String,
    val manufacturer: String
)

object DtcDatabase {

    private fun database(
        context: Context
    ): DTCDatabase {

        return DTCDatabase.getInstance(
            context.applicationContext
        )
    }

    fun initialize(
        context: Context
    ) {
        database(context)
    }

    fun get(
        context: Context,
        code: String,
        manufacturer: String? = null
    ): DtcInfo? {

        val cleanCode =
            code
                .trim()
                .uppercase()

        if (cleanCode.isBlank()) {
            return null
        }

        return try {

            val db =
                database(context)

            val dtc =
                if (
                    manufacturer != null &&
                    manufacturer.isNotBlank()
                ) {

                    db.getDTC(
                        cleanCode,
                        manufacturer
                            .trim()
                            .uppercase()
                    )

                } else {

                    db.getDTC(
                        cleanCode
                    )
                }

            if (dtc != null) {

                DtcInfo(

                    code =
                        dtc.code,

                    description =
                        dtc.description,

                    type =
                        dtc.type,

                    typeName =
                        dtc.getTypeName(),

                    manufacturer =
                        dtc.manufacturer
                )

            } else {

                // Try generic lookup explicitly
                val description =
                    db.getDescription(
                        cleanCode
                    )

                if (
                    description.isNullOrBlank()
                ) {

                    null

                } else {

                    DtcInfo(

                        code =
                            cleanCode,

                        description =
                            description,

                        type =
                            cleanCode
                                .substring(
                                    0,
                                    1
                                ),

                        typeName =
                            when (
                                cleanCode
                                    .first()
                            ) {

                                'P' ->
                                    "Powertrain"

                                'B' ->
                                    "Body"

                                'C' ->
                                    "Chassis"

                                'U' ->
                                    "Network"

                                else ->
                                    "Unknown"
                            },

                        manufacturer =
                            "GENERIC"
                    )
                }
            }

        } catch (_: Exception) {

            null
        }
    }

    fun close() {
        // The official wrapper manages its own
        // SQLiteOpenHelper lifecycle.
    }
}