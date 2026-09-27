package com.example.yadra

import android.content.Context

object VehicleProfileManager {

    private const val PREFS_NAME =
        "yadra_vehicle_profiles"

    private const val KEY_COUNT =
        "profile_count"

    private const val KEY_ACTIVE_INDEX =
        "active_profile_index"


    // =====================================================
    // SAVE
    // =====================================================

    fun save(
        context: Context,
        profile: VehicleProfile
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val count =
            prefs.getInt(
                KEY_COUNT,
                0
            )

        prefs.edit()
            .putString(
                "vehicle_type_$count",
                profile.vehicleType
            )
            .putString(
                "model_$count",
                profile.model
            )
            .putString(
                "mileage_$count",
                profile.mileage
            )
            .putString(
                "ecu_type_$count",
                profile.ecuType
            )
            .putString(
                "fuel_type_$count",
                profile.fuelType
            )
            .putInt(
                KEY_COUNT,
                count + 1
            )
            .putInt(
                KEY_ACTIVE_INDEX,
                count
            )
            .apply()
    }


    // =====================================================
    // LOAD ALL
    // =====================================================

    fun loadAll(
        context: Context
    ): MutableList<VehicleProfile> {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val count =
            prefs.getInt(
                KEY_COUNT,
                0
            )

        val profiles =
            mutableListOf<VehicleProfile>()

        for (
        index in 0 until count
        ) {

            profiles.add(
                VehicleProfile(

                    vehicleType =
                        prefs.getString(
                            "vehicle_type_$index",
                            ""
                        ) ?: "",

                    model =
                        prefs.getString(
                            "model_$index",
                            ""
                        ) ?: "",

                    mileage =
                        prefs.getString(
                            "mileage_$index",
                            ""
                        ) ?: "",

                    ecuType =
                        prefs.getString(
                            "ecu_type_$index",
                            ""
                        ) ?: "",

                    fuelType =
                        prefs.getString(
                            "fuel_type_$index",
                            ""
                        ) ?: ""
                )
            )
        }

        return profiles
    }


    // =====================================================
    // LOAD ACTIVE PROFILE
    // =====================================================

    fun load(
        context: Context
    ): VehicleProfile {

        val profiles =
            loadAll(context)

        if (profiles.isEmpty()) {
            return VehicleProfile()
        }

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val activeIndex =
            prefs.getInt(
                KEY_ACTIVE_INDEX,
                0
            )

        if (
            activeIndex >= 0 &&
            activeIndex < profiles.size
        ) {

            return profiles[activeIndex]
        }

        return profiles[0]
    }


    // =====================================================
    // ACTIVE INDEX
    // =====================================================

    fun getActiveIndex(
        context: Context
    ): Int {

        val profiles =
            loadAll(context)

        if (profiles.isEmpty()) {
            return -1
        }

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val index =
            prefs.getInt(
                KEY_ACTIVE_INDEX,
                0
            )

        return if (
            index >= 0 &&
            index < profiles.size
        ) {
            index
        } else {
            0
        }
    }


    // =====================================================
    // SELECT PROFILE
    // =====================================================

    fun setActiveProfile(
        context: Context,
        index: Int
    ) {

        val profiles =
            loadAll(context)

        if (
            index < 0 ||
            index >= profiles.size
        ) {
            return
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                KEY_ACTIVE_INDEX,
                index
            )
            .apply()
    }


    // =====================================================
    // DELETE
    // =====================================================

    fun delete(
        context: Context,
        index: Int
    ) {

        val profiles =
            loadAll(context)

        if (
            index < 0 ||
            index >= profiles.size
        ) {
            return
        }

        profiles.removeAt(index)

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val oldCount =
            prefs.getInt(
                KEY_COUNT,
                0
            )

        val oldActiveIndex =
            prefs.getInt(
                KEY_ACTIVE_INDEX,
                0
            )

        val editor =
            prefs.edit()

        for (
        oldIndex in 0 until oldCount
        ) {

            editor.remove(
                "vehicle_type_$oldIndex"
            )

            editor.remove(
                "model_$oldIndex"
            )

            editor.remove(
                "mileage_$oldIndex"
            )

            editor.remove(
                "ecu_type_$oldIndex"
            )

            editor.remove(
                "fuel_type_$oldIndex"
            )
        }

        profiles.forEachIndexed {
                newIndex,
                profile ->

            editor.putString(
                "vehicle_type_$newIndex",
                profile.vehicleType
            )

            editor.putString(
                "model_$newIndex",
                profile.model
            )

            editor.putString(
                "mileage_$newIndex",
                profile.mileage
            )

            editor.putString(
                "ecu_type_$newIndex",
                profile.ecuType
            )

            editor.putString(
                "fuel_type_$newIndex",
                profile.fuelType
            )
        }

        val newActiveIndex =
            when {

                profiles.isEmpty() ->
                    -1

                oldActiveIndex > index ->
                    oldActiveIndex - 1

                oldActiveIndex == index ->
                    minOf(
                        index,
                        profiles.size - 1
                    )

                else ->
                    oldActiveIndex
            }

        editor.putInt(
            KEY_COUNT,
            profiles.size
        )

        editor.putInt(
            KEY_ACTIVE_INDEX,
            newActiveIndex
        )

        editor.apply()
    }


    // =====================================================
    // CLEAR
    // =====================================================

    fun clear(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }


    // =====================================================
    // COMPLETE
    // =====================================================

    fun isComplete(
        context: Context
    ): Boolean {

        val profile =
            load(context)

        return profile.vehicleType.isNotBlank() &&
                profile.model.isNotBlank() &&
                profile.ecuType.isNotBlank() &&
                profile.fuelType.isNotBlank()
    }


    // =====================================================
    // HAS PROFILES
    // =====================================================

    fun hasProfiles(
        context: Context
    ): Boolean {

        return loadAll(
            context
        ).isNotEmpty()
    }
}