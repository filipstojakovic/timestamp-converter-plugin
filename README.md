# 🕒 Timestamp Converter — IntelliJ IDEA Plugin

Convert Unix timestamps into human-readable dates across all time zones, directly inside your IDE.  
Easily view system time, choose any time zone (with UTC offsets), customize date formats, and keep a conversion history — all from a convenient side panel.

---

## ✨ Features

- 🔄 Convert Unix timestamps to readable date/time instantly  
- 🌍 Supports all available time zones (with UTC offsets)  
- 🧠 Keeps a persistent history of conversions (saved between restarts)  
- 🖥️ Shows both **selected zone** and **system zone** time  
- ⚙️ Customizable date format (default: `dd.MM.yyyy HH:mm:ss`)  
- 💾 Settings remembered between IDE sessions  
- ⌨️ Press **Enter** to convert instantly  
- 🧹 “Clear History” button for quick cleanup  

---

## 🧭 Usage

1. Open **Timestamp Converter** from the **left sidebar** in IntelliJ IDEA.  
2. Enter a Unix timestamp (e.g. `1736185200` or `1736185200000`).  
3. Choose a target time zone from the dropdown list.  
4. Press **Enter** or click **Convert**.  
5. View results for both **selected zone** and **your system time** below.  
6. Review previous conversions in the **history panel**.

---

## ⚙️ Settings

You can change:
- The displayed date/time format (`dd-MM-yyyy HH:mm:ss`, `yyyy/MM/dd HH:mm`, etc.)
- The last used time zone (automatically saved)
  
Accessible under  
**File → Settings → Tools → Timestamp Converter**
