package com.example.yadra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class VehicleProfileActivity : AppCompatActivity() {

    private lateinit var profileContainer: LinearLayout
    private lateinit var btnCreateProfile: Button

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_vehicle_profile
        )

        profileContainer =
            findViewById(
                R.id.profileContainer
            )

        btnCreateProfile =
            findViewById(
                R.id.btnCreateProfile
            )

        btnCreateProfile.setOnClickListener {

            openCreateProfile()
        }

        loadProfiles()
    }

    // =====================================================
    // OPEN CREATE
    // =====================================================

    private fun openCreateProfile() {

        startActivity(
            Intent(
                this,
                VehicleProfileEditActivity::class.java
            )
        )
    }

    // =====================================================
    // LOAD PROFILES
    // =====================================================

    private fun loadProfiles() {

        profileContainer.removeAllViews()

        val profiles =
            VehicleProfileManager.loadAll(
                this
            )

        if (profiles.isEmpty()) {

            val emptyText =
                TextView(this)

            emptyText.text =
                "هیچ پروفایلی ساخته نشده است"

            emptyText.setTextColor(
                android.graphics.Color.parseColor(
                    "#8D99A6"
                )
            )

            emptyText.textSize = 14f

            emptyText.setPadding(
                8,
                30,
                8,
                30
            )

            profileContainer.addView(
                emptyText
            )

            return
        }

        val activeIndex =
            VehicleProfileManager.getActiveIndex(
                this
            )

        profiles.forEachIndexed {
                index,
                profile ->

            addProfileItem(
                index,
                profile,
                index == activeIndex
            )
        }
    }

    // =====================================================
    // ADD PROFILE ITEM
    // =====================================================

    private fun addProfileItem(
        index: Int,
        profile: VehicleProfile,
        isActive: Boolean
    ) {

        val item =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_vehicle_profile,
                    profileContainer,
                    false
                )

        val txtName =
            item.findViewById<TextView>(
                R.id.txtProfileName
            )

        val txtDetails =
            item.findViewById<TextView>(
                R.id.txtProfileDetails
            )

        val txtActive =
            item.findViewById<TextView>(
                R.id.txtProfileActive
            )

        val btnEdit =
            item.findViewById<Button>(
                R.id.btnEditProfile
            )

        val btnDelete =
            item.findViewById<Button>(
                R.id.btnDeleteProfile
            )

        val vehicleName =
            getVehicleName(
                profile
            )

        txtName.text =
            vehicleName

        txtDetails.text =
            buildProfileDetails(
                profile
            )

        if (isActive) {

            txtActive.visibility =
                View.VISIBLE

            txtActive.text =
                "● فعال"

        } else {

            txtActive.visibility =
                View.GONE
        }

        // =================================================
        // SELECT PROFILE
        // =================================================

        item.setOnClickListener {

            VehicleProfileManager.setActiveProfile(
                this,
                index
            )

            loadProfiles()
        }

        // =================================================
        // EDIT
        // =================================================

        btnEdit.setOnClickListener {

            val intent =
                Intent(
                    this,
                    VehicleProfileEditActivity::class.java
                )

            intent.putExtra(
                "profile_index",
                index
            )

            startActivity(
                intent
            )
        }

        // =================================================
        // DELETE
        // =================================================

        btnDelete.setOnClickListener {

            showDeleteDialog(
                index,
                vehicleName
            )
        }

        profileContainer.addView(
            item
        )
    }

    // =====================================================
    // PROFILE NAME
    // =====================================================

    private fun getVehicleName(
        profile: VehicleProfile
    ): String {

        val vehicleType =
            profile.vehicleType
                .trim()

        val model =
            profile.model
                .trim()

        return when {

            vehicleType.isNotBlank() &&
                    model.isNotBlank() ->
                "$vehicleType $model"

            vehicleType.isNotBlank() ->
                vehicleType

            model.isNotBlank() ->
                model

            else ->
                "پروفایل خودرو"
        }
    }

    // =====================================================
    // PROFILE DETAILS
    // =====================================================

    private fun buildProfileDetails(
        profile: VehicleProfile
    ): String {

        val ecu =
            profile.ecuType
                .trim()

        val fuel =
            profile.fuelType
                .trim()

        val mileage =
            profile.mileage
                .trim()

        return buildString {

            if (ecu.isNotBlank()) {

                append("ECU: ")
                append(ecu)
            }

            if (fuel.isNotBlank()) {

                if (isNotEmpty()) {
                    append("  •  ")
                }

                append("سوخت: ")
                append(fuel)
            }

            if (mileage.isNotBlank()) {

                if (isNotEmpty()) {
                    append("  •  ")
                }

                append("کارکرد: ")
                append(mileage)
            }
        }
    }

    // =====================================================
    // DELETE DIALOG
    // =====================================================

    private fun showDeleteDialog(
        index: Int,
        vehicleName: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("حذف پروفایل")
            .setMessage(
                "آیا پروفایل \"$vehicleName\" حذف شود؟"
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                VehicleProfileManager.delete(
                    this,
                    index
                )

                loadProfiles()
            }
            .setNegativeButton(
                "انصراف",
                null
            )
            .show()
    }

    // =====================================================
    // RESUME
    // =====================================================

    override fun onResume() {

        super.onResume()

        if (::profileContainer.isInitialized) {

            loadProfiles()
        }
    }
}