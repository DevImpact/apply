package com.crowdfunding.ui.projects

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
    val stats: ProjectStats = ProjectStats()
)

// Represents the statistics for a project
data class ProjectStats(
    val investors: Int = 0,
    val donors: Int = 0,
    val advertisers: Int = 0
)

// Represents a user's intention in the new lookup tables
data class IntentionRecord(
    val type: String = "",
    val timestamp: Long = 0
)
