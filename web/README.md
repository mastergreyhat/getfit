# GetFit — Web Application & Interactive Showcase

This directory contains the standalone web edition and interactive showcase landing page for **GetFit**, designed according to the "Clean" editorial SaaS design system (pure white backgrounds, OLED dark mode, geometric typography, subtle borders, zero clutter).

---

## 🚀 How to Run the Web App

You can open the web application directly in any browser:

### Option 1: Direct File Launch
Double-click `index.html` or open it with your browser:
```bash
# Windows
start index.html

# macOS
open index.html

# Linux
xdg-open index.html
```

### Option 2: Local HTTP Server (Recommended for PWA & full features)
Using Python:
```bash
python -m http.server 8080
# Open http://localhost:8080 in your browser
```

Using Node.js:
```bash
npx serve .
```

---

## 📁 Files

- `index.html` — Interactive web application, simulator, and marketing landing page with live responsive mobile frame, theme toggle (OLED Dark & Editorial Light), real-time macro calculation, pantry planner, and streak calendar.
- `logo.png` — High-resolution GetFit brand logo.
- `favicon.png` — Web browser favicon.
