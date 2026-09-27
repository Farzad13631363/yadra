package com.example.yadra

object EcuModuleScanner {

    data class ScanResult(
        val success: Boolean,
        val protocol: String,
        val ecuResponse: String,
        val supportedPids: List<String>,
        val message: String
    )

    fun scan(): ScanResult {

        return try {

            when (ConnectionSource.activeSource) {

                ConnectionSource.Source.WIFI -> {

                    if (!YadraConnectionManager.elmConnected) {
                        return ScanResult(
                            success = false,
                            protocol = "",
                            ecuResponse = "",
                            supportedPids = emptyList(),
                            message = "Wi-Fi / ELM327 متصل نیست"
                        )
                    }

                    val protocol =
                        YadraConnectionManager
                            .detectedProtocol
                            .ifBlank {
                                "Unknown"
                            }

                    val response =
                        YadraConnectionManager
                            .sendCommand("0100")

                    parseResponse(
                        protocol = protocol,
                        response = response
                    )
                }

                ConnectionSource.Source.BLUETOOTH -> {

                    if (!BluetoothConnectionManager.elmConnected) {
                        return ScanResult(
                            success = false,
                            protocol = "",
                            ecuResponse = "",
                            supportedPids = emptyList(),
                            message = "Bluetooth / ELM327 متصل نیست"
                        )
                    }

                    val protocol =
                        BluetoothConnectionManager
                            .protocol
                            .ifBlank {
                                "Unknown"
                            }

                    val response =
                        BluetoothConnectionManager
                            .sendActiveCommand("0100")

                    parseResponse(
                        protocol = protocol,
                        response = response
                    )
                }

                ConnectionSource.Source.NONE -> {

                    ScanResult(
                        success = false,
                        protocol = "",
                        ecuResponse = "",
                        supportedPids = emptyList(),
                        message = "هیچ اتصال فعالی وجود ندارد"
                    )
                }
            }

        } catch (e: Exception) {

            ScanResult(
                success = false,
                protocol = "",
                ecuResponse = "",
                supportedPids = emptyList(),
                message =
                    e.message
                        ?: "خطا هنگام اسکن ECU"
            )
        }
    }

    private fun parseResponse(
        protocol: String,
        response: String
    ): ScanResult {

        val normalized =
            response
                .uppercase()
                .replace(
                    Regex("[^0-9A-F]"),
                    ""
                )

        val index =
            normalized.indexOf("4100")

        if (index < 0) {

            return ScanResult(
                success = false,
                protocol = protocol,
                ecuResponse = response,
                supportedPids = emptyList(),
                message =
                    "پاسخ معتبر 0100 از ECU دریافت نشد"
            )
        }

        if (
            normalized.length <
            index + 12
        ) {

            return ScanResult(
                success = false,
                protocol = protocol,
                ecuResponse = response,
                supportedPids = emptyList(),
                message =
                    "پاسخ ECU ناقص است"
            )
        }

        val pidBytes =
            try {

                val a =
                    normalized
                        .substring(
                            index + 4,
                            index + 6
                        )
                        .toInt(16)

                val b =
                    normalized
                        .substring(
                            index + 6,
                            index + 8
                        )
                        .toInt(16)

                val c =
                    normalized
                        .substring(
                            index + 8,
                            index + 10
                        )
                        .toInt(16)

                val d =
                    normalized
                        .substring(
                            index + 10,
                            index + 12
                        )
                        .toInt(16)

                intArrayOf(
                    a,
                    b,
                    c,
                    d
                )

            } catch (_: Exception) {

                return ScanResult(
                    success = false,
                    protocol = protocol,
                    ecuResponse = response,
                    supportedPids = emptyList(),
                    message =
                        "خطا در تحلیل پاسخ ECU"
                )
            }

        val supported =
            mutableListOf<String>()

        for (
        byteIndex in 0..3
        ) {

            val value =
                pidBytes[byteIndex]

            for (
            bit in 0..7
            ) {

                if (
                    value and
                    (1 shl (7 - bit)) != 0
                ) {

                    val pid =
                        byteIndex * 8 +
                                bit + 1

                    supported.add(
                        String.format(
                            "%02X",
                            pid
                        )
                    )
                }
            }
        }

        return ScanResult(
            success = true,
            protocol = protocol,
            ecuResponse = response,
            supportedPids = supported,
            message =
                "ECU با موفقیت پاسخ داد"
        )
    }
}
