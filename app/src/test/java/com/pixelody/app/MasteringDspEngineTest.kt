package com.pixelody.app

import com.pixelody.app.core.playback.MasteringDspEngine
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.MasteringPreset
import com.pixelody.app.data.model.MasteringProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MasteringDspEngineTest {

    @Test
    fun testFlatProfileYieldsZeroDbResponse() {
        val profile = MasteringProfile(preset = MasteringPreset.AudiophileReference)
        for (freq in listOf(20f, 60f, 230f, 910f, 1000f, 3600f, 14000f, 20000f)) {
            val db = MasteringDspEngine.evaluateParametricResponseDb(freq, profile)
            assertEquals(0.0f, db, 0.001f)
        }
    }

    @Test
    fun testParametricPeakingFilterPeakGain() {
        // Boost 910Hz by +6dB with Q=1.4
        val profile = MasteringProfile(
            eqGainsDb = listOf(0f, 0f, 6.0f, 0f, 0f),
            eqQFactors = listOf(1.4f, 1.4f, 1.4f, 1.4f, 1.4f)
        )
        val peakDb = MasteringDspEngine.evaluateParametricResponseDb(910f, profile)
        assertEquals(6.0f, peakDb, 0.05f)

        // Half octave away should be significantly lower than peak
        val rollOffDb = MasteringDspEngine.evaluateParametricResponseDb(1820f, profile)
        assertTrue("Gain at 2f0 ($rollOffDb dB) should be lower than peak", rollOffDb < 3.5f)
        assertTrue("Gain at 2f0 ($rollOffDb dB) should be positive", rollOffDb > 0.0f)
    }

    @Test
    fun testSubBassBoostAddsLowShelfEnergy() {
        val profile = MasteringProfile(
            subBassBoostDb = 4.0f
        )
        val subGain = MasteringDspEngine.evaluateParametricResponseDb(30f, profile)
        val midGain = MasteringDspEngine.evaluateParametricResponseDb(1000f, profile)
        assertTrue("Sub-bass gain at 30Hz should be elevated: $subGain dB", subGain > 3.0f)
        assertEquals(0.0f, midGain, 0.05f)
    }

    @Test
    fun testTubeSaturationZeroDriveIsLinear() {
        val sample = 0.5f
        val output = MasteringDspEngine.applyTubeSaturation(sample, drive = 0.0f)
        assertEquals(sample, output, 0.001f)
    }

    @Test
    fun testTubeSaturationNonLinearHarmonicWarmth() {
        val peakSample = 1.6f
        val peakDrive0 = MasteringDspEngine.applyTubeSaturation(peakSample, drive = 0.0f)
        val peakDrive50 = MasteringDspEngine.applyTubeSaturation(peakSample, drive = 0.5f)
        val peakDrive100 = MasteringDspEngine.applyTubeSaturation(peakSample, drive = 1.0f)

        assertTrue("Output with drive should compress dynamic peaks", peakDrive50 < peakDrive0)
        assertTrue("Higher drive should compress peaks further", peakDrive100 < peakDrive50)
        assertTrue("Output should remain in bounded range", peakDrive100 in -1.5f..1.5f)

        // Mid-level harmonic excitation
        val midSample = 0.4f
        val midDrive50 = MasteringDspEngine.applyTubeSaturation(midSample, drive = 0.5f)
        assertTrue("Drive should add harmonic presence to mid levels", midDrive50 > midSample)
    }

    @Test
    fun testStereoHorizonMonoMatrixing() {
        val left = 0.8f
        val right = 0.2f
        val (outL, outR) = MasteringDspEngine.processStereoHorizon(left, right, spatialWidth = 0.0f)

        // At width=0.0 (pure mono), L and R should be identical
        assertEquals(outL, outR, 0.001f)
        assertTrue("Mono sum should be non-zero", outL > 0.4f)
    }

    @Test
    fun testStereoHorizonNormalStereoPassthrough() {
        val left = 0.7f
        val right = -0.3f
        val (outL, outR) = MasteringDspEngine.processStereoHorizon(left, right, spatialWidth = 1.0f)

        assertEquals(left, outL, 0.001f)
        assertEquals(right, outR, 0.001f)
    }

    @Test
    fun testStereoHorizonBinauralExpansion() {
        val left = 0.6f
        val right = 0.1f
        val (outL, outR) = MasteringDspEngine.processStereoHorizon(left, right, spatialWidth = 1.8f)

        // Expanded width increases separation
        assertTrue("Expanded left channel should differ from right", abs(outL - outR) > abs(left - right) * 0.5f)
    }

    @Test
    fun testStereoCorrelationCalculation() {
        val monoL = floatArrayOf(0.5f, -0.5f, 0.8f, -0.2f)
        val monoR = floatArrayOf(0.5f, -0.5f, 0.8f, -0.2f)
        val corrMono = MasteringDspEngine.computeStereoCorrelation(monoL, monoR)
        assertEquals(1.0f, corrMono, 0.001f)

        val outOfPhaseR = floatArrayOf(-0.5f, 0.5f, -0.8f, 0.2f)
        val corrPhaseInverted = MasteringDspEngine.computeStereoCorrelation(monoL, outOfPhaseR)
        assertEquals(-1.0f, corrPhaseInverted, 0.001f)
    }

    @Test
    fun testStudioPeakLimiterSoftKneeCompression() {
        // Below threshold: -0.5 dB corresponds to linear ~0.944
        val sampleUnder = 0.5f
        val sampleOver = 1.4f

        val limitedUnder = MasteringDspEngine.applyStudioLimiter(sampleUnder, thresholdDb = -0.5f)
        val limitedOver = MasteringDspEngine.applyStudioLimiter(sampleOver, thresholdDb = -0.5f)

        assertEquals(sampleUnder, limitedUnder, 0.001f)
        assertTrue("Over threshold signal should be compressed", limitedOver < sampleOver)
        assertTrue("Limited signal should remain bounded below 1.2", limitedOver < 1.2f)
    }

    @Test
    fun testDbToNeedleAngleMapping() {
        assertEquals(-45.0f, MasteringDspEngine.dbToNeedleAngle(-20f), 0.01f)
        assertEquals(0.0f, MasteringDspEngine.dbToNeedleAngle(0f), 0.01f)
        assertEquals(30.0f, MasteringDspEngine.dbToNeedleAngle(3f), 0.01f)
    }

    @Test
    fun testMasteringProfileNormalization() {
        val profile = MasteringProfile(
            eqGainsDb = listOf(-25f, 30f, 0f, 0f, 0f),
            eqQFactors = listOf(0.1f, 10f, 1.4f, 1.4f, 1.4f),
            tubeDrive = 2.5f,
            spatialWidth = 4.0f
        ).normalized()

        assertEquals(-12.0f, profile.eqGainsDb[0], 0.01f)
        assertEquals(12.0f, profile.eqGainsDb[1], 0.01f)
        assertEquals(MasteringProfile.MIN_Q, profile.eqQFactors[0], 0.01f)
        assertEquals(MasteringProfile.MAX_Q, profile.eqQFactors[1], 0.01f)
        assertEquals(1.0f, profile.tubeDrive, 0.01f)
        assertEquals(MasteringProfile.MAX_SPATIAL_WIDTH, profile.spatialWidth, 0.01f)
    }

    @Test
    fun testTapeWarmthHighFrequencyHysteresis() {
        val sample = 0.8f
        val prevSample = 0.2f
        val warmthOutput = MasteringDspEngine.applyTapeWarmth(sample, warmth = 0.5f, previousSample = prevSample)

        assertTrue("Warmth output should be bounded", warmthOutput in -1.2f..1.2f)
        assertTrue("Warmth filter should smooth sample towards previous value", warmthOutput < sample)
    }

    @Test
    fun testZeroEnergyStereoCorrelationReturnsOne() {
        val zerosL = FloatArray(10) { 0f }
        val zerosR = FloatArray(10) { 0f }
        val correlation = MasteringDspEngine.computeStereoCorrelation(zerosL, zerosR)
        assertEquals(1.0f, correlation, 0.001f)
    }

    @Test
    fun testLimiterExtremeSignalImmunity() {
        val extremePositive = 100.0f
        val extremeNegative = -100.0f

        val limitedPos = MasteringDspEngine.applyStudioLimiter(extremePositive, thresholdDb = -1.0f)
        val limitedNeg = MasteringDspEngine.applyStudioLimiter(extremeNegative, thresholdDb = -1.0f)

        assertTrue("Positive extreme must remain finite and bounded", limitedPos < 2.0f && limitedPos > 0f)
        assertTrue("Negative extreme must remain finite and bounded", limitedNeg > -2.0f && limitedNeg < 0f)
    }
}

