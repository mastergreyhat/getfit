package com.getfit.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarManagerTest {

    @Test
    fun nextLockedAvatar_andDaysRemaining_trackStreakThresholds() {
        val starter = AvatarManager.AVATARS.first()
        val threeMonth = AvatarManager.AVATARS[2]

        assertTrue(AvatarManager.isUnlocked(starter, 0))
        assertFalse(AvatarManager.isUnlocked(threeMonth, 47))
        assertEquals(43, AvatarManager.daysRemaining(threeMonth, 47))
        assertEquals("avatar_3_months", AvatarManager.nextLockedAvatar(47)?.id)
        assertNull(AvatarManager.nextLockedAvatar(365))
    }
}
