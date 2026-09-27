package com.example.yadra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object EcuProfileStore {

    private const val PREFS = "yadra_learned_ecu_profiles"
    private const val KEY_PROFILES = "profiles"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    @Synchronized
    fun getAll(context: Context): List<EcuConnectionProfile> {

        val raw = prefs(context)
            .getString(KEY_PROFILES, null)
            ?: return emptyList()

        return try {

            val array = JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {

                    parseProfile(
                        array.getJSONObject(i)
                    )?.let(::add)
                }
            }

        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getForSource(
        context: Context,
        source: ConnectionSource.Source
    ): List<EcuConnectionProfile> {

        return getAll(context)
            .filter {
                it.source == source.name
            }
            .sortedWith(
                compareByDescending<EcuConnectionProfile> {
                    it.confidence
                }.thenByDescending {
                    it.lastSeen
                }
            )
    }

    @Synchronized
    fun save(
        context: Context,
        profile: EcuConnectionProfile
    ) {

        val profiles =
            getAll(context).toMutableList()

        val index =
            profiles.indexOfFirst {
                it.id == profile.id
            }

        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles.add(profile)
        }

        write(
            context,
            profiles
        )
    }

    @Synchronized
    fun saveOrUpdate(
        context: Context,
        profile: EcuConnectionProfile
    ) {

        val profiles =
            getAll(context).toMutableList()

        val existingIndex =
            profiles.indexOfFirst {
                it.source == profile.source &&
                        it.protocolCode == profile.protocolCode &&
                        it.requestId == profile.requestId &&
                        it.ecuAddress == profile.ecuAddress
            }

        if (existingIndex >= 0) {

            val old = profiles[existingIndex]

            profiles[existingIndex] =
                profile.copy(
                    id = old.id,
                    createdAt = old.createdAt,
                    successCount =
                        old.successCount + 1,
                    lastSeen =
                        System.currentTimeMillis()
                )

        } else {

            profiles.add(profile)
        }

        write(
            context,
            profiles
        )
    }

    fun findById(
        context: Context,
        id: String
    ): EcuConnectionProfile? {

        return getAll(context)
            .firstOrNull {
                it.id == id
            }
    }

    fun findBest(
        context: Context,
        source: ConnectionSource.Source
    ): EcuConnectionProfile? {

        return getForSource(
            context,
            source
        ).firstOrNull()
    }

    @Synchronized
    fun delete(
        context: Context,
        id: String
    ) {

        write(
            context,
            getAll(context)
                .filterNot {
                    it.id == id
                }
        )
    }

    @Synchronized
    fun clear(
        context: Context
    ) {

        prefs(context)
            .edit()
            .remove(KEY_PROFILES)
            .apply()
    }

    private fun write(
        context: Context,
        profiles: List<EcuConnectionProfile>
    ) {

        val array = JSONArray()

        profiles.forEach {
            array.put(
                serializeProfile(it)
            )
        }

        prefs(context)
            .edit()
            .putString(
                KEY_PROFILES,
                array.toString()
            )
            .apply()
    }

    private fun serializeProfile(
        profile: EcuConnectionProfile
    ): JSONObject {

        return JSONObject().apply {

            put("id", profile.id)
            put("source", profile.source)

            put(
                "protocolCode",
                profile.protocolCode
            )

            put(
                "protocolName",
                profile.protocolName
            )

            put(
                "scanType",
                profile.scanType
            )

            putNullable(
                "elmProtocol",
                profile.elmProtocol
            )

            putNullable(
                "baudRate",
                profile.baudRate
            )

            putNullable(
                "canExtended",
                profile.canExtended
            )

            putNullable(
                "requestId",
                profile.requestId
            )

            put(
                "responseIds",
                JSONArray(
                    profile.responseIds
                )
            )

            putNullable(
                "ecuAddress",
                profile.ecuAddress
            )

            putNullable(
                "ecuIdentifier",
                profile.ecuIdentifier
            )

            putNullable(
                "vin",
                profile.vin
            )

            putNullable(
                "softwareVersion",
                profile.softwareVersion
            )

            putNullable(
                "hardwareVersion",
                profile.hardwareVersion
            )

            put(
                "successfulCommands",
                JSONArray(
                    profile.successfulCommands
                )
            )

            val responses = JSONObject()

            profile.fingerprintResponses
                .forEach { (key, value) ->
                    responses.put(
                        key,
                        value
                    )
                }

            put(
                "fingerprintResponses",
                responses
            )

            put(
                "elmVersion",
                profile.elmVersion
            )

            put(
                "createdAt",
                profile.createdAt
            )

            put(
                "lastSeen",
                profile.lastSeen
            )

            put(
                "successCount",
                profile.successCount
            )

            put(
                "failureCount",
                profile.failureCount
            )

            put(
                "confidence",
                profile.confidence
            )
        }
    }

    private fun parseProfile(
        json: JSONObject
    ): EcuConnectionProfile? {

        return try {

            val responseIds =
                jsonArrayToList(
                    json.optJSONArray(
                        "responseIds"
                    )
                )

            val commands =
                jsonArrayToList(
                    json.optJSONArray(
                        "successfulCommands"
                    )
                )

            val fingerprint =
                mutableMapOf<String, String>()

            val fingerprintJson =
                json.optJSONObject(
                    "fingerprintResponses"
                )

            if (fingerprintJson != null) {

                fingerprintJson.keys()
                    .forEach { key ->

                        fingerprint[key] =
                            fingerprintJson.optString(
                                key
                            )
                    }
            }

            EcuConnectionProfile(

                id =
                    json.optString("id"),

                source =
                    json.optString("source"),

                protocolCode =
                    json.optString("protocolCode"),

                protocolName =
                    json.optString("protocolName"),

                scanType =
                    json.optString("scanType"),

                elmProtocol =
                    if (json.has("elmProtocol") && !json.isNull("elmProtocol")) {
                        json.optInt("elmProtocol").toString()
                    } else {
                        ""
                    },

                baudRate =
                    json.optIntOrNull(
                        "baudRate"
                    ),

                canExtended =
                    json.optBooleanOrNull(
                        "canExtended"
                    ),

                requestId =
                    json.optStringOrNull(
                        "requestId"
                    ),

                responseIds =
                    responseIds,

                ecuAddress =
                    json.optStringOrNull(
                        "ecuAddress"
                    ),

                ecuIdentifier =
                    json.optStringOrNull(
                        "ecuIdentifier"
                    ),

                vin =
                    json.optStringOrNull(
                        "vin"
                    ),

                softwareVersion =
                    json.optStringOrNull(
                        "softwareVersion"
                    ),

                hardwareVersion =
                    json.optStringOrNull(
                        "hardwareVersion"
                    ),

                successfulCommands =
                    commands,

                fingerprintResponses =
                    fingerprint,

                elmVersion =
                    json.optString(
                        "elmVersion"
                    ),

                createdAt =
                    json.optLong(
                        "createdAt"
                    ),

                lastSeen =
                    json.optLong(
                        "lastSeen"
                    ),

                successCount =
                    json.optInt(
                        "successCount"
                    ),

                failureCount =
                    json.optInt(
                        "failureCount"
                    ),

                confidence =
                    json.optInt(
                        "confidence"
                    )
            )

        } catch (_: Exception) {
            null
        }
    }

    private fun jsonArrayToList(
        array: JSONArray?
    ): List<String> {

        if (array == null) {
            return emptyList()
        }

        return buildList {

            for (i in 0 until array.length()) {
                add(
                    array.optString(i)
                )
            }
        }
    }

    private fun JSONObject.putNullable(
        key: String,
        value: Any?
    ) {

        if (value == null) {
            put(
                key,
                JSONObject.NULL
            )
        } else {
            put(
                key,
                value
            )
        }
    }

    private fun JSONObject.optStringOrNull(
        key: String
    ): String? {

        if (!has(key) || isNull(key)) {
            return null
        }

        return optString(key)
            .takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optIntOrNull(
        key: String
    ): Int? {

        if (!has(key) || isNull(key)) {
            return null
        }

        return optInt(key)
    }

    private fun JSONObject.optBooleanOrNull(
        key: String
    ): Boolean? {

        if (!has(key) || isNull(key)) {
            return null
        }

        return optBoolean(key)
    }
}