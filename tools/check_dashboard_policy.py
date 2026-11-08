"""Source regression checks; no Gradle, device, network, or license mutation."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java"
EXPECTED = {
    "PROFILE": "VehicleProfileActivity", "BUY": "BuyActivity",
    "CONNECTION": "ConnectionActivity", "WARNING": "WarningsActivity",
    "SENSORS": "SensorsActivity", "ERRORS": "ErrorsActivity", "ECU": "EcuTestActivity",
    "UNKNOWN_ECU": "UnknownEcuScannerActivity", "KWP_TEST": "KwpTestActivity",
    "ECU_CODE_TEST": "ActuatorCodeTestActivity", "ACTUATORS": "ActuatorTestActivity",
    "AI": "AIActivity", "SETTINGS": "SettingsActivity", "TRIP": "TripActivity", "CHARTS": "ChartsActivity",
}

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class DashboardPolicyRegression(unittest.TestCase):
    def test_all_actual_callbacks_and_ids(self):
        main = read("com/example/yadra/MainActivity.kt")
        blocks = main.split("RotatingDashboardView.DashboardItem(")[1:]
        self.assertEqual(15, len(blocks))
        seen = set()
        for block in blocks:
            item = re.search(r"DashboardItemId\.(\w+)", block)[1]
            seen.add(item)
            self.assertEqual([EXPECTED[item]], re.findall(r"(\w+Activity)::class.java", block.split("// UPDATE DASHBOARD RPM")[0]))
        self.assertEqual(set(EXPECTED), seen)

    def test_both_touch_paths_use_license_and_connection_dispatch(self):
        view = read("com/example/yadra/RotatingDashboardView.kt")
        self.assertEqual(2, view.count("performItemClick(item)"))  # both touch paths
        dispatch = view.split("internal fun performItemClick")[1].split("private fun hasLicenseAccess")[0]
        self.assertIn("!hasLicenseAccess(item) -> onLockedItemClick", dispatch)
        self.assertIn("!isConnectionAllowed(item) -> onConnectionBlockedItemClick", dispatch)
        self.assertIn("else -> item.onClick()", dispatch)
        self.assertNotIn("item.title ==", view)
        self.assertIn("item.id, com.example.yadra.license.LicenseManager.isActive(context)", view)

    def test_immediate_refresh_and_no_destination_purchase_gates(self):
        manager = read("license/LicenseManager.kt")
        self.assertIn("notifyVerificationChanged()", manager.split(".apply()")[1])
        main = read("com/example/yadra/MainActivity.kt")
        self.assertIn("addVerificationListener(dashboardLicenseListener)", main)
        self.assertIn("removeVerificationListener(dashboardLicenseListener)", main)
        listener = main.split("private val dashboardLicenseListener")[1].split("private fun setupRotatingDashboard")[0]
        self.assertIn("refreshLicenseState()", listener)
        self.assertIn("applyDashboardMode()", listener)
        for destination in EXPECTED.values():
            if destination == "BuyActivity":
                continue
            source = read("com/example/yadra/" + destination + ".kt")
            self.assertNotRegex(source, r"BuyActivity|LicenseManager|isPremium|requirePremium")

if __name__ == "__main__":
    unittest.main()
