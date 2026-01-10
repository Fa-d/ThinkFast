# Migration Guide

This guide helps you migrate existing code to the Intently design system. Follow these steps to refactor components, screens, and patterns systematically.

---

## Quick Wins (High Impact, Low Effort)

Start with these simple replacements that immediately improve consistency:

### 1. Replace Hardcoded Spacing

**Find**: `padding(16.dp)`, `padding(20.dp)`, `Arrangement.spacedBy(8.dp)`
**Replace with**: `Spacing.*` tokens

```kotlin
// Before
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    // Content
}

// After
Column(
    modifier = Modifier.padding(Spacing.md),
    verticalArrangement = Spacing.verticalArrangementLG
) {
    // Content
}
```

### 2. Replace Hardcoded Shapes

**Find**: `RoundedCornerShape(12.dp)`, `RoundedCornerShape(16.dp)`
**Replace with**: `Shapes.*` tokens

```kotlin
// Before
Card(shape = RoundedCornerShape(16.dp)) { }

// After
Card(shape = Shapes.card) { }
```

### 3. Use Typography Scale

**Find**: `fontSize = 16.sp`, `fontSize = 18.sp, fontWeight = FontWeight.Bold`
**Replace with**: `style = MaterialTheme.typography.*`

```kotlin
// Before
Text(
    text = "Title",
    fontSize = 18.sp,
    fontWeight = FontWeight.Bold
)

// After
Text(
    text = "Title",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold
)
```

---

## Step-by-Step Migration Process

### Step 1: Update Imports

Add design token and component library imports:

```kotlin
// Design tokens
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Animation

// Component library
import dev.sadakat.thinkfaster.ui.design.components.PrimaryButton
import dev.sadakat.thinkfaster.ui.design.components.SecondaryButton
import dev.sadakat.thinkfaster.ui.design.components.StandardCard
import dev.sadakat.thinkfaster.ui.design.components.AppCircularProgress

// Colors (after consolidation)
import dev.sadakat.thinkfaster.ui.theme.AppColors
```

### Step 2: Find/Replace Hardcoded Values

Use your IDE's find/replace to systematically update common patterns:

#### Spacing Replacements

| Find | Replace With |
|------|-------------|
| `padding(4.dp)` | `padding(Spacing.xs)` |
| `padding(8.dp)` | `padding(Spacing.sm)` |
| `padding(16.dp)` | `padding(Spacing.md)` |
| `padding(20.dp)` | `padding(Spacing.lg)` |
| `padding(24.dp)` | `padding(Spacing.lg)` |
| `padding(32.dp)` | `padding(Spacing.xl)` |
| `Arrangement.spacedBy(8.dp)` | `Spacing.verticalArrangementSM` (or `horizontalArrangementSM`) |
| `Arrangement.spacedBy(16.dp)` | `Spacing.verticalArrangementMD` |
| `Arrangement.spacedBy(24.dp)` | `Spacing.verticalArrangementLG` |

#### Shape Replacements

| Find | Replace With |
|------|-------------|
| `RoundedCornerShape(8.dp)` | `Shapes.chip` |
| `RoundedCornerShape(12.dp)` | `Shapes.button` |
| `RoundedCornerShape(16.dp)` | `Shapes.card` |
| `RoundedCornerShape(24.dp)` | `Shapes.dialog` |
| `CircleShape` | `Shapes.badge` or `Shapes.avatar` |

#### Typography Replacements

| Find | Replace With |
|------|-------------|
| `fontSize = 12.sp` | `style = MaterialTheme.typography.bodySmall` |
| `fontSize = 14.sp` | `style = MaterialTheme.typography.bodyMedium` |
| `fontSize = 16.sp` | `style = MaterialTheme.typography.bodyLarge` |
| `fontSize = 18.sp, fontWeight = Bold` | `style = MaterialTheme.typography.titleMedium, fontWeight = Bold` |
| `fontSize = 22.sp` | `style = MaterialTheme.typography.titleLarge` |
| `fontSize = 24.sp` | `style = MaterialTheme.typography.headlineSmall` |

### Step 3: Adopt Component Library

Replace custom implementations with component library:

#### Buttons

```kotlin
// Before
Button(
    onClick = { },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

// After
PrimaryButton(
    text = "Save",
    onClick = { },
    modifier = Modifier.fillMaxWidth()
)
```

#### Cards

```kotlin
// Before
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Title", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Description", fontSize = 14.sp)
    }
}

// After
StandardCard(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Title",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Description",
        style = MaterialTheme.typography.bodyMedium
    )
}
```

#### Progress Indicators

```kotlin
// Before
val usagePercent = (currentUsage / dailyLimit * 100).toInt()
val progressColor = when {
    usagePercent <= 75 -> Color.Green
    usagePercent <= 100 -> Color.Orange
    else -> Color.Red
}

LinearProgressIndicator(
    progress = currentUsage / dailyLimit,
    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
    color = progressColor
)

// After
val usagePercent = (currentUsage / dailyLimit * 100).toInt()

AppLinearProgressBar(
    progress = currentUsage / dailyLimit,
    percentageUsed = usagePercent,
    showLabel = true
)
```

### Step 4: Update Colors

```kotlin
// Before
import dev.sadakat.thinkfaster.ui.theme.PrimaryColors
import dev.sadakat.thinkfaster.ui.theme.SemanticColors

val color = PrimaryColors.Blue500
val successColor = SemanticColors.Success

// After
import dev.sadakat.thinkfaster.ui.theme.AppColors

val color = AppColors.Primary.Default
val successColor = AppColors.Semantic.Success.Default
```

---

## Component-by-Component Migration Examples

### Example 1: StreakFreezeCard.kt

#### Before
```kotlin
@Composable
fun StreakFreezeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ProgressColors.Info.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = ProgressColors.Info
                )
                Text(
                    text = "Streak Freeze",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Protect your streak from being broken",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
```

#### After
```kotlin
@Composable
fun StreakFreezeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,  // ✓ Design token
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Semantic.Info.Default.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),  // ✓ Design token (24dp closest to 20dp)
            verticalArrangement = Spacing.verticalArrangementMD  // ✓ Design token
        ) {
            Row(
                horizontalArrangement = Spacing.horizontalArrangementSM,  // ✓ Design token
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = AppColors.Semantic.Info.Default
                )
                Text(
                    text = "Streak Freeze",
                    style = MaterialTheme.typography.titleMedium,  // ✓ Typography
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Protect your streak from being broken",
                style = MaterialTheme.typography.bodyMedium,  // ✓ Typography
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Example 2: HomeScreen.kt

#### Before
```kotlin
@Composable
fun HomeScreen() {
    Scaffold { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Today's Progress",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Usage", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = 0.75f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
```

#### After
```kotlin
@Composable
fun HomeScreen() {
    Scaffold { paddingValues ->
        LazyColumn(
            contentPadding = Spacing.all(Spacing.md),  // ✓ Design token
            verticalArrangement = Spacing.verticalArrangementLG  // ✓ Design token
        ) {
            item {
                Text(
                    text = "Today's Progress",
                    style = MaterialTheme.typography.headlineSmall,  // ✓ Typography
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                StandardCard {  // ✓ Component library (padding & shape applied)
                    Text(
                        text = "Usage",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AppLinearProgressBar(  // ✓ Component library
                        progress = 0.75f,
                        percentageUsed = 75
                    )
                }
            }
        }
    }
}
```

---

## Screen Refactoring Pattern

For each screen file, follow this checklist:

- [ ] Add design token imports (`Spacing`, `Shapes`, `Animation`)
- [ ] Add component library imports (`PrimaryButton`, `StandardCard`, etc.)
- [ ] Replace `LazyColumn`/`Column` contentPadding with `Spacing.all(Spacing.md)`
- [ ] Replace `verticalArrangement` with `Spacing.verticalArrangement*`
- [ ] Replace custom Card implementations with `StandardCard`
- [ ] Replace custom Button implementations with `PrimaryButton`/`SecondaryButton`
- [ ] Update all text styles to use `MaterialTheme.typography`
- [ ] Test screen layout on different screen sizes
- [ ] Verify scroll behavior
- [ ] Test animations and interactions
- [ ] Run accessibility verification (TalkBack, large fonts)

---

## Testing After Migration

### 1. Visual Regression Testing
- Screenshot before/after refactoring
- Compare layouts side-by-side
- Check spacing, shapes, colors

### 2. Functional Testing
- All interactions work (buttons, inputs, etc.)
- Navigation flows correctly
- Data displays properly

### 3. Accessibility Testing
- TalkBack reads content correctly
- Large font sizes work
- Reduced motion is respected
- Contrast ratios meet WCAG AA

### 4. Performance Testing
- No performance regression
- Animations are smooth
- Scroll performance unchanged

---

## Common Pitfalls & Solutions

### Pitfall 1: Spacing Doesn't Match Exactly
**Problem**: Old code uses `20.dp`, but design system has `16.dp` (md) or `24.dp` (lg)

**Solution**: Choose closest value. Usually `20.dp` → `Spacing.lg` (24dp) is fine. Visual difference is minimal.

### Pitfall 2: Too Many Imports
**Problem**: Many import statements clutter the file

**Solution**: Use import aliases or group imports:
```kotlin
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing as DS_Spacing
```

### Pitfall 3: Breaking Existing Layouts
**Problem**: Changing spacing breaks carefully balanced layouts

**Solution**:
1. Test incrementally
2. Keep screenshots of before state
3. Adjust one section at a time
4. Get design review if unsure

### Pitfall 4: Color Migration Errors
**Problem**: Old color objects still referenced, causing compilation errors

**Solution**: Use IDE's "Find Usages" to locate all references:
```kotlin
// Find all usages of PrimaryColors.Blue500
// Replace with AppColors.Primary.Default
```

---

## Verification Checklist

After migrating a file, verify:

- [ ] **No hardcoded spacing** (search for `.dp` not from tokens)
- [ ] **No hardcoded shapes** (search for `RoundedCornerShape(` not from Shapes)
- [ ] **Typography uses MaterialTheme** (no `fontSize = X.sp` except in special cases)
- [ ] **Buttons use component library** (PrimaryButton, SecondaryButton, AppTextButton)
- [ ] **Cards use component library** (StandardCard, ElevatedCard, OutlinedCard)
- [ ] **Colors use AppColors or MaterialTheme.colorScheme**
- [ ] **Animations respect reduced motion**
- [ ] **File compiles without errors**
- [ ] **Visual appearance is consistent**
- [ ] **Interactions work correctly**
- [ ] **Accessibility features work (TalkBack, large fonts)**

---

## Gradual Migration Strategy

**Option 1: File-by-File**
- Pick one component file
- Fully migrate it
- Test thoroughly
- Move to next file

**Option 2: Pattern-by-Pattern**
- Replace all spacing first
- Then all shapes
- Then all typography
- Finally adopt component library

**Option 3: Screen-by-Screen**
- Start with less critical screens
- Gain confidence
- Move to main screens (Home, Settings)
- Finish with complex screens

**Recommended**: Option 1 (File-by-File) for thoroughness

---

## Getting Help

If you encounter issues during migration:

1. **Check Documentation**: Review [DesignSystem.md](./DesignSystem.md) and specific guides
2. **Look at Examples**: See refactored components as templates
3. **Use Component Library Source**: Read `Buttons.kt`, `Cards.kt` for patterns
4. **Search Codebase**: Find similar patterns already migrated
5. **Ask Team**: Consult with design system maintainers

---

## Success Metrics

Track migration progress:

- **Files Migrated**: X / Y total files
- **Hardcoded Values Remaining**: 0 (goal)
- **Component Library Adoption**: 100% for buttons/cards
- **Typography Compliance**: 100%
- **Accessibility Pass Rate**: 100%

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Typography](./Typography.md)
- [Colors](./Colors.md)
- [Spacing](./Spacing.md)
- [Shapes](./Shapes.md)
- [Animation](./Animation.md)
- [Components](./Components.md)
