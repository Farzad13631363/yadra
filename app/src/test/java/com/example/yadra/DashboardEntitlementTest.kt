package com.example.yadra

import com.example.yadra.license.*
import org.junit.Assert.*
import org.junit.Test

class DashboardEntitlementTest {
    @Test fun allFifteenIdsBelongToVerifiedDiagAndFreeIdsRemainFree() {
        val ids = DashboardItemId.values()
        assertEquals(15, ids.size)
        val free = setOf(DashboardItemId.PROFILE, DashboardItemId.BUY,
            DashboardItemId.CONNECTION, DashboardItemId.WARNING, DashboardItemId.SETTINGS)
        ids.forEach {
            assertTrue(it.name, DiagEntitlement.canAccess(it, true))
            assertEquals(it.name, it in free, DiagEntitlement.canAccess(it, false))
        }
    }
}
