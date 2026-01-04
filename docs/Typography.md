# Typography System

The ThinkFast typography system provides a complete Material 3 type scale with iOS-inspired semantic naming for cross-platform consistency.

## Material 3 Type Scale

### Display Styles (Hero Content)

Large, prominent text for hero sections and major headlines.

#### displayLarge
- **Size**: 57sp / 64sp line height
- **Weight**: Light
- **Usage**: App headers, hero text, major landing sections
- **Example**: Onboarding welcome screen title

```kotlin
Text(
    text = "Welcome to ThinkFast",
    style = MaterialTheme.typography.displayLarge
)
```

####displayMedium
- **Size**: 45sp / 52sp line height
- **Weight**: Regular
- **Usage**: Section headers, feature highlights
- **Example**: Major section dividers

```kotlin
Text(
    text = "Your Progress",
    style = MaterialTheme.typography.displayMedium
)
```

#### displaySmall
- **Size**: 36sp / 44sp line height
- **Weight**: Regular
- **Usage**: Card headers, important statistics
- **Example**: Large stat displays

```kotlin
Text(
    text = "15 days",
    style = MaterialTheme.typography.displaySmall
)
```

---

### Headline Styles (Major Sections)

Primary headings for screen titles and major content sections.

#### headlineLarge
- **Size**: 32sp / 40sp line height
- **Weight**: Regular
- **Usage**: Screen titles, dialog headers
- **Example**: Main screen top bar title

```kotlin
Text(
    text = "Statistics",
    style = MaterialTheme.typography.headlineLarge
)
```

#### headlineMedium
- **Size**: 28sp / 36sp line height
- **Weight**: Regular
- **Usage**: Dialog titles, important headings
- **Example**: Confirmation dialog title

```kotlin
Text(
    text = "Delete Goal?",
    style = MaterialTheme.typography.headlineMedium
)
```

#### headlineSmall
- **Size**: 24sp / 32sp line height
- **Weight**: Regular
- **Usage**: Card titles, section headers
- **Example**: Stats card header

```kotlin
Text(
    text = "Today's Progress",
    style = MaterialTheme.typography.headlineSmall
)
```

---

### Title Styles (Subsections)

Secondary headings for list sections and content subsections.

#### titleLarge
- **Size**: 22sp / 28sp line height
- **Weight**: Medium
- **Usage**: List section headers, prominent labels
- **Example**: Settings section headers

```kotlin
Text(
    text = "Account",
    style = MaterialTheme.typography.titleLarge
)
```

#### titleMedium
- **Size**: 16sp / 24sp line height
- **Weight**: Medium
- **Letter Spacing**: 0.15sp
- **Usage**: List item titles, card subtitles
- **Example**: App name in goal list

```kotlin
Text(
    text = "Instagram",
    style = MaterialTheme.typography.titleMedium
)
```

#### titleSmall
- **Size**: 14sp / 20sp line height
- **Weight**: Medium
- **Letter Spacing**: 0.1sp
- **Usage**: Dense list titles, compact headers
- **Example**: Compact list items

```kotlin
Text(
    text = "Quick Win",
    style = MaterialTheme.typography.titleSmall
)
```

---

### Body Styles (Content)

Primary text for content, descriptions, and paragraphs.

#### bodyLarge
- **Size**: 16sp / 24sp line height
- **Weight**: Regular
- **Letter Spacing**: 0.5sp
- **Usage**: Primary content, main descriptions
- **Example**: Empty state descriptions, main text

```kotlin
Text(
    text = "Set daily usage limits for your tracked apps to build healthier habits.",
    style = MaterialTheme.typography.bodyLarge
)
```

#### bodyMedium
- **Size**: 14sp / 20sp line height
- **Weight**: Regular
- **Letter Spacing**: 0.25sp
- **Usage**: Secondary content, supporting text
- **Example**: Subtitles, secondary descriptions

```kotlin
Text(
    text = "You're doing great! Keep it up.",
    style = MaterialTheme.typography.bodyMedium
)
```

#### bodySmall
- **Size**: 12sp / 16sp line height
- **Weight**: Regular
- **Letter Spacing**: 0.4sp
- **Usage**: Captions, metadata, timestamps
- **Example**: Last updated time, goal metadata

```kotlin
Text(
    text = "Last updated 2 hours ago",
    style = MaterialTheme.typography.bodySmall
)
```

---

### Label Styles (UI Elements)

Text for buttons, chips, and interactive UI elements.

#### labelLarge
- **Size**: 14sp / 20sp line height
- **Weight**: Medium
- **Letter Spacing**: 0.1sp
- **Usage**: Button text, prominent labels
- **Example**: Primary button text

```kotlin
// Used internally by PrimaryButton component
Text(
    text = "Save Goal",
    style = MaterialTheme.typography.labelLarge
)
```

#### labelMedium
- **Size**: 12sp / 16sp line height
- **Weight**: Medium
- **Letter Spacing**: 0.5sp
- **Usage**: Chip text, small buttons, tabs
- **Example**: Tab labels, filter chips

```kotlin
Text(
    text = "Today",
    style = MaterialTheme.typography.labelMedium
)
```

#### labelSmall
- **Size**: 11sp / 16sp line height
- **Weight**: Medium
- **Letter Spacing**: 0.5sp
- **Usage**: Small chips, badges, minimal labels
- **Example**: Notification badges, small tags

```kotlin
Text(
    text = "New",
    style = MaterialTheme.typography.labelSmall
)
```

---

## iOS-Inspired Semantic Typography

For developers familiar with iOS, these semantic names map Material 3 styles to iOS equivalents:

### AppTypography Extension

```kotlin
import dev.sadakat.thinkfaster.ui.theme.AppTypography

// iOS-style semantic names
Text("Caption text", style = AppTypography.caption)        // 12sp
Text("Footnote text", style = AppTypography.footnote)      // 13sp → 12sp
Text("Body text", style = AppTypography.body)              // 17sp → 16sp
Text("Headline", style = AppTypography.headline)           // 17sp → 16sp
Text("Title 3", style = AppTypography.title3)              // 20sp → 22sp
Text("Title 2", style = AppTypography.title2)              // 22sp → 24sp
Text("Title 1", style = AppTypography.title1)              // 28sp
Text("Large Title", style = AppTypography.largeTitle)      // 34sp → 36sp
```

### iOS → Material 3 Mapping

| iOS Style | Material 3 Equivalent | Size |
|-----------|----------------------|------|
| caption | bodySmall | 12sp |
| footnote | labelMedium | 12sp |
| body | bodyLarge | 16sp |
| headline | titleMedium | 16sp |
| title3 | titleLarge | 22sp |
| title2 | headlineSmall | 24sp |
| title1 | headlineMedium | 28sp |
| largeTitle | displaySmall | 36sp |

---

## Specialized Typography

### Intervention Typography

For intervention overlays, specialized typography with psychological impact is available:

```kotlin
import dev.sadakat.thinkfaster.ui.theme.InterventionTypography

// Psychology-backed typography for different intervention types
Text(
    text = "Take a moment to reflect",
    style = InterventionTypography.reflectionTitle  // Serif for contemplation
)

Text(
    text = "5 minutes used today",
    style = InterventionTypography.statsContent     // Monospace for precision
)
```

**When to use**:
- Intervention overlay screens only
- Content requiring psychological emphasis
- Statistics requiring precision perception

**When NOT to use**:
- Regular app screens
- Standard UI elements
- Navigation or settings

---

## Usage Examples

### Screen Title
```kotlin
@Composable
fun MyScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Screen",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            )
        }
    ) { /* content */ }
}
```

### Card with Title and Description
```kotlin
StandardCard {
    Text(
        text = "Total Usage",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "2h 15m today",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "25% less than yesterday",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

### List Item
```kotlin
ListItem(
    headlineContent = {
        Text(
            text = "Instagram",
            style = MaterialTheme.typography.titleMedium
        )
    },
    supportingContent = {
        Text(
            text = "Daily limit: 30 minutes",
            style = MaterialTheme.typography.bodyMedium
        )
    }
)
```

### Empty State
```kotlin
EmptyStateView(
    icon = Icons.Default.CheckCircle,
    title = "No Goals Yet",  // Uses headlineMedium internally
    subtitle = "Set your first goal to start building better habits."  // Uses bodyMedium
)
```

---

## Font Weight Hierarchy

Proper weight usage creates visual hierarchy:

```kotlin
// Titles and headers: Bold or SemiBold
Text(
    text = "Important Header",
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold
)

// Body content: Regular
Text(
    text = "This is regular body text for reading.",
    style = MaterialTheme.typography.bodyLarge
    // fontWeight defaults to Regular
)

// Labels and metadata: Medium
Text(
    text = "Section Label",
    style = MaterialTheme.typography.titleMedium
    // fontWeight is Medium by default in titleMedium
)

// Captions and hints: Regular
Text(
    text = "Last updated today",
    style = MaterialTheme.typography.bodySmall
    // fontWeight defaults to Regular
)
```

---

## Color with Typography

Typography should use semantic colors from the theme:

```kotlin
// Primary content
Text(
    text = "Main content",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurface  // Highest contrast
)

// Secondary content
Text(
    text = "Supporting text",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant  // Lower contrast
)

// Disabled or tertiary content
Text(
    text = "Disabled text",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
```

---

## Accessibility Considerations

### Dynamic Type

The typography system automatically respects user font size preferences:

```kotlin
// ✓ Correct - scales with user preferences
Text(
    text = "Accessible text",
    style = MaterialTheme.typography.bodyLarge
)

// ✗ Incorrect - ignores user preferences
Text(
    text = "Fixed size text",
    fontSize = 16.sp  // DON'T DO THIS
)
```

### Minimum Touch Targets

When text is interactive, ensure minimum 48dp touch target:

```kotlin
Text(
    text = "Tap me",
    style = MaterialTheme.typography.labelLarge,
    modifier = Modifier
        .clickable { /* action */ }
        .padding(Spacing.md)  // Ensures 48dp touch target
)
```

### Contrast Ratios

- **bodyLarge on surface**: WCAG AA compliant (4.5:1 minimum)
- **bodySmall on surface**: Test with `AccessibilityUtils.meetsContrastRequirements()`
- **Text on colored backgrounds**: Always verify contrast

---

## Migration Guide

### Before (Hardcoded)
```kotlin
Text(
    text = "Title",
    fontSize = 18.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 24.sp
)
```

### After (Design System)
```kotlin
Text(
    text = "Title",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold
)
```

### Common Migrations

| Old Pattern | New Pattern |
|------------|-------------|
| `fontSize = 16.sp` | `style = MaterialTheme.typography.bodyLarge` |
| `fontSize = 14.sp` | `style = MaterialTheme.typography.bodyMedium` |
| `fontSize = 12.sp` | `style = MaterialTheme.typography.bodySmall` |
| `fontSize = 18.sp, fontWeight = Bold` | `style = MaterialTheme.typography.titleMedium, fontWeight = Bold` |
| `fontSize = 24.sp` | `style = MaterialTheme.typography.headlineSmall` |
| `fontSize = 28.sp` | `style = MaterialTheme.typography.headlineMedium` |

---

## Best Practices

### DO ✓
- Use Material 3 typography for all text
- Choose appropriate style for content hierarchy
- Use `fontWeight` parameter for emphasis when needed
- Test with large font sizes (accessibility settings)
- Use semantic colors from theme

### DON'T ✗
- Hardcode `fontSize`, `lineHeight`, `letterSpacing`
- Create custom `TextStyle` objects unnecessarily
- Use display styles for body content
- Use body styles for headlines
- Ignore dynamic type preferences

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Colors](./Colors.md)
- [Migration Guide](./Migration.md)
