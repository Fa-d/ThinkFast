# Component Library

The ThinkFast component library provides reusable, polished UI components that automatically use the design system tokens. All components follow Material 3 principles with iOS-inspired interactions.

## Component Principles

1. **Design Token Usage** - All components use `Spacing.*`, `Shapes.*`, `MaterialTheme.typography.*`, and `AppColors.*`
2. **iOS-Inspired Polish** - Scale animations on press, spring physics, haptic feedback
3. **Accessibility First** - Minimum 48dp touch targets, proper contrast, semantic colors
4. **Consistent API** - Similar parameter patterns across all components
5. **Composable by Default** - Built for Jetpack Compose, no XML

---

## Buttons

### PrimaryButton

**Purpose**: Main call-to-action button with gradient background.

**Features**:
- Gradient background (blue → purple)
- Scale animation on press (0.95 → 1.0)
- Haptic feedback
- Loading state with spinner
- Disabled state
- Optional leading icon

**Usage**:
```kotlin
PrimaryButton(
    text = "Save Goal",
    onClick = { viewModel.saveGoal() }
)

// With icon
PrimaryButton(
    text = "Add Goal",
    onClick = { navigateToAddGoal() },
    leadingIcon = {
        Icon(Icons.Default.Add, contentDescription = null)
    }
)

// Loading state
PrimaryButton(
    text = "Saving...",
    onClick = { },
    isLoading = true
)

// Disabled
PrimaryButton(
    text = "Save",
    onClick = { },
    enabled = false
)
```

**Parameters**:
- `text: String` - Button label text
- `onClick: () -> Unit` - Click callback
- `modifier: Modifier = Modifier` - Optional modifier
- `enabled: Boolean = true` - Enable/disable button
- `isLoading: Boolean = false` - Show loading spinner
- `leadingIcon: @Composable (() -> Unit)? = null` - Optional icon before text
- `hapticFeedback: Boolean = true` - Enable haptic feedback

---

### SecondaryButton

**Purpose**: Secondary actions with outlined style.

**Features**:
- Outlined border (1dp)
- Secondary background
- Destructive variant (red color)
- Scale animation on press
- Haptic feedback

**Usage**:
```kotlin
SecondaryButton(
    text = "Cancel",
    onClick = { dismiss() }
)

// Destructive action
SecondaryButton(
    text = "Delete Goal",
    onClick = { deleteGoal() },
    isDestructive = true
)

// With icon
SecondaryButton(
    text = "Back",
    onClick = { navigateBack() },
    leadingIcon = {
        Icon(Icons.Default.ArrowBack, contentDescription = null)
    }
)
```

**Parameters**:
- `text: String` - Button label text
- `onClick: () -> Unit` - Click callback
- `modifier: Modifier = Modifier` - Optional modifier
- `enabled: Boolean = true` - Enable/disable button
- `isDestructive: Boolean = false` - Use error/destructive styling
- `leadingIcon: @Composable (() -> Unit)? = null` - Optional icon
- `hapticFeedback: Boolean = true` - Enable haptic feedback

---

### AppTextButton

**Purpose**: Minimal text-only button for tertiary actions.

**Features**:
- No background or border
- Text only with minimal padding
- Scale animation on press
- Lighter haptic feedback

**Usage**:
```kotlin
AppTextButton(
    text = "Learn More",
    onClick = { openHelp() }
)

AppTextButton(
    text = "Skip",
    onClick = { skipOnboarding() }
)
```

**Parameters**:
- `text: String` - Button label text
- `onClick: () -> Unit` - Click callback
- `modifier: Modifier = Modifier` - Optional modifier
- `enabled: Boolean = true` - Enable/disable button
- `hapticFeedback: Boolean = true` - Enable haptic feedback

---

## Cards

### StandardCard

**Purpose**: Default card for content grouping.

**Features**:
- Consistent padding (`Spacing.Card.padding` = 16dp)
- Rounded corners (`Shapes.card` = 16dp)
- 2dp elevation
- Optional onClick for interactive cards
- ColumnScope content for easy vertical layout

**Usage**:
```kotlin
StandardCard {
    Text(
        text = "Today's Progress",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = "2h 15m",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
}

// Interactive card
StandardCard(
    onClick = { navigateToDetails() }
) {
    // Content
}
```

**Parameters**:
- `modifier: Modifier = Modifier` - Optional modifier
- `onClick: (() -> Unit)? = null` - Optional click handler
- `elevation: Dp = 2.dp` - Card elevation
- `content: @Composable ColumnScope.() -> Unit` - Card content

---

### ElevatedCard

**Purpose**: Prominent card with higher elevation for featured content.

**Features**:
- Higher elevation (8dp)
- Same consistent styling as StandardCard

**Usage**:
```kotlin
ElevatedCard {
    Text("Featured Content")
    Text("This card stands out with higher elevation")
}
```

**Parameters**: Same as StandardCard except elevation (fixed at 8dp)

---

### OutlinedCard

**Purpose**: Flat card with border, no shadow.

**Features**:
- 1dp border, no elevation
- Configurable border color
- Lighter visual weight

**Usage**:
```kotlin
OutlinedCard {
    Text("Outlined Content")
}

// Custom border color
OutlinedCard(
    borderColor = AppColors.Semantic.Warning.Default
) {
    Text("Warning Message")
}
```

**Parameters**:
- `modifier: Modifier = Modifier` - Optional modifier
- `onClick: (() -> Unit)? = null` - Optional click handler
- `borderColor: Color = MaterialTheme.colorScheme.outline` - Border color
- `content: @Composable ColumnScope.() -> Unit` - Card content

---

### StatCard

**Purpose**: Display statistics with icon, title, value, and subtitle.

**Features**:
- Icon + title header
- Large bold value display
- Optional subtitle
- Color-coded icon for visual hierarchy

**Usage**:
```kotlin
StatCard(
    title = "Total Usage",
    value = "2h 15m",
    subtitle = "25% less than yesterday",
    icon = Icons.Default.TrendingUp,
    color = AppColors.Semantic.Success.Default
)

// Without icon
StatCard(
    title = "Streak",
    value = "7 days"
)
```

**Parameters**:
- `title: String` - Stat label
- `value: String` - Stat value
- `modifier: Modifier = Modifier` - Optional modifier
- `subtitle: String? = null` - Optional subtitle
- `icon: ImageVector? = null` - Optional icon
- `color: Color = MaterialTheme.colorScheme.primary` - Icon/accent color

---

### InfoCard

**Purpose**: Colored container card for callouts, tips, warnings.

**Features**:
- Colored background
- Optional icon
- Title and description layout

**Usage**:
```kotlin
InfoCard(
    title = "Tip",
    description = "Set realistic goals to build sustainable habits.",
    icon = Icons.Default.Lightbulb,
    containerColor = AppColors.Semantic.Info.Container
)
```

**Parameters**:
- `title: String` - Card title
- `description: String` - Description text
- `modifier: Modifier = Modifier` - Optional modifier
- `icon: ImageVector? = null` - Optional leading icon
- `containerColor: Color` - Background color
- `contentColor: Color` - Text color

---

## Progress Indicators

### AppLinearProgressBar

**Purpose**: Linear progress bar with semantic colors.

**Features**:
- Semantic colors based on percentage (green 0-75%, orange 75-100%, red >100%)
- Optional percentage label
- Smooth animation on progress changes
- Rounded corners

**Usage**:
```kotlin
AppLinearProgressBar(
    progress = 0.85f,
    percentageUsed = 85,
    showLabel = true
)

// Without label
AppLinearProgressBar(
    progress = 0.5f,
    showLabel = false
)
```

**Parameters**:
- `progress: Float` - Progress (0.0 - 1.0)
- `modifier: Modifier = Modifier` - Optional modifier
- `percentageUsed: Int` - Percentage for color (0-100+)
- `showLabel: Boolean = true` - Show percentage label

**Color Logic**:
- 0-75%: Green (OnTrack)
- 75-100%: Orange (Approaching)
- >100%: Red (OverLimit)

---

### AppCircularProgress

**Purpose**: Circular progress with percentage display in center.

**Features**:
- Circular progress with percentage in center
- Semantic colors based on progress
- Spring animation (iOS-style)
- Round stroke caps for polished look
- Configurable size and stroke width

**Usage**:
```kotlin
AppCircularProgress(
    progress = 0.75f,
    percentageUsed = 75,
    size = 100.dp
)

// Without percentage text
AppCircularProgress(
    progress = 0.5f,
    showPercentage = false
)

// Custom size
AppCircularProgress(
    progress = 0.85f,
    size = 120.dp,
    strokeWidth = 10.dp
)
```

**Parameters**:
- `progress: Float` - Progress (0.0 - 1.0)
- `modifier: Modifier = Modifier` - Optional modifier
- `size: Dp = 80.dp` - Circle diameter
- `strokeWidth: Dp = 8.dp` - Stroke width
- `percentageUsed: Int` - Percentage for color/display
- `showPercentage: Boolean = true` - Show percentage text

---

### IndeterminateProgress

**Purpose**: Spinning progress indicator for unknown duration.

**Usage**:
```kotlin
IndeterminateProgress(
    modifier = Modifier.size(48.dp)
)

IndeterminateProgress(
    color = AppColors.Semantic.Success.Default,
    strokeWidth = 6.dp
)
```

**Parameters**:
- `modifier: Modifier = Modifier` - Optional modifier
- `color: Color = MaterialTheme.colorScheme.primary` - Indicator color
- `strokeWidth: Dp = 4.dp` - Stroke width

---

## Empty States

### EmptyStateView

**Purpose**: Full empty state with icon, title, subtitle, and optional action.

**Features**:
- Large icon (60dp) for visual context
- Headline title
- Body subtitle with explanation
- Optional action button
- Centered layout with generous spacing

**Usage**:
```kotlin
EmptyStateView(
    icon = Icons.Default.CheckCircle,
    title = "No Goals Yet",
    subtitle = "Set your first goal to start building better habits.",
    actionText = "Add Goal",
    onAction = { navigateToAddGoal() }
)

// Without action
EmptyStateView(
    icon = Icons.Default.Inbox,
    title = "No Data",
    subtitle = "Check back later for updates."
)
```

**Parameters**:
- `icon: ImageVector` - Large icon
- `title: String` - Headline text
- `subtitle: String` - Supporting text
- `modifier: Modifier = Modifier` - Optional modifier
- `actionText: String? = null` - Optional action button text
- `onAction: (() -> Unit)? = null` - Optional action callback

---

### CompactEmptyState

**Purpose**: Smaller empty state for cards or limited space.

**Usage**:
```kotlin
CompactEmptyState(
    icon = Icons.Default.Inbox,
    message = "No data available"
)
```

**Parameters**:
- `icon: ImageVector` - Icon
- `message: String` - Brief message
- `modifier: Modifier = Modifier` - Optional modifier

---

## Loading States

### LoadingIndicator

**Purpose**: Centered loading spinner with optional message.

**Usage**:
```kotlin
LoadingIndicator(
    message = "Loading your goals..."
)

// Without message
LoadingIndicator()
```

**Parameters**:
- `modifier: Modifier = Modifier` - Optional modifier
- `message: String? = null` - Optional loading message
- `color: Color = MaterialTheme.colorScheme.primary` - Spinner color

---

### CompactLoadingIndicator

**Purpose**: Small inline loading spinner.

**Usage**:
```kotlin
// In a button or card
CompactLoadingIndicator()

CompactLoadingIndicator(
    size = 16,
    color = Color.White
)
```

**Parameters**:
- `modifier: Modifier = Modifier` - Optional modifier
- `size: Int = 24` - Spinner size in dp
- `color: Color = MaterialTheme.colorScheme.primary` - Spinner color

---

### FullScreenLoading

**Purpose**: Full-screen loading overlay with message.

**Usage**:
```kotlin
if (isLoading) {
    FullScreenLoading(
        message = "Syncing your data..."
    )
}
```

**Parameters**:
- `message: String` - Loading message
- `modifier: Modifier = Modifier` - Optional modifier

---

## Component Combinations

### Common Patterns

#### Card with Progress
```kotlin
StandardCard {
    Text(
        text = "Daily Goal",
        style = MaterialTheme.typography.titleMedium
    )
    AppLinearProgressBar(
        progress = 0.75f,
        percentageUsed = 75
    )
}
```

#### Stat Card with Circular Progress
```kotlin
StandardCard {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's Usage",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "2h 15m",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
        AppCircularProgress(
            progress = 0.75f,
            size = 80.dp
        )
    }
}
```

#### Action Card with Buttons
```kotlin
StandardCard {
    Text("Delete all data?")
    Text("This action cannot be undone.")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Button.gap)
    ) {
        SecondaryButton(
            text = "Cancel",
            onClick = { dismiss() },
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = "Delete",
            onClick = { deleteData() },
            modifier = Modifier.weight(1f)
        )
    }
}
```

---

## Best Practices

### DO ✓
- Use component library instead of creating custom components
- Pass `modifier` parameter to allow parent control of size/position
- Use semantic colors from `AppColors` for consistent meaning
- Test components with long text to ensure proper wrapping
- Test dark mode appearance
- Verify touch targets are minimum 48dp

### DON'T ✗
- Create custom buttons/cards without using library components
- Hardcode dimensions - use design tokens
- Skip accessibility testing
- Ignore loading/error states
- Use non-semantic colors for status indication

---

## Accessibility Considerations

All components follow accessibility best practices:

- **Touch Targets**: Minimum 48dp for all interactive elements
- **Contrast**: WCAG AA compliance (4.5:1 minimum)
- **Dynamic Type**: All text scales with user font size preferences
- **Content Descriptions**: Icons have proper content descriptions
- **Semantic Colors**: Consistent color meaning (green = success, red = error)
- **Haptic Feedback**: Tactile confirmation for interactions
- **Focus Indicators**: Visible focus states for keyboard navigation

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Typography](./Typography.md)
- [Colors](./Colors.md)
- [Spacing](./Spacing.md)
- [Shapes](./Shapes.md)
- [Animation](./Animation.md)
- [Migration Guide](./Migration.md)
