# Shape System

The ThinkFast shape system provides consistent corner radius values for all UI components, creating a cohesive and modern aesthetic.

## Corner Radius Scale

All corner radii use a predictable scale based on component size and importance:

```kotlin
Shapes.sm   //  8dp - Buttons, input fields, chips
Shapes.md   // 12dp - Cards, toggle buttons
Shapes.lg   // 16dp - Large cards, bottom sheets
Shapes.xl   // 24dp - Dialogs, modals
```

---

## When to Use Each Size

### sm (8dp) - Small Components
**Usage**:
- Chips and badges
- Small buttons
- Input fields
- Toggle switches
- Small icons with backgrounds

**Visual Character**: Subtle rounding, modern but not overly soft

**Example**:
```kotlin
Chip(
    onClick = { },
    shape = Shapes.chip  // 8dp
) {
    Text("Filter")
}
```

### md (12dp) - Default Components **[Most Common]**
**Usage**:
- Standard buttons
- Default cards
- Tab indicators
- Progress bars
- Medium-sized containers

**Visual Character**: Balanced rounding, friendly and approachable

**Example**:
```kotlin
Button(
    onClick = { },
    shape = Shapes.button  // 12dp
) {
    Text("Save")
}
```

### lg (16dp) - Large Components
**Usage**:
- Large cards
- Bottom sheets
- Image containers
- Feature highlights
- Prominent containers

**Visual Character**: Pronounced rounding, soft and polished

**Example**:
```kotlin
Card(
    shape = Shapes.card  // 16dp
) {
    // Large feature content
}
```

### xl (24dp) - Modal Components
**Usage**:
- Dialogs and modals
- Overlays
- Full-screen sheets
- Hero content containers
- Celebration screens

**Visual Character**: Dramatic rounding, playful and engaging

**Example**:
```kotlin
Dialog(
    onDismissRequest = { },
    properties = DialogProperties()
) {
    Surface(
        shape = Shapes.dialog  // 24dp
    ) {
        // Dialog content
    }
}
```

---

## Component Shapes

Pre-defined shapes for specific components ensure consistency:

```kotlin
Shapes.button              // RoundedCornerShape(12.dp)
Shapes.card                // RoundedCornerShape(16.dp)
Shapes.chip                // RoundedCornerShape(8.dp)
Shapes.dialog              // RoundedCornerShape(24.dp)
Shapes.bottomSheet         // RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
Shapes.textButton          // RoundedCornerShape(8.dp)
Shapes.toggleButton        // RoundedCornerShape(12.dp)
Shapes.progressBar         // RoundedCornerShape(8.dp)
Shapes.badge               // CircleShape (fully rounded)
Shapes.avatar              // CircleShape (fully rounded)
```

---

## Specialized Shapes

### Bottom Sheet
Rounded on top corners only for slide-up effect:

```kotlin
Shapes.bottomSheet
// RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
```

**Usage**:
```kotlin
ModalBottomSheet(
    onDismissRequest = { },
    shape = Shapes.bottomSheet
) {
    // Sheet content
}
```

### Fully Rounded (Circular)
Perfect circles for avatars, badges, floating actions:

```kotlin
Shapes.badge    // CircleShape
Shapes.avatar   // CircleShape
```

**Usage**:
```kotlin
// Avatar
Box(
    modifier = Modifier
        .size(48.dp)
        .clip(Shapes.avatar)
        .background(AppColors.Primary.Container)
) {
    Icon(imageVector = Icons.Default.Person)
}

// Badge
Box(
    modifier = Modifier
        .size(20.dp)
        .clip(Shapes.badge)
        .background(AppColors.Semantic.Error.Default)
) {
    Text(
        text = "3",
        color = Color.White,
        fontSize = 10.sp
    )
}
```

---

## Custom Shape Helpers

For special cases, helper functions create custom shapes:

### Top Rounded Only
```kotlin
Shapes.topRounded(cornerRadius: Dp)
// RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
```

**Usage**: Top of grouped lists, expandable headers
```kotlin
Surface(
    shape = Shapes.topRounded(16.dp)
) {
    // Content
}
```

### Bottom Rounded Only
```kotlin
Shapes.bottomRounded(cornerRadius: Dp)
// RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
```

**Usage**: Bottom of grouped lists, footer elements
```kotlin
Surface(
    shape = Shapes.bottomRounded(16.dp)
) {
    // Content
}
```

### Custom Corners
```kotlin
Shapes.custom(
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp
)
```

**Usage**: Asymmetric designs, special layouts
```kotlin
Surface(
    shape = Shapes.custom(
        topLeft = 24.dp,
        topRight = 24.dp,
        bottomRight = 8.dp,
        bottomLeft = 8.dp
    )
) {
    // Content with custom shape
}
```

---

## Shape + Elevation Combinations

Shapes work with Material 3 elevation system:

### Elevated Card (Default)
```kotlin
Card(
    shape = Shapes.card,
    elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp
    )
) {
    // Content
}
```

### Prominent Card
```kotlin
Card(
    shape = Shapes.card,
    elevation = CardDefaults.cardElevation(
        defaultElevation = 8.dp
    )
) {
    // Featured content
}
```

### Flat Card (No Elevation)
```kotlin
Card(
    shape = Shapes.card,
    elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
) {
    // Outlined style
}
```

---

## Shape Patterns by Component Type

### Buttons
```kotlin
// Primary button
PrimaryButton(/* uses Shapes.button: 12dp */)

// Secondary button
SecondaryButton(/* uses Shapes.button: 12dp */)

// Text button
AppTextButton(/* uses Shapes.textButton: 8dp */)

// Icon button
IconButton(/* uses CircleShape */)
```

### Cards
```kotlin
// Standard card
StandardCard(/* uses Shapes.card: 16dp */)

// Elevated card
ElevatedCard(/* uses Shapes.card: 16dp */)

// Outlined card
OutlinedCard(/* uses Shapes.card: 16dp */)

// Stat card
StatCard(/* uses Shapes.card: 16dp */)
```

### Input Fields
```kotlin
OutlinedTextField(
    shape = Shapes.chip  // 8dp for compact appearance
)

TextField(
    shape = Shapes.chip  // 8dp
)
```

### Progress Indicators
```kotlin
LinearProgressIndicator(
    modifier = Modifier.clip(Shapes.progressBar)  // 8dp
)

// Circular progress is naturally round (no shape needed)
```

---

## Usage Examples

### Standard Button
```kotlin
@Composable
fun SaveButton() {
    Button(
        onClick = { },
        shape = Shapes.button,  // 12dp
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save Goal")
    }
}
```

### Card Layout
```kotlin
@Composable
fun StatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,  // 16dp
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Today's Progress")
            AppCircularProgress(progress = 0.75f)
        }
    }
}
```

### Dialog with Rounded Corners
```kotlin
@Composable
fun ConfirmationDialog() {
    AlertDialog(
        onDismissRequest = { },
        shape = Shapes.dialog,  // 24dp
        title = { Text("Delete Goal?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            PrimaryButton(text = "Delete", onClick = { })
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = { })
        }
    )
}
```

### Bottom Sheet
```kotlin
@Composable
fun OptionsBottomSheet() {
    ModalBottomSheet(
        onDismissRequest = { },
        shape = Shapes.bottomSheet  // Top corners: 24dp
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Options", style = MaterialTheme.typography.headlineSmall)
            // Options list
        }
    }
}
```

### Chip with Small Radius
```kotlin
@Composable
fun FilterChip() {
    FilterChip(
        selected = true,
        onClick = { },
        label = { Text("Today") },
        shape = Shapes.chip  // 8dp
    )
}
```

---

## Shape Clipping

Use `.clip()` modifier to apply shapes to any composable:

```kotlin
// Clip image to card shape
Image(
    painter = painterResource(R.drawable.feature_image),
    contentDescription = null,
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(Shapes.card)  // 16dp rounded corners
)

// Clip container with background
Box(
    modifier = Modifier
        .size(100.dp)
        .clip(Shapes.button)  // 12dp
        .background(AppColors.Primary.Container)
) {
    // Content
}
```

---

## Migration Guide

### Before (Hardcoded)
```kotlin
Card(
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("Content")
    }
}

Button(
    onClick = { },
    shape = RoundedCornerShape(12.dp)
) {
    Text("Save")
}
```

### After (Design System)
```kotlin
StandardCard {  // shape = Shapes.card, padding already applied
    Text("Content")
}

PrimaryButton(  // shape = Shapes.button
    text = "Save",
    onClick = { }
)
```

### Common Migrations

| Old Pattern | New Pattern |
|------------|-------------|
| `RoundedCornerShape(8.dp)` | `Shapes.chip` or `Shapes.textButton` |
| `RoundedCornerShape(12.dp)` | `Shapes.button` |
| `RoundedCornerShape(16.dp)` | `Shapes.card` |
| `RoundedCornerShape(24.dp)` | `Shapes.dialog` |
| `CircleShape` | `Shapes.badge` or `Shapes.avatar` |
| `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)` | `Shapes.bottomSheet` |

---

## Best Practices

### DO ✓
- Use component-specific shapes (`Shapes.button`, `Shapes.card`)
- Match shape size to component size (small component = small radius)
- Use larger radii for larger, more prominent components
- Test shapes at different screen sizes
- Use `.clip()` modifier for custom shape applications
- Combine shapes with appropriate elevation

### DON'T ✗
- Hardcode `RoundedCornerShape(X.dp)` directly
- Use large radii (24dp) on small components (looks odd)
- Use small radii (8dp) on large prominent components (looks cheap)
- Mix different radius values arbitrarily
- Over-use circular shapes (reserve for avatars/badges)
- Create asymmetric shapes without design justification

---

## Shape Accessibility

### Touch Targets
Shapes don't affect touch target size, but visual size should be clear:

```kotlin
// ✓ Correct - clear button boundary
Button(
    onClick = { },
    shape = Shapes.button,
    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
) {
    Text("Save")
}

// ✗ Unclear - is this clickable?
Text(
    text = "Save",
    modifier = Modifier
        .clip(Shapes.button)
        .clickable { }  // Unclear that this is a button
)
```

### Visual Consistency
Consistent shapes help users recognize component types:
- Buttons always use 12dp radius
- Cards always use 16dp radius
- Dialogs always use 24dp radius

This creates a **mental model** that improves usability.

---

## Shape + Color Combinations

Shapes look best with appropriate color treatments:

### Elevated Card (Shape + Shadow)
```kotlin
Card(
    shape = Shapes.card,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    // Content
}
```

### Outlined Card (Shape + Border)
```kotlin
OutlinedCard {  // Uses Shapes.card with border
    // Content
}
```

### Filled Container (Shape + Solid Color)
```kotlin
Surface(
    shape = Shapes.button,
    color = AppColors.Primary.Container,
    modifier = Modifier.padding(Spacing.md)
) {
    // Content with colored background
}
```

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Spacing](./Spacing.md)
- [Components](./Components.md)
