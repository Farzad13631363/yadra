package com.example.yadra

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VehicleProfileEditActivity : AppCompatActivity() {

    private lateinit var spinnerVehicleType: Spinner
    private lateinit var editModel: EditText
    private lateinit var editMileage: EditText
    private lateinit var spinnerEcuType: Spinner
    private lateinit var spinnerFuelType: Spinner
    private lateinit var btnSaveVehicleProfile: Button

    private var editingIndex = -1

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_vehicle_profile_edit
        )

        spinnerVehicleType =
            findViewById(
                R.id.spinnerVehicleType
            )

        editModel =
            findViewById(
                R.id.editModel
            )

        editMileage =
            findViewById(
                R.id.editMileage
            )

        spinnerEcuType =
            findViewById(
                R.id.spinnerEcuType
            )

        spinnerFuelType =
            findViewById(
                R.id.spinnerFuelType
            )

        btnSaveVehicleProfile =
            findViewById(
                R.id.btnSaveVehicleProfile
            )

        editingIndex =
            intent.getIntExtra(
                "profile_index",
                -1
            )

        setupSpinners()

        if (editingIndex >= 0) {

            loadProfileForEdit()

        } else {

            btnSaveVehicleProfile.text =
                "ایجاد پروفایل"
        }

        btnSaveVehicleProfile.setOnClickListener {

            saveProfile()
        }
    }

    // =====================================================
    // SPINNERS
    // =====================================================

    private fun setupSpinners() {

        val vehicleTypes =
            listOf(
                "Peugeot",
                "Iran Khodro",
                "Saipa",
                "Renault",
                "Toyota",
                "Hyundai",
                "Kia",
                "Other"
            )

        val ecuTypes =
            listOf(
                "Bosch",
                "Valeo",
                "Siemens",
                "Continental",
                "Other"
            )

        val fuelTypes =
            listOf(
                "Gasoline",
                "Diesel",
                "Hybrid",
                "Other"
            )

        spinnerVehicleType.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                vehicleTypes
            )

        spinnerEcuType.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ecuTypes
            )

        spinnerFuelType.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                fuelTypes
            )
    }

    // =====================================================
    // LOAD PROFILE
    // =====================================================

    private fun loadProfileForEdit() {

        val profiles =
            VehicleProfileManager.loadAll(
                this
            )

        val profile =
            profiles.getOrNull(
                editingIndex
            )

        if (profile == null) {

            finish()

            return
        }

        editModel.setText(
            profile.model
        )

        editMileage.setText(
            profile.mileage
        )

        setSpinnerValue(
            spinnerVehicleType,
            profile.vehicleType
        )

        setSpinnerValue(
            spinnerEcuType,
            profile.ecuType
        )

        setSpinnerValue(
            spinnerFuelType,
            profile.fuelType
        )

        btnSaveVehicleProfile.text =
            "ذخیره تغییرات"
    }

    // =====================================================
    // SAVE
    // =====================================================

    private fun saveProfile() {

        val vehicleType =
            spinnerVehicleType
                .selectedItem
                ?.toString()
                ?.trim()
                ?: ""

        val model =
            editModel
                .text
                .toString()
                .trim()

        val mileage =
            editMileage
                .text
                .toString()
                .trim()

        val ecuType =
            spinnerEcuType
                .selectedItem
                ?.toString()
                ?.trim()
                ?: ""

        val fuelType =
            spinnerFuelType
                .selectedItem
                ?.toString()
                ?.trim()
                ?: ""

        if (model.isBlank()) {

            editModel.error =
                "مدل خودرو را وارد کنید"

            editModel.requestFocus()

            return
        }

        val profile =
            VehicleProfile(
                vehicleType = vehicleType,
                model = model,
                mileage = mileage,
                ecuType = ecuType,
                fuelType = fuelType
            )

        if (editingIndex >= 0) {

            updateExistingProfile(
                profile
            )

        } else {

            VehicleProfileManager.save(
                this,
                profile
            )
        }

        Toast.makeText(
            this,
            if (editingIndex >= 0)
                "پروفایل ویرایش شد"
            else
                "پروفایل ایجاد شد",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    // =====================================================
    // UPDATE EXISTING
    // =====================================================

    private fun updateExistingProfile(
        profile: VehicleProfile
    ) {

        val profiles =
            VehicleProfileManager.loadAll(
                this
            )

        if (
            editingIndex < 0 ||
            editingIndex >= profiles.size
        ) {
            return
        }

        val activeIndex =
            VehicleProfileManager.getActiveIndex(
                this
            )

        profiles[editingIndex] =
            profile

        VehicleProfileManager.clear(
            this
        )

        profiles.forEach { savedProfile ->

            VehicleProfileManager.save(
                this,
                savedProfile
            )
        }

        if (
            activeIndex >= 0 &&
            activeIndex < profiles.size
        ) {

            VehicleProfileManager.setActiveProfile(
                this,
                activeIndex
            )
        }
    }

    // =====================================================
    // SPINNER VALUE
    // =====================================================

    private fun setSpinnerValue(
        spinner: Spinner,
        value: String
    ) {

        for (
        index in 0 until spinner.count
        ) {

            if (
                spinner
                    .getItemAtPosition(index)
                    .toString()
                    .equals(
                        value,
                        ignoreCase = true
                    )
            ) {

                spinner.setSelection(
                    index
                )

                return
            }
        }
    }
}