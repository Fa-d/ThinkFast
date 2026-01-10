# Intently Design System

> **Material 3 foundation + iOS-inspired polish = Best of both worlds**

## Welcome

This design system provides a comprehensive foundation for building consistent, accessible, and polished user interfaces in the Intently Android app. It combines Material Design 3 principles with iOS-level UX refinement to create an exceptional user experience.

## Design Philosophy

### Core Principles

1. **Consistency** - Use design tokens, never hardcode values
2. **Accessibility** - WCAG AA compliance, reduced motion support, TalkBack optimization
3. **Responsiveness** - 4dp baseline grid, fluid spacing across screen sizes
4. **Polish** - Smooth animations with spring physics, haptic feedback, delightful interactions
5. **Material 3 + iOS Polish** - Material Design foundation with iOS-level UX refinement

### Visual Identity

- **Card-based layouts** with subtle shadows for hierarchy
- **Spring physics animations** (0.4s response, 0.7 damping) for natural motion
- **Semantic color usage** - Green = success, Orange = warning, Red = error
- **Empty states** that guide users to next action
- **Loading states** that prevent uncertainty
- **Haptic feedback** on key interactions

## Quick Start

### 1. Import Design Tokens

```kotlin
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Animation
```

### 2. Use Component Library

```kotlin
import dev.sadakat.thinkfaster.ui.design.components.PrimaryButton
import dev.sadakat.thinkfaster.ui.design.components.StandardCard
```

### 3. Apply Design System

```kotlin
// ✓ Correct
StandardCard {
    Text(
        text = "Title",
        style = MaterialTheme.typography.titleMedium
    )
    PrimaryButton(
        text = "Save",
        onClick = { /* action */ }
    )
}

// ✗ Incorrect
Card(
    shape = RoundedCornerShape(16.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Title", fontSize = 16.sp)
        Button(onClick = { }) { Text("Save") }
    }
}
```

## File Organization

### Design Tokens
**Location**: `/app/src/main/java/dev/sadakat/thinkfaster/ui/design/tokens/`

- `Spacing.kt` - 4dp baseline grid, component-specific spacing
- `Shapes.kt` - Corner radius scale, component shapes
- `Animation.kt` - Duration scales, easing curves, spring specs

### Theme Configuration
**Location**: `/app/src/main/java/dev/sadakat/thinkfaster/ui/theme/`

- `Type.kt` - Complete Material 3 typography scale
- `Color.kt` - Unified AppColors structure
- `Theme.kt` - Material 3 theme configuration

### Component Library
**Location**: `/app/src/main/java/dev/sadakat/thinkfaster/ui/design/components/`

- `Buttons.kt` - PrimaryButton, SecondaryButton, AppTextButton
- `Cards.kt` - StandardCard, StatCard, ElevatedCard, OutlinedCard
- `Progress.kt` - AppLinearProgressBar, AppCircularProgress
- `EmptyState.kt` - EmptyStateView
- `Loading.kt` - LoadingIndicator

### Utilities
**Location**: `/app/src/main/java/dev/sadakat/thinkfaster/ui/design/utils/`

- `AccessibilityUtils.kt` - TalkBack support, contrast checking, reduced motion
- `ResponsiveLayout.kt` - Screen size detection, adaptive layouts

## Documentation

Detailed documentation for each design system component:

- [Typography](./Typography.md) - Complete type scale, semantic naming, usage
- [Colors](./Colors.md) - AppColors structure, semantic colors, gradients
- [Spacing](./Spacing.md) - 4dp grid system, component spacing patterns
- [Shapes](./Shapes.md) - Corner radii, component shapes
- [Animation](./Animation.md) - Motion principles, timing, easing, reduced motion
- [Components](./Components.md) - Component library reference
- [Migration](./Migration.md) - Refactoring guide for existing code

## Design Tokens Summary

### Spacing (4dp baseline)
- `xs: 4dp` - Tiny gaps, icon padding
- `sm: 8dp` - Small gaps, tight spacing
- `md: 16dp` - Default padding, card padding
- `lg: 24dp` - Section spacing
- `xl: 32dp` - Extra large gaps
- `xxl: 48dp` - Hero section spacing

### Shapes (Corner Radius)
- `sm: 8dp` - Buttons, input fields, chips
- `md: 12dp` - Cards, toggle buttons
- `lg: 16dp` - Large cards, bottom sheets
- `xl: 24dp` - Dialogs, modals

### Typography (Material 3)
- Display styles: 57sp, 45sp, 36sp (Hero content)
- Headline styles: 32sp, 28sp, 24sp (Major sections)
- Title styles: 22sp, 16sp, 14sp (Subsections)
- Body styles: 16sp, 14sp, 12sp (Content)
- Label styles: 14sp, 12sp, 11sp (UI elements)

### Colors (Semantic)
- **Primary**: Blue (#2196F3) - Brand, primary actions
- **Secondary**: Purple (#9C27B0) - Accent color
- **Success**: Green (#4CAF50) - Goals met, positive trends
- **Warning**: Orange (#FF9800) - Approaching limits
- **Error**: Red (#F44336) - Limits exceeded, destructive actions
- **Info**: Blue (#2196F3) - Neutral information

## Migration Path

Transitioning from old patterns to the new design system:

1. **Replace hardcoded spacing**: `16.dp` → `Spacing.md`
2. **Replace hardcoded shapes**: `RoundedCornerShape(12.dp)` → `Shapes.button`
3. **Use typography**: `fontSize = 16.sp` → `style = MaterialTheme.typography.bodyLarge`
4. **Adopt components**: Custom buttons → `PrimaryButton`/`SecondaryButton`
5. **Consolidate colors**: `PrimaryColors.Blue500` → `AppColors.Primary.Default`

See [Migration.md](./Migration.md) for detailed step-by-step instructions.

## Success Metrics

A well-implemented design system achieves:

- ✅ **0 hardcoded spacing values** - All use `Spacing.*`
- ✅ **0 hardcoded shapes** - All use `Shapes.*`
- ✅ **100% typography** - All use `MaterialTheme.typography.*`
- ✅ **Component library adoption** - All buttons/cards use library
- ✅ **Consistent UX** - iOS polish level with Material 3 design

## Best Practices

### DO ✓
- Use design tokens for all spacing, shapes, colors
- Use component library for common patterns
- Follow Material 3 typography scale
- Test with TalkBack and large fonts
- Verify dark mode appearance
- Profile animations for performance

### DON'T ✗
- Hardcode dimensions (`16.dp`, `RoundedCornerShape(12.dp)`)
- Create custom buttons/cards without using library
- Use inline font sizes (`fontSize = 16.sp`)
- Skip accessibility testing
- Ignore reduced motion preferences
- Add animations without performance testing

## Support

For questions or issues with the design system:

1. **First**: Check the relevant documentation page
2. **Second**: Review migration guide for refactoring patterns
3. **Third**: Examine component library source code for examples
4. **Last**: Consult with the development team

---

**Version**: 1.0
**Last Updated**: 2026-01-04
**Maintained By**: Intently Development Team
