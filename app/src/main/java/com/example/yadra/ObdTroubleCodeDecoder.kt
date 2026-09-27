package com.example.yadra

object ObdTroubleCodeDecoder {

    private val hexTokenRegex =
        Regex("""\b[0-9A-Fa-f]{2}\b""")

    private val obdCodeRegex =
        Regex("""\b[PCBU][0-3][0-9A-Fa-f]{3}\b""")

    fun decode(response: String): List<String> {

        if (response.isBlank()) {
            return emptyList()
        }

        val normalized =
            response
                .uppercase()
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(">", " ")

        /*
         * بعضی ELM327 ها ممکن است کد را مستقیماً
         * به شکل P0301 برگردانند.
         */
        val directCodes =
            obdCodeRegex
                .findAll(normalized)
                .map { it.value }
                .toList()

        if (directCodes.isNotEmpty()) {
            return directCodes.distinct()
        }

        /*
         * حالت استاندارد پاسخ Mode 03:
         *
         * 43 01 31 00 00 00
         *
         * یا:
         *
         * 43 01 31 02 33 00
         */

        val tokens =
            hexTokenRegex
                .findAll(normalized)
                .map { it.value }
                .toList()

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val bytes =
            tokens.mapNotNull {
                it.toIntOrNull(16)
            }

        if (bytes.isEmpty()) {
            return emptyList()
        }

        val modeIndex =
            bytes.indexOfFirst {
                it == 0x43
            }

        if (modeIndex == -1) {
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        var index =
            modeIndex + 1

        while (index + 1 < bytes.size) {

            val first =
                bytes[index]

            val second =
                bytes[index + 1]

            if (
                first == 0 &&
                second == 0
            ) {
                index += 2
                continue
            }

            val code =
                decodePair(
                    first,
                    second
                )

            if (code != null) {
                result.add(code)
            }

            index += 2
        }

        return result.distinct()
    }

    private fun decodePair(
        first: Int,
        second: Int
    ): String? {

        if (
            first == 0 &&
            second == 0
        ) {
            return null
        }

        val type =
            when (
                (first and 0xC0) shr 6
            ) {

                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                3 -> 'U'

                else -> return null
            }

        val digit1 =
            (first and 0x30) shr 4

        val digit2 =
            first and 0x0F

        val digit3 =
            (second and 0xF0) shr 4

        val digit4 =
            second and 0x0F

        return buildString {

            append(type)

            append(digit1)

            append(
                digit2.toString(16)
                    .uppercase()
            )

            append(
                digit3.toString(16)
                    .uppercase()
            )

            append(
                digit4.toString(16)
                    .uppercase()
            )
        }
    }
}