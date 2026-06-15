package com.watermelon.data.repository

import com.watermelon.domain.autoplay.RecommendationWeights
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test

class AutoplayRepositoryImplTest {
    @Test
    fun testWeights_haveDefaultValues() {
        val w = RecommendationWeights()
        assertThat(w.transitionFreq, `is`(30.0))
        assertEquals(20.0, w.likeSkipRatio, 0.001)
        assertEquals(15.0, w.skipPenalty, 0.001)
        assertEquals(0.5, w.recencyDecay, 0.001)
    }
}