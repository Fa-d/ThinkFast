package dev.sadakat.thinkfast.domain.model

/**
 * Sealed class representing different types of intervention content
 * that can be shown on reminder and timer overlay screens.
 *
 * These interventions are the CORE of ThinkFast's value proposition.
 */
sealed class InterventionContent {

    /**
     * Reflection questions that promote self-awareness and conscious decision-making.
     * Most effective content type (40% weight).
     */
    data class ReflectionQuestion(
        val question: String,
        val subtext: String = "Take a moment to honestly answer",
        val category: ReflectionCategory
    ) : InterventionContent()

    /**
     * Shows what the user could have done with their time instead.
     * Leverages loss aversion (30% weight).
     */
    data class TimeAlternative(
        val sessionMinutes: Int,
        val alternative: Alternative,
        val prefix: String = "This ${sessionMinutes} min could have been"
    ) : InterventionContent()

    /**
     * Interactive breathing exercise to create temporal friction.
     * Provides immediate mindfulness benefit (20% weight).
     */
    data class BreathingExercise(
        val duration: Int = 19,  // 4+7+8 seconds
        val instruction: String = "Let's take a moment to breathe together",
        val variant: BreathingVariant = BreathingVariant.FOUR_SEVEN_EIGHT
    ) : InterventionContent()

    /**
     * Shows usage statistics with gamification elements.
     * Good for progress tracking (10% weight).
     */
    data class UsageStats(
        val todayMinutes: Int,
        val yesterdayMinutes: Int,
        val weekAverage: Int,
        val goalMinutes: Int?,
        val message: String
    ) : InterventionContent()

    /**
     * Emotional appeals used selectively for high-impact moments.
     * Used sparingly to maintain effectiveness.
     */
    data class EmotionalAppeal(
        val message: String,
        val subtext: String,
        val context: EmotionalContext
    ) : InterventionContent()

    /**
     * Inspirational quotes (rare, quality over quantity).
     */
    data class Quote(
        val quote: String,
        val author: String
    ) : InterventionContent()

    /**
     * Gamification elements to encourage behavior change.
     */
    data class Gamification(
        val challenge: String,
        val reward: String,
        val currentProgress: Int,
        val target: Int
    ) : InterventionContent()
}

/**
 * Categories of reflection questions
 */
enum class ReflectionCategory {
    TRIGGER_AWARENESS,     // "What brought you here?"
    PRIORITY_CHECK,        // "Is this the most important thing?"
    EMOTIONAL_AWARENESS,   // "How are you feeling?"
    PATTERN_RECOGNITION,   // "What pattern keeps bringing you back?"
    LATE_NIGHT,           // Special late-night questions
    QUICK_REOPEN          // For rapid reopens
}

/**
 * Types of breathing exercises
 */
enum class BreathingVariant {
    FOUR_SEVEN_EIGHT,     // 4s inhale, 7s hold, 8s exhale
    BOX_BREATHING,        // 4s each phase
    CALM_BREATHING        // 5s inhale, 5s exhale
}

/**
 * Context for emotional appeals
 */
enum class EmotionalContext {
    LATE_NIGHT,           // 22:00-05:00
    WEEKEND_MORNING,      // Saturday/Sunday 06:00-11:00
    RAPID_REOPEN,         // Opened within 2 min
    EXTENDED_SESSION      // 15+ minutes
}

/**
 * Alternative activities users could do instead
 */
data class Alternative(
    val activity: String,
    val emoji: String,
    val estimatedMinutes: Int,
    val category: AlternativeCategory
)

/**
 * Categories of alternative activities
 */
enum class AlternativeCategory {
    PHYSICAL,       // Exercise, walk, stretch
    SOCIAL,         // Text someone, call, meet
    PRODUCTIVE,     // Read, work, clean
    MINDFUL,        // Meditate, breathe, journal
    CREATIVE        // Draw, write, play music
}

/**
 * Content pools containing all intervention messages
 */
object InterventionContentPools {

    // REFLECTION QUESTIONS (20+ unique questions)

    val triggerAwarenessQuestions = listOf(
        "What was happening before you felt the urge to open this?",
        "What are you trying to avoid or escape from?",
        "What pattern keeps bringing you back here?",
        "What triggered this impulse to scroll?"
    )

    val priorityCheckQuestions = listOf(
        "Is this the most important thing right now?",
        "What could you do instead that future-you would thank you for?",
        "Are you here for something specific, or just browsing?",
        "Would you do this if someone was watching?",
        "What's the best use of the next 10 minutes?"
    )

    val emotionalAwarenessQuestions = listOf(
        "How are you feeling right now? Bored? Stressed? Anxious?",
        "Will scrolling actually make you feel better?",
        "When was the last time scrolling made you genuinely happy?",
        "What emotion are you trying to satisfy?",
        "How will you feel after 20 minutes of scrolling?"
    )

    val patternRecognitionQuestions = listOf(
        "You've done this before. What happened last time?",
        "Is this becoming a habit you want to keep?",
        "What would break this cycle?",
        "How many times this week have you opened this?"
    )

    val lateNightQuestions = listOf(
        "Are you scrolling to avoid sleeping?",
        "Your future self needs rest more than you need scrolling.",
        "Will this be worth being tired tomorrow?",
        "What time did you want to sleep tonight?"
    )

    val quickReopenQuestions = listOf(
        "You just closed this. What changed?",
        "You've opened this %d times today. What are you looking for?",
        "Is this becoming compulsive?",
        "Take a breath. Do you really need to check again?"
    )

    // TIME ALTERNATIVES (organized by duration)

    val twoMinuteAlternatives = listOf(
        Alternative("Drink a glass of water and feel refreshed", "☕", 2, AlternativeCategory.PHYSICAL),
        Alternative("Do 20 push-ups", "💪", 2, AlternativeCategory.PHYSICAL),
        Alternative("Text someone you care about", "💬", 2, AlternativeCategory.SOCIAL),
        Alternative("Listen to your favorite song", "🎵", 2, AlternativeCategory.MINDFUL)
    )

    val fiveMinuteAlternatives = listOf(
        Alternative("A full meditation session", "🧘", 5, AlternativeCategory.MINDFUL),
        Alternative("Read 2-3 pages of a book", "📖", 5, AlternativeCategory.PRODUCTIVE),
        Alternative("Walk around the block", "🚶", 5, AlternativeCategory.PHYSICAL),
        Alternative("Sketch something creative", "🎨", 5, AlternativeCategory.CREATIVE),
        Alternative("Do a quick stretching routine", "🤸", 5, AlternativeCategory.PHYSICAL)
    )

    val tenMinuteAlternatives = listOf(
        Alternative("A 1-2km run", "🏃", 10, AlternativeCategory.PHYSICAL),
        Alternative("Read 5-10 pages", "📚", 10, AlternativeCategory.PRODUCTIVE),
        Alternative("A meaningful phone call", "☎️", 10, AlternativeCategory.SOCIAL),
        Alternative("Tidy up your space", "🧹", 10, AlternativeCategory.PRODUCTIVE),
        Alternative("Practice an instrument", "🎸", 10, AlternativeCategory.CREATIVE),
        Alternative("Cook a healthy snack", "🍳", 10, AlternativeCategory.PRODUCTIVE)
    )

    val twentyMinuteAlternatives = listOf(
        Alternative("A 3km run", "🏃", 20, AlternativeCategory.PHYSICAL),
        Alternative("A full chapter of a book", "📖", 20, AlternativeCategory.PRODUCTIVE),
        Alternative("Write in your journal", "✍️", 20, AlternativeCategory.MINDFUL),
        Alternative("Learn something new online", "💡", 20, AlternativeCategory.PRODUCTIVE),
        Alternative("Prepare a healthy meal", "🍲", 20, AlternativeCategory.PRODUCTIVE)
    )

    // BREATHING EXERCISE MESSAGES

    val breathingInstructions = listOf(
        "Let's take a moment to breathe together",
        "Your mind needs a break. Let's breathe.",
        "Before you continue, let's ground ourselves with breathing"
    )

    // USAGE STATS TEMPLATES

    fun getStatsMessage(todayMinutes: Int, yesterdayMinutes: Int, goalMinutes: Int?): String {
        val comparison = when {
            yesterdayMinutes == 0 -> "Your first session today"
            todayMinutes < yesterdayMinutes -> {
                val diff = yesterdayMinutes - todayMinutes
                "You're down $diff min from yesterday! 📉"
            }
            todayMinutes > yesterdayMinutes -> {
                val diff = todayMinutes - yesterdayMinutes
                "You're up $diff min from yesterday"
            }
            else -> "Same usage as yesterday"
        }

        val goalMessage = goalMinutes?.let { goal ->
            if (todayMinutes <= goal) {
                "\n\nStill under your $goal min goal! 🎯"
            } else {
                val over = todayMinutes - goal
                "\n\nYou're $over min over your goal"
            }
        } ?: ""

        return comparison + goalMessage
    }

    // EMOTIONAL APPEALS (use sparingly)

    val lateNightAppeals = listOf(
        InterventionContent.EmotionalAppeal(
            message = "It's late. Tomorrow-you will regret this.",
            subtext = "Sleep is more valuable than scrolling",
            context = EmotionalContext.LATE_NIGHT
        ),
        InterventionContent.EmotionalAppeal(
            message = "You know you'll regret staying up.",
            subtext = "Your body needs rest more than your mind needs scrolling",
            context = EmotionalContext.LATE_NIGHT
        )
    )

    val weekendMorningAppeals = listOf(
        InterventionContent.EmotionalAppeal(
            message = "Is this how you want to spend your Saturday morning?",
            subtext = "You only get so many weekends",
            context = EmotionalContext.WEEKEND_MORNING
        ),
        InterventionContent.EmotionalAppeal(
            message = "Weekend mornings are precious.",
            subtext = "Use this time for something that matters",
            context = EmotionalContext.WEEKEND_MORNING
        )
    )

    val rapidReopenAppeals = listOf(
        InterventionContent.EmotionalAppeal(
            message = "This is becoming compulsive.",
            subtext = "You're in control. You can stop.",
            context = EmotionalContext.RAPID_REOPEN
        )
    )

    val extendedSessionAppeals = listOf(
        InterventionContent.EmotionalAppeal(
            message = "You've been here for 15 minutes.",
            subtext = "What are you really looking for?",
            context = EmotionalContext.EXTENDED_SESSION
        ),
        InterventionContent.EmotionalAppeal(
            message = "This session is getting long.",
            subtext = "The longer you stay, the harder it is to leave",
            context = EmotionalContext.EXTENDED_SESSION
        )
    )

    // QUOTES (rare, quality over quantity)

    val inspirationalQuotes = listOf(
        InterventionContent.Quote(
            quote = "You will never find time for anything. If you want time, you must make it.",
            author = "Charles Buxton"
        ),
        InterventionContent.Quote(
            quote = "The cost of a thing is the amount of life which is required to be exchanged for it.",
            author = "Henry David Thoreau"
        ),
        InterventionContent.Quote(
            quote = "Time is what we want most, but what we use worst.",
            author = "William Penn"
        ),
        InterventionContent.Quote(
            quote = "The bad news is time flies. The good news is you're the pilot.",
            author = "Michael Altshuler"
        )
    )

    // GAMIFICATION CHALLENGES

    fun getGamificationChallenge(
        currentSessionMinutes: Int,
        bestSessionMinutes: Int,
        streakDays: Int
    ): InterventionContent.Gamification? {
        return when {
            currentSessionMinutes < 10 && bestSessionMinutes > 10 -> InterventionContent.Gamification(
                challenge = "Beat your record: Stop now at ${currentSessionMinutes} min!",
                reward = "New personal best 🏆",
                currentProgress = currentSessionMinutes,
                target = bestSessionMinutes
            )
            streakDays >= 7 -> InterventionContent.Gamification(
                challenge = "You're on a ${streakDays}-day streak!",
                reward = "Keep it going 🔥",
                currentProgress = streakDays,
                target = streakDays + 7
            )
            else -> null
        }
    }
}
