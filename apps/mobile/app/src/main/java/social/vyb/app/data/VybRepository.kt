package social.vyb.app.data

class VybRepository {
    val stories = listOf(
        Story("Your story", 0xFFBAFF43, true),
        Story("Aarav", 0xFF7DE2FF),
        Story("Meera", 0xFFFF875F),
        Story("Kabir", 0xFFB69CFF),
        Story("Diya", 0xFFFFD166)
    )

    val posts = listOf(
        FeedPost(
            "preview-1",
            "Meera Sharma", "@meerash", "8m",
            "Golden hour, chai, and the quiet corner behind the library. Campus has its moments ✨",
            128, 14, "Campus"
        ),
        FeedPost(
            "preview-2",
            "Coding Club", "@codeoncampus", "32m",
            "Hack night registrations are open. Bring an idea, find a team, build till sunrise.",
            84, 22, "Community"
        ),
        FeedPost(
            "preview-3",
            "Aarav Singh", "@aarav", "1h",
            "Anyone up for a badminton match after the last lecture?",
            41, 9, "Sports"
        )
    )

    val chats = listOf(
        Chat("Meera", "That reel was too real 😂", "2m", 2),
        Chat("Design Society", "Riya: updated the poster", "18m", 5),
        Chat("Aarav", "Meet at the court?", "1h"),
        Chat("Hack Night Team", "Kabir sent a photo", "3h", 1)
    )

    val listings = listOf(
        Listing("Scientific calculator", "₹650", "Rohan · 2nd year", "Like new"),
        Listing("Engineering drawing kit", "₹300", "Ananya · 1st year", "Stationery"),
        Listing("Atomic Habits", "₹220", "Kunal · MBA", "Books"),
        Listing("Cycle with lock", "₹3,200", "Sara · Final year", "Pickup")
    )
}
