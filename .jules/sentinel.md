## 2025-03-09 - Hardcoded VNC Password Fallback
**Vulnerability:** A hardcoded default password (`12334555`) was used as a fallback when reading stored preferences in `PreferencesRepository`.
**Learning:** Default fallbacks in preference loading functions can inadvertently expose hardcoded credentials or cause unauthorized access with hardcoded passwords.
**Prevention:** Always default sensitive fields (passwords, tokens, keys) to empty strings or `null` when no saved preference exists.
