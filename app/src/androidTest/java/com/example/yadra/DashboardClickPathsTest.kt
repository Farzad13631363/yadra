package com.example.yadra

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.yadra.license.DashboardItemId
import org.junit.Assert.*
import org.junit.Test

/** Executes the production callbacks without opening screens or touching ECU hardware or licenses. */
class DashboardClickPathsTest {
    private class RecordingMainActivity : MainActivity() {
        init { attachBaseContext(InstrumentationRegistry.getInstrumentation().targetContext) }
        var destination: String? = null
        override fun startActivity(intent: Intent) { destination = intent.component?.className }
    }

    @Test fun everyProductionItemOpensItsIntendedScreen() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = RecordingMainActivity()
            val expected = mapOf(
                DashboardItemId.PROFILE to VehicleProfileActivity::class.java,
                DashboardItemId.BUY to BuyActivity::class.java,
                DashboardItemId.CONNECTION to ConnectionActivity::class.java,
                DashboardItemId.WARNING to WarningsActivity::class.java,
                DashboardItemId.SENSORS to SensorsActivity::class.java,
                DashboardItemId.ERRORS to ErrorsActivity::class.java,
                DashboardItemId.ECU to EcuTestActivity::class.java,
                DashboardItemId.UNKNOWN_ECU to UnknownEcuScannerActivity::class.java,
                DashboardItemId.KWP_TEST to KwpTestActivity::class.java,
                DashboardItemId.ECU_CODE_TEST to ActuatorCodeTestActivity::class.java,
                DashboardItemId.ACTUATORS to ActuatorTestActivity::class.java,
                DashboardItemId.AI to AIActivity::class.java,
                DashboardItemId.SETTINGS to SettingsActivity::class.java,
                DashboardItemId.TRIP to TripActivity::class.java,
                DashboardItemId.CHARTS to ChartsActivity::class.java)
            val items = activity.dashboardItems()
            assertEquals(15, items.size)
            assertEquals(expected.keys, items.map { it.id }.toSet())
            items.forEach { item ->
                activity.destination = null
                item.onClick()
                assertEquals(item.id.name, expected.getValue(item.id).name, activity.destination)
            }
        }
    }
}
