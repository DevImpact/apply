package com.crowdfunding.ui.projects

import com.google.firebase.database.PropertyName

// Represents the main project data object
data class Project(
    val id: String = "",
    // New schema fields
    val title: String = "",
    val description: String = "",
    val ownerUid: String = "",
    val active: Boolean = false,
    val coverImage: String = "",
    // Legacy fields for compatibility
    val name: String = "",
    val shortDescription: String = "",
    val detailedDescription: String = "",
    val goal: String? = null,
    val status: String? = null,
    // Common fields
    val pdfLinks: Map<String, String> = emptyMap(),
    val stats: ProjectStats = ProjectStats(),
    // Legacy intentions map
    @get:PropertyName("النوايا")
    val legacyIntentions: Map<String, Map<String, Boolean>>? = null
)

// Represents the statistics for a project
data class ProjectStats(
    val investors: Int = 0,
    val donors: Int = 0,
    val advertisers: Int = 0,
    // Legacy arabic fields for compatibility
    @get:PropertyName("مستثمرون")
    val legacyInvestors: Int = 0,
    @get:PropertyName("مانحون")
    val legacyDonors: Int = 0,
    @get:PropertyName("معلنون")
    val legacyAdvertisers: Int = 0
) {
    fun getInvestorsCount(): Int = investors + legacyInvestors
    fun getDonorsCount(): Int = donors + legacyDonors
    fun getAdvertisersCount(): Int = advertisers + legacyAdvertisers
}

// Represents a user's intention in the new lookup tables
data class IntentionRecord(
    val type: String = "",
    val timestamp: Long = 0
)
