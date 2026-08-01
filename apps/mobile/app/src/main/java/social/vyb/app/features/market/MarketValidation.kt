package social.vyb.app.features.market

internal fun MarketPostDraft.validated(): MarketPostDraft {
    require(tab in setOf("sale", "buying", "lend")) {
        "Choose a valid market post type."
    }
    val cleanTitle = title.trim()
    val cleanCategory = category.trim()
    val cleanDescription = description.trim()
    val cleanCampusSpot = campusSpot.trim()
    val cleanCondition = condition.trim()

    require(cleanTitle.isNotEmpty()) { "Add a title for your market post." }
    require(cleanTitle.length <= 120) { "Keep the title under 120 characters." }
    require(cleanCategory.isNotEmpty()) { "Choose a category." }
    require(cleanCategory.length <= 60) { "Keep the category under 60 characters." }
    require(cleanDescription.isNotEmpty()) { "Add a short description." }
    require(cleanDescription.length <= 2_000) {
        "Keep the description under 2,000 characters."
    }
    require(cleanCampusSpot.length <= 120) { "Keep the campus spot under 120 characters." }
    require(cleanCondition.length <= 80) { "Keep the condition under 80 characters." }
    require(amount == null || amount in 0..999_999_999L) {
        "Enter a valid amount."
    }
    require(tab != "sale" || (amount ?: 0L) > 0L) {
        "Add a valid price for the listing."
    }

    return copy(
        title = cleanTitle,
        category = cleanCategory,
        description = cleanDescription,
        campusSpot = cleanCampusSpot,
        condition = cleanCondition
    )
}
