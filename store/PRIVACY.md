# Privacy Policy for Tama

**Last updated: 1 August 2026**

*(English version of `DATENSCHUTZ.md`. Google Play reviews submissions in English — keep both in
sync when either changes.)*

## In short

Tama collects no personal data, sends nothing to any server, and contains neither advertising nor
analytics. Everything you create in the app stays on your device.

## Controller

> **[FILL IN]**
> Name
> Postal address
> Email address

## What data is collected?

**None.** Tama does not collect, process or transmit any personal data. There are no user
accounts, no registration and no sign-in.

## What the app stores on your device

To work at all, the app stores the following in its own private storage on your device:

- **Your reminders** — label, weekdays, time window, interval, chosen motif and daily goal.
- **Your responses** — when a reminder appeared and whether you reacted to it. This is what the
  in-app statistics and your avatar's mood are derived from.
- **Your settings** — chosen avatar, clock design, language, orientation, dock-mode brightness.
- **Your animation library** — the bundled motifs and which of them you selected.

This data never leaves your device. It is not backed up to us, not synchronised and not shared
with third parties.

## Deletion

Uninstall the app and all of the above is removed with it, completely and irreversibly. No
deletion request is necessary, because there is no place where data about you would be held.

## No network access

Tama contains no code that opens a network connection. No advertising, analytics, crash
reporting or usage-tracking libraries are included.

## The AI import

The app can create reminders from text you copy or share from an AI application such as ChatGPT
or Claude.

**The app talks to no server while doing so.** It only reads the text you hand it and parses it
on the device. The conversation with the AI happens in that provider's own app; for that part,
their terms apply, not this policy.

## Permissions

The Play Store listing shows the following permissions:

| Permission | Why |
|---|---|
| `RECEIVE_BOOT_COMPLETED` | Re-schedule your reminders after the device restarts — otherwise they would stay silent until you next opened the app. |
| `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE` | Come from the Android system library *WorkManager*, which periodically verifies that the reminder chain is intact. They are not used by the app for its own purposes; the network-state permission only allows checking whether a connection exists, and permits no data transfer whatsoever. |

## Children

The app is not directed at children and collects data from nobody, regardless of age.

## Changes

If anything about this processing changes — for instance by later adding advertising or crash
reporting — this policy will be updated beforehand and given a new date.
