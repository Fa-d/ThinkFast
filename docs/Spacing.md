# Spacing System

The ThinkFast spacing system uses a 4dp baseline grid to create consistent rhythm and visual hierarchy throughout the app.

## 4dp Baseline Grid

All spacing values are multiples of 4dp, creating a predictable and harmonious layout system:

```kotlin
Spacing.xs   //  4dp - Tiny gaps, icon padding
Spacing.sm   //  8dp - Small gaps, tight spacing
Spacing.md   // 16dp - Default padding, card padding
Spacing.lg   // 24dp - Section spacing, large padding
Spacing.xl   // 32dp - Extra large gaps
Spacing.xxl  // 48dp - Hero section spacing
```

### When to Use Each Size

#### xs (4dp) - Tiny Gaps
- Icon padding within buttons
- Minimal spacing between tightly related elements
- Badge offset
- Fine-tuning alignment

**Example**:
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    Icon(imageVector = Icons.Default.Star)
    Icon(imageVector = Icons.Default.Star)
    Icon(imageVector = Icons.Default.Star)
}
```

#### sm (8dp) - Small Gaps
- List item internal spacing
- Spacing between icon and text
- Chip padding
- Close elements that are related

**Example**:
```kotlin
Row(horizontalArrangement = Spacing.horizontalArrangementSM) {
    Icon(imageVector = Icons.Default.CheckCircle)
    Text("Goal achieved!")
}
```

#### md (16dp) - Default Padding **[Most Common]**
- Card padding
- Screen horizontal padding
- Default vertical spacing between elements
- List item padding

**Example**:
```kotlin
Column(
    modifier = Modifier.padding(Spacing.md),
    verticalArrangement = Spacing.verticalArrangementMD
) {
    Text("Title")
    Text("Description")
}
```

#### lg (24dp) - Section Spacing
- Spacing between major sections
- Large card padding
- Dialog content padding
- Generous breathing room

**Example**:
```kotlin
Column(verticalArrangement = Spacing.verticalArrangementLG) {
    SectionHeader("Statistics")
    StatisticsContent()

    SectionHeader("Goals")
    GoalsContent()
}
```

#### xl (32dp) - Extra Large Gaps
- Top/bottom screen padding
- Major section separation
- Empty state padding
- Hero content spacing

**Example**:
```kotlin
Box(modifier = Modifier.padding(Spacing.xl)) {
    EmptyStateView(/* ... */)
}
```

#### xxl (48dp) - Hero Spacing
- Landing page sections
- Major feature separation
- Celebration screens
- Dramatic spacing for emphasis

**Example**:
```kotlin
Column(verticalArrangement = Spacing.verticalArrangementXXL) {
    HeroImage()
    HeroTitle()
    HeroDescription()
}
```

---

## Component-Specific Spacing

Pre-defined spacing objects for common components ensure consistency:

### Button Spacing

```kotlin
Spacing.Button.horizontal        // 24dp - Button horizontal padding
Spacing.Button.vertical          // 12dp - Button vertical padding
Spacing.Button.iconPadding       //  8dp - Icon-text spacing
Spacing.Button.gap               //  8dp - Gap between buttons
Spacing.Button.textButtonHorizontal  // 16dp - Text button padding
```

**Usage**:
```kotlin
Button(
    contentPadding = PaddingValues(
        horizontal = Spacing.Button.horizontal,
        vertical = Spacing.Button.vertical
    )
) {
    Icon(/* ... */)
    Spacer(modifier = Modifier.width(Spacing.Button.iconPadding))
    Text("Save")
}
```

### Card Spacing

```kotlin
Spacing.Card.padding            // 16dp - Card internal padding
Spacing.Card.gap                // 12dp - Gap between card elements
Spacing.Card.headerBottomPadding // 8dp - Header bottom spacing
Spacing.Card.elevation          //  2dp - Default card elevation
```

**Usage**:
```kotlin
Card(
    modifier = Modifier.padding(Spacing.Card.padding)
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Card.gap)) {
        CardHeader()
        CardContent()
        CardFooter()
    }
}
```

### List Spacing

```kotlin
Spacing.List.itemPadding        // 16dp - List item internal padding
Spacing.List.itemGap            //  8dp - Gap between list items
Spacing.List.sectionGap         // 24dp - Gap between list sections
Spacing.List.iconTextGap        //  12dp - Icon to text spacing
```

**Usage**:
```kotlin
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(Spacing.List.itemGap)
) {
    items(goals) { goal ->
        ListItem(
            modifier = Modifier.padding(Spacing.List.itemPadding)
        ) {
            // Content
        }
    }
}
```

### Dialog Spacing

```kotlin
Spacing.Dialog.padding          // 24dp - Dialog padding
Spacing.Dialog.titleBottomPadding // 16dp - Title bottom spacing
Spacing.Dialog.buttonTopPadding // 24dp - Buttons top spacing
Spacing.Dialog.buttonGap        //  8dp - Gap between dialog buttons
```

**Usage**:
```kotlin
AlertDialog(
    onDismissRequest = { },
    title = { Text("Delete Goal?") },
    text = {
        Column(
            modifier = Modifier.padding(Spacing.Dialog.padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Dialog.titleBottomPadding)
        ) {
            Text("This action cannot be undone.")
        }
    },
    confirmButton = { },
    dismissButton = { }
)
```

### Screen Spacing

```kotlin
Spacing.Screen.horizontal       // 16dp - Screen horizontal padding
Spacing.Screen.vertical         // 16dp - Screen vertical padding
Spacing.Screen.sectionGap       // 24dp - Section vertical gap
Spacing.Screen.topPadding       // 16dp - Top safe area padding
Spacing.Screen.bottomPadding    // 16dp - Bottom safe area padding
```

**Usage**:
```kotlin
Scaffold { paddingValues ->
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = Spacing.Screen.horizontal,
            top = paddingValues.calculateTopPadding() + Spacing.Screen.topPadding,
            bottom = paddingValues.calculateBottomPadding() + Spacing.Screen.bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Screen.sectionGap)
    ) {
        // Screen content
    }
}
```

### Input Spacing

```kotlin
Spacing.Input.padding           // 16dp - TextField padding
Spacing.Input.gap               // 12dp - Gap between form fields
Spacing.Input.labelGap          //  4dp - Label to field gap
Spacing.Input.errorGap          //  4dp - Field to error message gap
```

**Usage**:
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(Spacing.Input.gap)) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        modifier = Modifier.padding(Spacing.Input.padding)
    )
    OutlinedTextField(
        value = value2,
        onValueChange = { }
    )
}
```

### Bottom Sheet Spacing

```kotlin
Spacing.BottomSheet.topPadding  // 24dp - Top handle area
Spacing.BottomSheet.contentPadding // 16dp - Content padding
Spacing.BottomSheet.handleWidth // 32dp - Handle width
Spacing.BottomSheet.handleHeight //  4dp - Handle height
Spacing.BottomSheet.handleTopPadding // 12dp - Handle top spacing
```

---

## Arrangement Helpers

Pre-configured `Arrangement` objects for common spacing patterns:

```kotlin
// Horizontal arrangements
Spacing.horizontalArrangementXS  // spacedBy(4.dp)
Spacing.horizontalArrangementSM  // spacedBy(8.dp)
Spacing.horizontalArrangementMD  // spacedBy(16.dp)
Spacing.horizontalArrangementLG  // spacedBy(24.dp)

// Vertical arrangements
Spacing.verticalArrangementXS    // spacedBy(4.dp)
Spacing.verticalArrangementSM    // spacedBy(8.dp)
Spacing.verticalArrangementMD    // spacedBy(16.dp)
Spacing.verticalArrangementLG    // spacedBy(24.dp)
Spacing.verticalArrangementXL    // spacedBy(32.dp)
Spacing.verticalArrangementXXL   // spacedBy(48.dp)
```

**Usage**:
```kotlin
// Instead of this
Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // Content
}

// Use this
Column(verticalArrangement = Spacing.verticalArrangementMD) {
    // Content
}
```

---

## PaddingValues Helpers

Quickly create `PaddingValues` for common patterns:

```kotlin
Spacing.all(size: Dp)                    // All sides
Spacing.horizontal(horizontal: Dp)       // Left and right
Spacing.vertical(vertical: Dp)           // Top and bottom
Spacing.symmetric(                       // Horizontal and vertical
    horizontal: Dp,
    vertical: Dp
)
```

**Usage**:
```kotlin
// All sides 16dp
LazyColumn(
    contentPadding = Spacing.all(Spacing.md)
) { }

// Horizontal 16dp, vertical 24dp
LazyColumn(
    contentPadding = Spacing.symmetric(
        horizontal = Spacing.md,
        vertical = Spacing.lg
    )
) { }
```

---

## Responsive Spacing

Spacing adapts to screen size using `ResponsiveLayout`:

```kotlin
val screenSize = ResponsiveLayout.getScreenSize()
val adaptivePadding = ResponsiveLayout.getAdaptivePadding(screenSize)
```

**Responsive values**:
- **Small screens** (< 600dp): Default spacing
- **Medium screens** (600-840dp): +8dp padding
- **Large screens** (> 840dp): +16dp padding

**Usage**:
```kotlin
val screenSize = ResponsiveLayout.getScreenSize()
val padding = when (screenSize) {
    ScreenSize.SMALL -> Spacing.md
    ScreenSize.MEDIUM -> Spacing.lg
    ScreenSize.LARGE -> Spacing.xl
    ScreenSize.EXTRA_LARGE -> Spacing.xxl
}

Column(modifier = Modifier.padding(padding)) {
    // Adaptive content
}
```

---

## Usage Examples

### Screen Layout
```kotlin
@Composable
fun MyScreen() {
    Scaffold { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = Spacing.Screen.horizontal,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding()
            ),
            verticalArrangement = Spacing.verticalArrangementLG
        ) {
            item {
                SectionHeader("Statistics")
            }
            item {
                StatisticsCard()
            }
            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            item {
                SectionHeader("Goals")
            }
            items(goals) { goal ->
                GoalCard(goal)
            }
        }
    }
}
```

### Card with Consistent Spacing
```kotlin
@Composable
fun StatCard() {
    Card(
        modifier = Modifier.padding(Spacing.Card.padding)
    ) {
        Column(
            verticalArrangement = Spacing.verticalArrangementMD
        ) {
            // Header
            Row(
                horizontalArrangement = Spacing.horizontalArrangementSM,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.TrendingUp)
                Text("Total Usage", style = MaterialTheme.typography.titleMedium)
            }

            // Value
            Text(
                text = "2h 15m",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            // Subtitle
            Text(
                text = "25% less than yesterday",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Semantic.Success.Default
            )
        }
    }
}
```

### Form with Input Spacing
```kotlin
@Composable
fun GoalEditorForm() {
    Column(
        modifier = Modifier.padding(Spacing.md),
        verticalArrangement = Spacing.verticalArrangementLG
    ) {
        // App selection
        Text(
            text = "Select App",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.Input.labelGap))
        AppDropdown()

        // Daily limit
        Spacer(modifier = Modifier.height(Spacing.Input.gap))
        Text(
            text = "Daily Limit",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.Input.labelGap))
        LimitSlider()

        // Action buttons
        Spacer(modifier = Modifier.height(Spacing.Dialog.buttonTopPadding))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Button.gap),
            modifier = Modifier.fillMaxWidth()
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

---

## Migration Guide

### Before (Hardcoded)
```kotlin
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    Card(modifier = Modifier.padding(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(/* ... */)
            Text("Text")
        }
    }
}
```

### After (Design System)
```kotlin
Column(
    modifier = Modifier.padding(Spacing.md),
    verticalArrangement = Spacing.verticalArrangementLG
) {
    StandardCard {  // Padding already applied
        Row(horizontalArrangement = Spacing.horizontalArrangementSM) {
            Icon(/* ... */)
            Text("Text")
        }
    }
}
```

### Common Migrations

| Old Pattern | New Pattern |
|------------|-------------|
| `padding(4.dp)` | `padding(Spacing.xs)` |
| `padding(8.dp)` | `padding(Spacing.sm)` |
| `padding(16.dp)` | `padding(Spacing.md)` |
| `padding(20.dp)` | `padding(Spacing.lg)` (24dp closest) |
| `padding(24.dp)` | `padding(Spacing.lg)` |
| `padding(32.dp)` | `padding(Spacing.xl)` |
| `Arrangement.spacedBy(8.dp)` | `Spacing.verticalArrangementSM` |
| `Arrangement.spacedBy(16.dp)` | `Spacing.verticalArrangementMD` |

---

## Best Practices

### DO ✓
- Use component-specific spacing (`Spacing.Card.padding`, `Spacing.Button.horizontal`)
- Use arrangement helpers for consistent spacing
- Stick to the 4dp grid (xs, sm, md, lg, xl, xxl)
- Use responsive spacing for larger screens
- Test layouts on different screen sizes

### DON'T ✗
- Hardcode dimensions (`16.dp`, `20.dp`)
- Use arbitrary values not in the grid (`15.dp`, `13.dp`)
- Mix hardcoded and token spacing
- Create custom spacing values without justification
- Ignore responsive considerations

---

## Spacing Accessibility

### Touch Targets
Ensure minimum 48dp touch target for interactive elements:

```kotlin
// ✓ Correct - 48dp touch target
IconButton(
    onClick = { },
    modifier = Modifier.size(48.dp)
) {
    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
}

// ✗ Incorrect - too small
Icon(
    imageVector = Icons.Default.Delete,
    modifier = Modifier
        .size(16.dp)
        .clickable { }  // Touch target too small!
)
```

### Generous Spacing for Readability
Use larger spacing (`lg`, `xl`) for:
- Reading-heavy content
- Accessibility mode (large fonts)
- Important decisions (confirmations)

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Shapes](./Shapes.md)
- [Components](./Components.md)
