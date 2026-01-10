# Phase 1: UX Foundation

**Goal:** Make the app SO good that users can't imagine living without it.

**Duration:** 2-3 weeks

**Success Criteria:**
- [ ] 10 beta users complete 1 week of usage
- [ ] 70%+ say "I'd be sad if this app disappeared"
- [ ] Users open app 4+ times per week
- [ ] Users set up goals within first 3 days

---

## 1.1 Onboarding Overhaul

### Current State
- Users land on HomeScreen immediately
- Permissions requested without context
- No guidance on goal setting
- No sample data shown

### Target State
- 3-screen onboarding flow explaining value
- Permission requests with "WHY" explanations
- Suggested goals based on research
- Sample charts visible before user has data

### Tasks
- [ ] Design onboarding screens (Figma/sketch)
  - Screen 1: Welcome + Value Prop ("Take control of your screen time")
  - Screen 2: Permission explanation ("Why we need these permissions")
  - Screen 3: Goal setup ("Let's set your first goal")
- [ ] Implement OnboardingScreen.kt composable
- [ ] Add "show only once" logic (SharedPreferences)
- [ ] Create sample data generator for chart preview
- [ ] Test onboarding flow with 3-5 users

**Priority:** 🔴 Critical

**Estimated Time:** 3-4 days

**Files to Create:**
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/onboarding/OnboardingScreen.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/onboarding/OnboardingViewModel.kt`

---

## 1.2 Home Screen Improvements

### Current State
- Service status card
- Start/stop button
- Basic information
- No at-a-glance insights

### Target State
- "Today at a Glance" card showing:
  - Current usage vs goal
  - Streak count prominently (🔥 7 days)
  - Quick progress bar
  - Celebration for wins
- Quick actions (jump to stats, adjust goals)
- More visual, less text

### Tasks
- [ ] Design new HomeScreen layout
- [ ] Create "TodayAtAGlance" composable card
- [ ] Add streak counter with emoji (🔥)
- [ ] Implement progress bar (visual usage/goal)
- [ ] Add celebration messages ("18 min under goal! 🎉")
- [ ] Add quick action buttons
- [ ] Ensure it loads fast (<500ms)

**Priority:** 🔴 Critical

**Estimated Time:** 2-3 days

**Files to Modify:**
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/home/HomeScreen.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/home/HomeViewModel.kt`

---

## 1.3 Intervention Overlay Enhancement (THE CORE)

**⚠️ CRITICAL:** This is THE most important feature in Intently. The intervention overlays (reminder + timer) are where behavior change happens. Users interact with these 5-20 times per day. Poor interventions = uninstall. Great interventions = retention + word-of-mouth growth.

**📖 Full Strategy:** See `/docs/INTERVENTION_OVERLAY_STRATEGY.md` for complete research and implementation plan.

### Current State
**Reminder Overlay (Launch Intervention):**
- Static message: [DEFAULT_REMINDER_MESSAGE]
- Generic subtext: "Take a moment to consider..."
- Single "Proceed" button (easy to dismiss)
- No variety, no personalization, no emotional engagement

**Timer Overlay (10-Minute Alert):**
- Shows session statistics (good!)
- Static message: "You've been using this app for 10 minutes continuously..."
- No loss framing or alternatives
- "I Understand" button (easy to dismiss)

**Problems:**
- Users habituate to repetitive messages (ignore them)
- No emotional engagement (just a barrier, not meaningful)
- No alternatives suggested (what SHOULD they do instead?)
- No context awareness (same message at 2 AM as at 2 PM)
- No progressive difficulty (too easy to dismiss)

### Target State

**8 Content Strategies (Weighted Randomization):**
1. **Reflection Questions (40%)** - "What are you trying to avoid right now?"
2. **Time Alternatives (30%)** - "This 10 min could have been a 1km run"
3. **Breathing Exercise (20%)** - 4-7-8 breathing with animation
4. **Usage Statistics (10%)** - "You're down 26 min from yesterday!"
5. **Emotional Appeals (Selective)** - Late night: "Tomorrow-you will regret this"
6. **Alternative Suggestions** - Time-based: morning walk, evening reading
7. **Quotes & Inspiration (Rare)** - Quality over quantity
8. **Gamification** - "Beat your record: Stop now at 8 min!"

**Dynamic Features:**
- 50+ unique intervention messages (prevent habituation)
- Context-aware content (time of day, session count, rapid reopens)
- Progressive friction (gentle → moderate → firm over weeks)
- Loss framing ("This time could have been...")
- Celebration when user chooses "Go Back"

**Research-Backed Effectiveness:**
- One Sec app: **36% dismissal rate** (users chose NOT to open app)
- Session duration reduction: **37% over 6 weeks**
- Strategic friction increases self-control

### Implementation Phases

**Phase A: Content Foundation (Week 1) - PRIORITY 🔴**
- [ ] Create `InterventionContent.kt` model (sealed classes)
- [ ] Build content pools:
  - [ ] 20+ reflection questions
  - [ ] Time alternatives (2min, 5min, 10min, 20min)
  - [ ] 3 breathing variants
  - [ ] Usage stats templates
- [ ] Implement weighted randomization selector
- [ ] Track last shown content (prevent repeats)
- [ ] Test with 5 users for variety

**Phase B: Visual Enhancement (Week 1-2) - PRIORITY 🔴**
- [ ] Define `InterventionColors` (amber/blue/green/red by type)
- [ ] Update overlay backgrounds based on content type
- [ ] Improve typography (serif for reflection, monospace for stats)
- [ ] Add entrance animations (fade + scale)
- [ ] Add button press micro-interactions
- [ ] Test emotional impact with users

**Phase C: Breathing Exercise (Week 2) - PRIORITY 🟡**
- [ ] Create `BreathingExercise.kt` composable
- [ ] Implement 4-7-8 breathing animation
- [ ] Expanding/contracting circle visual
- [ ] Phase labels ("Breathe In", "Hold", "Breathe Out")
- [ ] Integrate with content selector (20% weight)

**Phase D: Timer Overlay Enhancement (Week 2-3) - PRIORITY 🔴**
- [ ] Add loss framing: "This 12 min could have been..."
- [ ] Dynamic time alternatives calculation
- [ ] Add "I'll Do Something Better" button (track choice)
- [ ] Pulsing animation for 15+ min sessions
- [ ] Celebration effect when user stops early

**Phase E: Context Awareness (Week 3) - PRIORITY 🟡**
- [ ] Implement context detection:
  - [ ] Time of day (0-23)
  - [ ] Session count today
  - [ ] Rapid reopen detection (< 2 min since last close)
  - [ ] Day of week (weekend vs weekday)
- [ ] Special messages for:
  - [ ] Late night (22:00-05:00) - "Sleep is more valuable"
  - [ ] Weekend mornings - "Is this how you want to spend your Saturday?"
  - [ ] Rapid reopens - "You've opened this 7 times. What are you looking for?"
  - [ ] Extended sessions (15+ min) - Escalated urgency

**Phase F: Progressive Friction (Week 3-4) - PRIORITY 🟡**
- [ ] Create `FrictionLevel` enum (Gentle/Moderate/Firm/Locked)
- [ ] Track user install date and usage patterns
- [ ] Implement delayed button appearance:
  - [ ] Week 1-2: 0 second delay
  - [ ] Week 3-4: 3 second delay + countdown
  - [ ] Week 5+: 5 second delay + required interaction
  - [ ] User-requested Locked Mode: 10 second + multi-step
- [ ] Store friction level in user profile

**Phase G: Effectiveness Tracking (Week 4) - PRIORITY 🟢**
- [ ] Create Room database table `InterventionResult`
- [ ] Track: content type, user proceeded (yes/no), session duration after
- [ ] Calculate effectiveness by content type
- [ ] Build debug screen to view analytics
- [ ] Adjust content weights based on data

### Success Metrics

**Primary (The Only Ones That Matter):**
- [ ] **Dismissal Rate:** 30%+ choose "Go Back" instead of "Proceed"
- [ ] **Session Reduction:** 25%+ shorter sessions after intervention
- [ ] **Launch Reduction:** 30%+ fewer app launch attempts over 4 weeks

**Secondary:**
- [ ] **Engagement Time:** 8+ seconds average (indicates genuine consideration)
- [ ] **Content Effectiveness:** Identify top 3 performing content types
- [ ] **User Sentiment:** >70% describe as "helpful" not "annoying"

### Testing Questions for Beta Users

**Week 1:**
1. "Do the intervention screens feel repetitive or fresh?"
2. "Which message made you stop and really think?"
3. "Have you ever chosen NOT to proceed because of the overlay?"

**Week 2:**
4. "Have you noticed a change in how often you open Facebook/Instagram?"
5. "Do you find yourself pausing before opening these apps now?"

**Week 4:**
6. "Would you keep using Intently if it cost $3/month?"
7. "Have you recommended Intently to anyone? Why or why not?"

### Files to Create
- `/app/src/main/java/dev/sadakat/thinkfast/domain/model/InterventionContent.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/domain/intervention/ContentSelector.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/domain/intervention/InterventionContext.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/domain/intervention/FrictionLevelManager.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/overlay/components/BreathingExercise.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/data/local/entity/InterventionResultEntity.kt`

### Files to Modify
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/overlay/ReminderOverlayActivity.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/overlay/TimerOverlayActivity.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/ui/theme/Color.kt`

**Estimated Time:** 3-4 weeks (can be done in parallel with other tasks)

**Priority:** 🔴 CRITICAL - This is THE core of Intently's value proposition

---

## 1.4 Visual Polish

### Current State
- Light mode only
- Basic Material 3 theming
- No custom animations
- Static UI

### Target State
- Dark mode support (essential for wellbeing app)
- Smooth animations for transitions
- Haptic feedback for achievements
- Consistent color language:
  - Green = on track
  - Yellow = approaching limit
  - Red = over limit
- Delightful micro-interactions

### Tasks
- [ ] Implement dark mode theme
- [ ] Add theme toggle in settings
- [ ] Create custom animations for:
  - Chart loading
  - Achievement unlocks
  - Streak milestones
  - Goal completion
- [ ] Add haptic feedback (vibration) for:
  - Streak milestones
  - Goal achieved
  - Limit exceeded
- [ ] Define color palette for progress states
- [ ] Polish card elevations and shadows
- [ ] Ensure 60fps scrolling performance

**Priority:** 🟡 High

**Estimated Time:** 2-3 days

**Files to Modify:**
- `/app/src/main/java/dev/sadakat/thinkfast/ui/theme/Theme.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/ui/theme/Color.kt`

---

## 1.5 Quick Wins (Dopamine Hits)

### Current State
- Stats screen shows data
- No celebrations or achievements
- Streak tracking exists but not prominent

### Target State
- Daily achievement notifications:
  - "Under goal today! 🎉"
  - "First time under 60 min! 🌟"
- Streak milestones:
  - 3 days, 7 days, 14 days, 30 days
  - Celebratory UI when milestones hit
- First-time celebrations:
  - "First session under 10 min!"
  - "First week completed!"
- In-app confetti or animation for big wins

### Tasks
- [ ] Design achievement notification format
- [ ] Implement achievement detection logic
- [ ] Create celebration animations/effects
- [ ] Add confetti library (optional, lightweight)
- [ ] Track achievement history
- [ ] Test with beta users (does it feel good?)

**Priority:** 🟢 Medium

**Estimated Time:** 2 days

**Files to Create:**
- `/app/src/main/java/dev/sadakat/thinkfast/domain/usecase/achievements/CheckAchievementsUseCase.kt`
- `/app/src/main/java/dev/sadakat/thinkfast/presentation/components/CelebrationAnimation.kt`

---

## Testing & Validation

### Beta Testing Plan
- [ ] Recruit 10 beta testers
  - 3 from friends/family (biased but helpful)
  - 7 from Reddit (r/nosurf, r/digitalminimalism)
- [ ] Create Google Form for feedback
- [ ] Send weekly check-in emails
- [ ] Track usage in Firebase Analytics (optional)

### Questions to Ask Beta Users (Week 1)
1. On a scale of 1-10, how likely are you to recommend Intently?
2. What's the #1 thing you love about the app?
3. What's the most frustrating part?
4. Would you be sad if this app disappeared tomorrow?
5. What feature would make you use it MORE?

### Success Metrics to Track
- Onboarding completion rate (target: >80%)
- Goal setup rate (target: >70% within 3 days)
- Daily app opens (target: 4+ per week)
- Session duration in Intently (target: <2 min - quick checks)
- Actual usage reduction (target: 20%+ in first week)

---

## Checkpoint: Before Phase 2

### Required Before Moving Forward

**Quantitative:**
- [ ] 10 beta users completed 1 week
- [ ] 70%+ would be sad if app disappeared (NPS >40)
- [ ] Average 4+ app opens per week
- [ ] 70%+ set up goals within 3 days

**Intervention Overlay Metrics (CRITICAL):**
- [ ] 30%+ dismissal rate (users choose "Go Back" vs "Proceed")
- [ ] 25%+ reduction in session duration after interventions
- [ ] 8+ second average engagement time with overlays
- [ ] >70% describe interventions as "helpful" not "annoying"
- [ ] Users report interventions feel "fresh" not "repetitive"

**Qualitative:**
- [ ] Identified top 3 loved features
- [ ] Identified top 3 pain points (and fixed or documented)
- [ ] Users report actual usage reduction (self-reported)
- [ ] Clear understanding of WHY users find it valuable
- [ ] At least 3 users spontaneously mention the intervention screens as valuable

### If Checkpoint Fails
- DO NOT proceed to Phase 2
- **If intervention metrics fail:** This is the core product - iterate until effective
- **If overall metrics fail:** Iterate on UX based on feedback
- Re-test with new users
- Don't move forward until users genuinely love it

---

## Notes & Learnings

### Feedback Log
_Document user feedback here as you collect it_

**Date:**
**User:**
**Feedback:**

---

**Example:**
- Date: 2024-12-28
- User: Sarah (28, Marketing)
- Feedback: "I love the streak counter, but I wish I could see WHY I went over my goal. Like, which app consumed most time?"

---

### Feature Requests
_Track feature requests here_

1.
2.
3.

---

### Technical Debt
_Track shortcuts or TODOs for later_

1. Goal data not yet integrated into charts (Phase 4 task)
2. Sample data generator hardcoded (refactor later)

---

**Status:** 🟡 In Progress
**Started:** December 27, 2024
**Target Completion:** January 15, 2025
**Last Updated:** December 27, 2024
