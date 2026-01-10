# Privacy Policy for Intently

**Last Updated:** December 26, 2025

## Introduction

Intently is a digital wellbeing application designed to help you build mindful social media usage habits. This privacy policy explains how Intently collects, uses, and protects your information.

## Developer Information

**App Name:** Intently
**Developer:** MD Sadakat Hussain Fahad
**Contact Email:** contact@sadakat.dev

## Our Commitment to Privacy

Intently is built with privacy as a core principle. Your data never leaves your device. We do not collect, transmit, or share any of your personal information with third parties.

## Information We Collect

Intently collects the following information to provide its core functionality:

### App Usage Data
- **Target App Detection:** When you open monitored apps (e.g., Facebook, Instagram)
- **Usage Sessions:** Start time, end time, and duration of app usage
- **Session Events:** When reminders are shown, when timer alerts are triggered
- **Daily Statistics:** Aggregated usage data per day
- **User Goals:** Goals you set for managing your usage time

### What We DON'T Collect
- ❌ No personal information (name, email, phone number)
- ❌ No app content (posts, messages, photos)
- ❌ No account credentials
- ❌ No location data
- ❌ No device identifiers
- ❌ No browsing history outside monitored apps

## How We Use Your Information

All data collected is used exclusively to:
1. **Show Reminders:** Display mindfulness prompts when you open monitored apps
2. **Track Usage Time:** Monitor how long you use specific apps
3. **Display Statistics:** Show you charts and insights about your usage patterns
4. **Timer Alerts:** Notify you when you've exceeded your set time limits
5. **Goal Tracking:** Help you monitor progress toward your usage goals

## Data Storage and Security

### Local Storage Only
- All data is stored **locally on your device** using Android's SQLite database
- Data is stored in your app's private storage directory
- **No cloud storage** - your data never leaves your device
- **No internet connection required** - Intently works 100% offline

### Data Retention
- Usage sessions older than **90 days** are automatically deleted
- You can manually clear all data by uninstalling the app
- No backup of your data is created

### Data Security
- Data is stored in Android's app-specific storage (protected by Android OS)
- Only Intently can access its own data
- Standard Android security protections apply

## Data Sharing and Third Parties

**We do not share, sell, or transmit your data to anyone.**

- ❌ No third-party analytics services
- ❌ No advertising networks
- ❌ No cloud services
- ❌ No external servers
- ❌ No data brokers

Intently contains zero third-party tracking or analytics libraries.

## Permissions Explained

Intently requires the following Android permissions to function:

### 1. Usage Access (PACKAGE_USAGE_STATS)
- **Purpose:** Detect when you open Facebook, Instagram, or other configured apps
- **What it accesses:** Recent app launch history and foreground app detection
- **What it does NOT access:** App content, messages, or personal data within apps
- **User control:** You grant this in Android Settings → Apps → Special Access → Usage Access

### 2. Display Over Other Apps (SYSTEM_ALERT_WINDOW)
- **Purpose:** Show reminder overlays when you open monitored apps
- **What it does:** Displays full-screen mindfulness prompts over your apps
- **User control:** You grant this in Android Settings → Apps → Special Access → Display Over Other Apps

### 3. Foreground Service - Special Use
- **Purpose:** Run continuous monitoring in the background
- **What it does:** Keeps the app running to detect app launches even when you're not actively using Intently
- **Notification:** Shows a persistent "Intently is monitoring" notification (required by Android)

### 4. Post Notifications
- **Purpose:** Display the persistent foreground service notification
- **What it does:** Shows low-priority notification that the service is active

### 5. Wake Lock
- **Purpose:** Keep monitoring active during usage sessions
- **Battery impact:** Optimized with adaptive polling to minimize battery drain

### 6. Receive Boot Completed
- **Purpose:** Restart monitoring service after device reboot
- **User control:** Monitoring automatically resumes only if it was active before reboot

## Your Rights and Control

### You Have Full Control:
- **Enable/Disable Monitoring:** Turn the service on/off anytime from the app
- **Configure Target Apps:** Choose which apps to monitor
- **Adjust Settings:** Customize timer duration and reminder behavior
- **View Your Data:** See all collected usage statistics in the Stats screen
- **Delete All Data:** Uninstalling Intently permanently removes all data

### No Account Required:
- Intently does not require registration or login
- No email, username, or password needed
- No user profile created

## Children's Privacy

Intently does not knowingly collect information from children under 13. The app is designed for general audiences and does not contain age-restricted content. Since all data is stored locally and no personal information is collected, there is no risk of children's data being transmitted or shared.

## Changes to This Privacy Policy

We may update this privacy policy from time to time. Any changes will be reflected in:
- The "Last Updated" date at the top of this policy
- App updates through the Google Play Store

Continued use of the app after policy changes constitutes acceptance of the updated policy.

## Data Protection Rights (GDPR/CCPA Compliance)

If you are in the European Union or California, you have the following rights:

- **Right to Access:** View all data through the app's Stats screen
- **Right to Deletion:** Uninstall the app to permanently delete all data
- **Right to Portability:** Not applicable (no account or cloud storage)
- **Right to Rectification:** Modify goals and settings within the app
- **Right to Object:** Disable monitoring or uninstall the app

Since all data is local and never transmitted, there is no data processing or sharing to object to.

## Technical Implementation Details

For transparency, here's how Intently works technically:

1. **App Detection:** Uses Android's UsageStatsManager API to detect foreground apps
2. **Storage:** SQLite database via Android Room library
3. **Monitoring:** Foreground service with adaptive polling (reduces frequency when screen is off or no activity detected)
4. **Overlays:** WindowManager API to display reminders over apps
5. **Network:** Zero network libraries - completely offline
6. **Analytics:** No analytics or crash reporting SDKs

## Contact Us

If you have questions about this privacy policy or Intently's privacy practices:

**Email:** contact@sadakat.dev
**Response Time:** Within 48 hours

## Compliance

This privacy policy complies with:
- Google Play Store Developer Policies
- General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Android app privacy best practices

---

**Summary:** Intently is a privacy-first app. Your usage data stays on your device, is never shared with anyone, and is automatically deleted after 90 days. We collect only what's necessary for the app to help you build mindful social media habits.
