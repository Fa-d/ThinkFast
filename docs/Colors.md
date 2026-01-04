# Color System

The ThinkFast color system uses semantic colors to communicate meaning consistently throughout the app, following Material 3 principles with iOS-inspired polish.

## Design Philosophy

**Color Purpose**:
- **Blue**: Primary actions, neutral information, trust
- **Green**: Success, goals met, positive trends (inverse for screen time!)
- **Orange**: Caution, active streaks, attention needed
- **Red**: Limits exceeded, destructive actions, urgency
- **Purple**: Secondary accent, variety, milestone achievements

**Semantic Usage**:
Colors convey meaning beyond aesthetics. A trend going "down" (reduced screen time) uses green because that's positive for wellbeing. Limits "exceeded" use red for urgency.

---

## AppColors Structure

The unified `AppColors` object consolidates all color definitions with clear hierarchy:

### Primary Palette (Blue - Brand)

```kotlin
AppColors.Primary.Default        // #2196F3 - Blue 500
AppColors.Primary.Dark           // #1565C0 - Blue 800
AppColors.Primary.Light          // #64B5F6 - Blue 300
AppColors.Primary.Container      // #E3F2FD - Blue 50
AppColors.Primary.OnPrimary      // #FFFFFF - White
AppColors.Primary.OnContainer    // #0D47A1 - Blue 900
```

**Usage**:
- Primary actions (buttons, links)
- Selected states
- Focus indicators
- Brand elements

**Example**:
```kotlin
PrimaryButton(
    text = "Save Goal",
    onClick = { /* action */ }
    // Uses Primary gradient internally
)
```

---

### Secondary Palette (Purple - Accent)

```kotlin
AppColors.Secondary.Default      // #9C27B0 - Purple 500
AppColors.Secondary.Dark         // #7B1FA2 - Purple 800
AppColors.Secondary.Light        // #BA68C8 - Purple 300
AppColors.Secondary.Container    // #F3E5F5 - Purple 50
AppColors.Secondary.OnSecondary  // #FFFFFF - White
AppColors.Secondary.OnContainer  // #4A148C - Purple 900
```

**Usage**:
- Secondary actions
- Accent colors for variety
- Milestone achievements (30+ day streaks)
- Decorative elements

---

### Semantic Colors

#### Success (Green)

```kotlin
AppColors.Semantic.Success.Default      // #4CAF50 - Green 500
AppColors.Semantic.Success.Light        // #81C784 - Green 300
AppColors.Semantic.Success.Container    // #E8F5E9 - Green 50
AppColors.Semantic.Success.OnSuccess    // #FFFFFF - White
AppColors.Semantic.Success.OnContainer  // #1B5E20 - Green 900
```

**Usage**:
- Goals met (under daily limit)
- Positive trends (reduced usage)
- Achievements unlocked
- Success confirmations
- Streak milestones

**Example**:
```kotlin
// Goal under limit
Text(
    text = "Great job! 25% under your limit",
    color = AppColors.Semantic.Success.Default
)
```

#### Warning (Orange)

```kotlin
AppColors.Semantic.Warning.Default      // #FF9800 - Orange 500
AppColors.Semantic.Warning.Light        // #FFB74D - Orange 300
AppColors.Semantic.Warning.Container    // #FFF3E0 - Orange 50
AppColors.Semantic.Warning.OnWarning    // #FFFFFF - White
AppColors.Semantic.Warning.OnContainer  // #E65100 - Orange 900
```

**Usage**:
- Approaching limits (75-100% of goal)
- Active streaks (ongoing)
- Caution states (not errors)
- Attention-needed situations

**Example**:
```kotlin
// Approaching limit
LinearProgressIndicator(
    progress = 0.85f,
    color = AppColors.Semantic.Warning.Default
)
```

#### Error (Red)

```kotlin
AppColors.Semantic.Error.Default      // #F44336 - Red 500
AppColors.Semantic.Error.Light        // #E57373 - Red 300
AppColors.Semantic.Error.Container    // #FFEBEE - Red 50
AppColors.Semantic.Error.OnError      // #FFFFFF - White
AppColors.Semantic.Error.OnContainer  // #B71C1C - Red 900
```

**Usage**:
- Limits exceeded (over daily goal)
- Destructive actions (delete, remove)
- Error states
- Failed operations

**Example**:
```kotlin
// Destructive action
SecondaryButton(
    text = "Delete Goal",
    onClick = { /* delete */ },
    isDestructive = true  // Uses error color
)
```

#### Info (Blue)

```kotlin
AppColors.Semantic.Info.Default      // #2196F3 - Blue 500
AppColors.Semantic.Info.Light        // #64B5F6 - Blue 300
AppColors.Semantic.Info.Container    // #E3F2FD - Blue 50
AppColors.Semantic.Info.OnInfo       // #FFFFFF - White
AppColors.Semantic.Info.OnContainer  // #0D47A1 - Blue 900
```

**Usage**:
- Neutral information
- Informational messages
- Default states
- Generic highlights

---

### Progress States

Helper object for progress-based color selection:

```kotlin
AppColors.Progress.OnTrack      // Green - 0-75%
AppColors.Progress.Approaching  // Orange - 75-100%
AppColors.Progress.OverLimit    // Red - >100%
AppColors.Progress.Neutral      // Blue - No status

// Helper function
AppColors.Progress.getColorForPercentage(percentage: Int): Color
```

**Usage**:
```kotlin
val usagePercentage = 85  // 85% of daily limit
val progressColor = AppColors.Progress.getColorForPercentage(usagePercentage)
// Returns Orange (Approaching)

CircularProgressIndicator(
    progress = 0.85f,
    color = progressColor
)
```

**Logic**:
- **0-75%**: Green (OnTrack) - Comfortably under limit
- **75-100%**: Orange (Approaching) - Getting close to limit
- **>100%**: Red (OverLimit) - Exceeded daily goal

---

### Streak Colors

Special colors for streak milestone visualization:

```kotlin
AppColors.Streak.Day1to6    // #FF9800 - Orange
AppColors.Streak.Day7to13   // #FF5722 - Deep Orange
AppColors.Streak.Day14to29  // #F44336 - Red
AppColors.Streak.Day30Plus  // #9C27B0 - Purple (milestone!)

// Helper function
AppColors.Streak.getColorForStreak(days: Int): Color
```

**Usage**:
```kotlin
val streakDays = 35
val streakColor = AppColors.Streak.getColorForStreak(streakDays)
// Returns Purple (30+ days - major milestone!)

Text(
    text = "$streakDays day streak!",
    color = streakColor
)
```

**Progression**:
- **1-6 days**: Orange - Starting out
- **7-13 days**: Deep Orange - Building momentum
- **14-29 days**: Red - Getting serious
- **30+ days**: Purple - Major achievement!

---

### Gradients

Pre-defined gradients for visual polish:

```kotlin
// Primary gradient (Blue → Purple)
AppColors.Gradients.primary()
// Returns: listOf(Blue 500 @ 90%, Purple 500 @ 90%)

// Success gradient (Light Green → Green)
AppColors.Gradients.success()

// Warning gradient (Light Orange → Orange)
AppColors.Gradients.warning()

// Error gradient (Light Red → Red)
AppColors.Gradients.error()
```

**Usage**:
```kotlin
Box(
    modifier = Modifier.background(
        brush = Brush.linearGradient(
            colors = AppColors.Gradients.primary()
        )
    )
) {
    // Content with gradient background
}
```

**Where Used**:
- PrimaryButton background
- Hero sections
- Celebration dialogs
- Feature highlights

---

## Material 3 Color Scheme

ThinkFast uses Material 3's semantic color scheme:

```kotlin
MaterialTheme.colorScheme.primary          // Primary brand color
MaterialTheme.colorScheme.onPrimary        // Text on primary
MaterialTheme.colorScheme.primaryContainer // Tinted backgrounds
MaterialTheme.colorScheme.onPrimaryContainer // Text on container

MaterialTheme.colorScheme.surface          // Surface backgrounds
MaterialTheme.colorScheme.onSurface        // Text on surface
MaterialTheme.colorScheme.surfaceVariant   // Variant backgrounds
MaterialTheme.colorScheme.onSurfaceVariant // Secondary text

MaterialTheme.colorScheme.outline          // Borders, dividers
MaterialTheme.colorScheme.outlineVariant   // Subtle dividers
```

**Prefer Material 3 scheme for**:
- UI backgrounds (`surface`, `surfaceVariant`)
- Text colors (`onSurface`, `onSurfaceVariant`)
- Borders and dividers (`outline`)

**Use AppColors for**:
- Semantic meaning (success, warning, error)
- Progress states
- Gradients
- Brand elements

---

## Intervention Colors

Specialized colors for intervention overlays (keep separate from AppColors):

```kotlin
InterventionColors.Reflection.*     // Soft blue - Contemplative
InterventionColors.TimeAlternative.* // Purple - Creative
InterventionColors.Breathing.*      // Cyan - Calming
InterventionColors.Stats.*          // Teal - Analytical
InterventionColors.Emotional.*      // Pink - Empathetic
InterventionColors.Activity.*       // Green - Energetic
```

**Usage**: Only in intervention overlay screens.

---

## Dark Mode Support

All colors automatically adapt to dark mode via Material 3 theming:

```kotlin
// Light mode
MaterialTheme.colorScheme.surface        // White
MaterialTheme.colorScheme.onSurface      // Black

// Dark mode (automatic)
MaterialTheme.colorScheme.surface        // Dark gray
MaterialTheme.colorScheme.onSurface      // White
```

**AppColors** provide the same color in both modes (they're accent colors, not backgrounds).

**Tips for dark mode**:
- Use `MaterialTheme.colorScheme` for surfaces
- Use `AppColors` for accents and semantic colors
- Test contrast ratios in both modes
- Use `.copy(alpha = 0.X)` for subtle variations

---

## Accessibility & Contrast

### WCAG AA Compliance

All color combinations meet WCAG AA (4.5:1) contrast requirements:

```kotlin
// Check contrast programmatically
val meetsRequirements = AccessibilityUtils.meetsContrastRequirements(
    foreground = AppColors.Primary.Default,
    background = MaterialTheme.colorScheme.surface,
    normalText = true
)
```

### High Contrast Mode

When high contrast is enabled (accessibility setting):

```kotlin
if (AccessibilityUtils.isHighContrastEnabled()) {
    // Increase contrast further
    val textColor = MaterialTheme.colorScheme.onSurface
} else {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
}
```

---

## Usage Examples

### Progress with Semantic Color
```kotlin
val usagePercentage = (currentUsage / dailyLimit * 100).toInt()
val progressColor = AppColors.Progress.getColorForPercentage(usagePercentage)

AppCircularProgress(
    progress = currentUsage / dailyLimit,
    percentageUsed = usagePercentage,
    color = progressColor
)
```

### Success Message
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = AppColors.Semantic.Success.Default
    )
    Text(
        text = "Goal saved successfully!",
        color = AppColors.Semantic.Success.Default,
        style = MaterialTheme.typography.bodyMedium
    )
}
```

### Streak Display
```kotlin
val streakDays = 42
val streakColor = AppColors.Streak.getColorForStreak(streakDays)

Row {
    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        tint = streakColor
    )
    Text(
        text = "$streakDays days",
        color = streakColor,
        fontWeight = FontWeight.Bold
    )
}
```

### Destructive Action
```kotlin
SecondaryButton(
    text = "Delete All Data",
    onClick = { showConfirmation = true },
    isDestructive = true  // Red color
)
```

---

## Migration Guide

### From Old Color System

| Old Pattern | New Pattern |
|------------|-------------|
| `PrimaryColors.Blue500` | `AppColors.Primary.Default` |
| `SemanticColors.Success` | `AppColors.Semantic.Success.Default` |
| `ProgressColors.OnTrack` | `AppColors.Progress.OnTrack` |
| `Color(0xFF2196F3)` | `AppColors.Primary.Default` |

### Deprecation Warnings

Old color objects are marked `@Deprecated` with automatic replacements:

```kotlin
// Compiler shows warning with fix suggestion
val color = PrimaryColors.Blue500
// Suggested replacement: AppColors.Primary.Default
```

---

## Best Practices

### DO ✓
- Use `AppColors` for semantic meaning
- Use `MaterialTheme.colorScheme` for UI surfaces and text
- Use helper functions (`getColorForPercentage`, `getColorForStreak`)
- Test colors in both light and dark mode
- Verify contrast ratios for accessibility
- Use gradients sparingly for emphasis

### DON'T ✗
- Hardcode color hex values (`Color(0xFF2196F3)`)
- Use primary colors for error states
- Use error colors for success states
- Ignore dark mode appearance
- Skip accessibility contrast testing
- Overuse gradients (visual noise)

---

## Color Accessibility Checklist

When using colors:

- [ ] Contrast ratio ≥ 4.5:1 for normal text
- [ ] Contrast ratio ≥ 3:1 for large text (18sp+)
- [ ] Color is not the only indicator (use icons too)
- [ ] Tested in dark mode
- [ ] Tested with high contrast mode
- [ ] Meaningful to colorblind users (position/icon redundancy)

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Typography](./Typography.md)
- [Components](./Components.md)
