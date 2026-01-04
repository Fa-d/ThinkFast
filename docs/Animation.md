# Animation System

The ThinkFast animation system provides smooth, natural motion with Material 3 easing curves and iOS-inspired spring physics. All animations respect user accessibility preferences.

## Motion Principles

1. **Purposeful** - Every animation serves a function (feedback, transition, hierarchy)
2. **Natural** - Spring physics create organic, realistic motion
3. **Responsive** - Fast feedback for interactions, deliberate pacing for transitions
4. **Accessible** - Respects reduced motion preferences

---

## Duration Scale

Pre-defined durations for consistent timing:

```kotlin
Animation.Duration.Instant  //  50ms - Instant feedback (tooltip show/hide)
Animation.Duration.Fast     // 200ms - Button presses, toggles, quick transitions
Animation.Duration.Normal   // 300ms - Screen transitions, fades, standard animations
Animation.Duration.Slow     // 500ms - Complex animations, layout changes
Animation.Duration.Slower   // 800ms - Celebrations, special effects, emphasis
```

### When to Use Each Duration

#### Instant (50ms)
- Tooltip appearance
- Immediate visual feedback
- Micro-interactions

**Example**:
```kotlin
animateFloatAsState(
    targetValue = if (selected) 1f else 0f,
    animationSpec = tween(durationMillis = Animation.Duration.Instant)
)
```

#### Fast (200ms) **[Most Common for Interactions]**
- Button presses
- Toggle switches
- Tab selections
- Hover states
- Selection changes

**Example**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1.0f,
    animationSpec = tween(
        durationMillis = Animation.Duration.Fast,
        easing = Animation.Easing.Standard
    )
)
```

#### Normal (300ms) **[Most Common for Transitions]**
- Fade in/out
- Screen transitions
- Modal appearance
- Content expansion
- Card reveals

**Example**:
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(Animation.Duration.Normal)),
    exit = fadeOut(animationSpec = tween(Animation.Duration.Normal))
) {
    // Content
}
```

#### Slow (500ms)
- Complex layout changes
- Multi-step animations
- Progress updates
- Number count-ups

**Example**:
```kotlin
val progress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = tween(
        durationMillis = Animation.Duration.Slow,
        easing = Animation.Easing.Standard
    )
)
```

#### Slower (800ms)
- Celebration animations
- Achievement reveals
- Special effects
- Emphasis moments

**Example**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (celebrating) 1.2f else 1.0f,
    animationSpec = tween(
        durationMillis = Animation.Duration.Slower,
        easing = Animation.Easing.Emphasized
    )
)
```

---

## Easing Curves

Material 3 easing curves for different motion types:

```kotlin
Animation.Easing.Standard    // FastOutSlowInEasing - Default, balanced
Animation.Easing.Emphasized  // CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f) - Dramatic enter/exit
Animation.Easing.Linear      // LinearEasing - Constant speed (progress bars)
```

### Standard (FastOutSlowIn)
**Usage**: Most animations - smooth acceleration and deceleration

**Character**: Natural, balanced motion

**Example**:
```kotlin
animateFloatAsState(
    targetValue = target,
    animationSpec = tween(
        durationMillis = Animation.Duration.Normal,
        easing = Animation.Easing.Standard
    )
)
```

### Emphasized (Custom Cubic Bezier)
**Usage**: Enter/exit animations, dramatic reveals

**Character**: Starts slow, ends dramatically

**Example**:
```kotlin
slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(
        durationMillis = Animation.Duration.Normal,
        easing = Animation.Easing.Emphasized
    )
)
```

### Linear
**Usage**: Progress indicators, continuous animations

**Character**: Constant speed, no easing

**Example**:
```kotlin
infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 1000,
        easing = Animation.Easing.Linear
    )
)
```

---

## Spring Physics (iOS-Inspired)

Natural, physics-based animations with bounce:

```kotlin
Animation.Spring.Default     // dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium
Animation.Spring.Bouncy      // dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium
Animation.Spring.Smooth      // dampingRatio = 1.0f, stiffness = Spring.StiffnessLow
```

### Default Spring
**Usage**: Button presses, scale animations, general interactions

**Character**: Slight bounce, responsive

**Parameters**:
- Damping: 0.8 (minimal bounce)
- Stiffness: Medium (responsive)

**Example**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1.0f,
    animationSpec = Animation.Spring.Default,
    label = "button_press"
)

Button(
    modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
) {
    Text("Press Me")
}
```

### Bouncy Spring
**Usage**: Celebrations, achievements, playful interactions

**Character**: Pronounced bounce, playful

**Parameters**:
- Damping: 0.5 (more bounce)
- Stiffness: Medium

**Example**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (celebrating) 1.2f else 1.0f,
    animationSpec = Animation.Spring.Bouncy,
    label = "celebration"
)
```

### Smooth Spring
**Usage**: Smooth transitions, no bounce desired

**Character**: Smooth settle, no bounce

**Parameters**:
- Damping: 1.0 (critically damped, no overshoot)
- Stiffness: Low (slower, smoother)

**Example**:
```kotlin
val offset by animateDpAsState(
    targetValue = if (expanded) 0.dp else 100.dp,
    animationSpec = Animation.Spring.Smooth
)
```

---

## Pre-defined Animation Specs

Commonly used animation specifications:

### Fade Animations
```kotlin
Animation.FadeInSpec        // fadeIn(tween(300ms, Standard))
Animation.FadeOutSpec       // fadeOut(tween(300ms, Standard))
```

**Usage**:
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = Animation.FadeInSpec,
    exit = Animation.FadeOutSpec
) {
    Content()
}
```

### Button Press
```kotlin
Animation.ButtonPressSpec   // spring(dampingRatio = 0.8f, stiffness = 800f)
```

**Usage**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1.0f,
    animationSpec = Animation.ButtonPressSpec
)
```

### Progress Update
```kotlin
Animation.ProgressUpdateSpec  // tween(500ms, Standard)
```

**Usage**:
```kotlin
val progress by animateFloatAsState(
    targetValue = newProgress,
    animationSpec = Animation.ProgressUpdateSpec
)
```

### Celebration Bounce
```kotlin
Animation.CelebrationBounceSpec  // spring(dampingRatio = 0.5f)
```

**Usage**:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (achieved) 1.15f else 1.0f,
    animationSpec = Animation.CelebrationBounceSpec
)
```

---

## Scale Animations (iOS Pattern)

iOS-style press animations (scale down on press):

```kotlin
Animation.Scale.PressDown   // 0.95f - Scale when pressed
Animation.Scale.PressUp     // 1.0f - Normal scale
```

**Usage**:
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) Animation.Scale.PressDown else Animation.Scale.PressUp,
    animationSpec = Animation.ButtonPressSpec
)

Button(
    onClick = { },
    interactionSource = interactionSource,
    modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
) {
    Text("Press Me")
}
```

---

## Reduced Motion Support

**Critical**: Always respect user accessibility preferences!

```kotlin
// Check if reduced motion is enabled
val reducedMotionEnabled = AccessibilityUtils.isReducedMotionEnabled()

// Get appropriate animation spec
val animSpec = Animation.ReducedMotion.spec(
    reducedMotion = reducedMotionEnabled,
    normalSpec = Animation.Spring.Default
)

// Use the spec
val scale by animateFloatAsState(
    targetValue = target,
    animationSpec = animSpec
)
```

### What Reduced Motion Does

When enabled:
- **Durations**: Cut to 50ms (nearly instant)
- **Springs**: Converted to instant snaps
- **Fades**: Still allowed (accessible)
- **Complex animations**: Simplified or disabled

**Example**:
```kotlin
@Composable
fun AnimatedContent() {
    val reducedMotion = AccessibilityUtils.isReducedMotionEnabled()

    if (reducedMotion) {
        // Simple, instant transitions
        if (isVisible) {
            Content()
        }
    } else {
        // Full animation
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Content()
        }
    }
}
```

---

## Common Animation Patterns

### Button Press (Scale Down)
```kotlin
@Composable
fun AnimatedButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = Animation.ButtonPressSpec
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Text("Press Me")
    }
}
```

### Fade In on Appear
```kotlin
@Composable
fun FadeInContent() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = Animation.FadeInSpec
    ) {
        Content()
    }
}
```

### Progress Animation
```kotlin
@Composable
fun AnimatedProgress(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = Animation.ProgressUpdateSpec
    )

    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Expandable Card
```kotlin
@Composable
fun ExpandableCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded }
    ) {
        Column {
            CardHeader()

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CardDetails()
            }
        }
    }
}
```

### Stagger Animation (List Items)
```kotlin
@Composable
fun StaggeredList(items: List<Item>) {
    LazyColumn {
        itemsIndexed(items) { index, item ->
            val animatedModifier = Modifier.animateEnterExit(
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = Animation.Duration.Normal,
                        delayMillis = index * 50  // Stagger by 50ms
                    )
                )
            )

            ItemCard(
                item = item,
                modifier = animatedModifier
            )
        }
    }
}
```

---

## Haptic Feedback with Animations

Combine animations with haptics for iOS-level polish:

```kotlin
@Composable
fun HapticButton(onClick: () -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            // Haptic feedback on press
            HapticFeedback.selection(context)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = Animation.ButtonPressSpec
    )

    Button(
        onClick = {
            HapticFeedback.success(context)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Text("Save")
    }
}
```

---

## Performance Considerations

### DO ✓
- Use `animateFloat`/`animateDp`/etc. AsState for simple property animations
- Use `updateTransition` for multi-property coordinated animations
- Profile animations with Layout Inspector
- Test on low-end devices
- Use `graphicsLayer` for scale/rotation/translation (hardware accelerated)
- Respect reduced motion preferences

### DON'T ✗
- Animate `layout()` modifiers (triggers recomposition)
- Run heavy animations on main thread
- Nest too many animations (visual noise)
- Ignore reduced motion settings
- Animate large lists without virtualization
- Use `alpha` for visibility (use `AnimatedVisibility` instead)

---

## Accessibility Guidelines

### Reduced Motion Compliance
```kotlin
// ✓ Correct - respects reduced motion
val animSpec = Animation.ReducedMotion.spec(
    reducedMotion = AccessibilityUtils.isReducedMotionEnabled(),
    normalSpec = Animation.Spring.Default
)

// ✗ Incorrect - ignores user preference
val animSpec = spring<Float>(dampingRatio = 0.5f)
```

### Essential vs Decorative
- **Essential animations** (show/hide content): Simplify but don't remove
- **Decorative animations** (bounces, flourishes): Disable completely

### Testing
- Test all animations with reduced motion ON
- Verify content is still accessible
- Ensure no critical information is animation-dependent

---

## Migration Guide

### Before (Hardcoded)
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1.0f,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
)

AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(animationSpec = tween(300))
) {
    Content()
}
```

### After (Design System)
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) Animation.Scale.PressDown else Animation.Scale.PressUp,
    animationSpec = Animation.ButtonPressSpec
)

AnimatedVisibility(
    visible = isVisible,
    enter = Animation.FadeInSpec,
    exit = Animation.FadeOutSpec
) {
    Content()
}
```

---

## Best Practices

### DO ✓
- Use pre-defined animation specs for consistency
- Respect reduced motion preferences
- Match animation duration to interaction type (fast for buttons, normal for transitions)
- Use spring physics for interactive elements
- Test animations on real devices
- Profile performance for complex animations

### DON'T ✗
- Hardcode animation durations or easing curves
- Ignore reduced motion accessibility setting
- Use overly long animations (>800ms is rarely justified)
- Animate too many things at once (visual chaos)
- Use animations for critical user feedback only (provide alternatives)
- Assume all devices can handle complex animations

---

**Related Documentation**:
- [Design System Overview](./DesignSystem.md)
- [Components](./Components.md)
- [Accessibility Utils](../app/src/main/java/dev/sadakat/thinkfaster/ui/design/utils/AccessibilityUtils.kt)
