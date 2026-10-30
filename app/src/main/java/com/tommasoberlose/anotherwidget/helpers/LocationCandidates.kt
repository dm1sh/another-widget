package com.tommasoberlose.anotherwidget.helpers

data class LocationSample(
    val provider: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val ageMs: Long
)

object LocationCandidates {
    fun selectBest(
        samples: Iterable<LocationSample>,
        maxAgeMs: Long,
        maxAccuracyMeters: Float
    ): LocationSample? {
        return samples
            .asSequence()
            .filter { it.latitude.isFinite() && it.longitude.isFinite() }
            .filter { !(it.latitude == 0.0 && it.longitude == 0.0) }
            .filter { it.ageMs in 0..maxAgeMs }
            .filter { it.accuracyMeters == null || (it.accuracyMeters > 0f && it.accuracyMeters <= maxAccuracyMeters) }
            .minByOrNull { sample ->
                val acc = sample.accuracyMeters ?: 500f
                val ageMinutes = sample.ageMs / 60_000f
                // Balance spatial accuracy with temporal freshness (25m penalty per minute of age)
                acc + ageMinutes * 25f
            }
    }
}
