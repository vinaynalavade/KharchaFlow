# KharchaFlow — Official Website

This folder contains the complete, production-quality official website for the **KharchaFlow** Android application.

Built using lightweight, semantic HTML5, modern CSS3 (with CSS Custom Properties and dark mode support), and clean Vanilla JavaScript with zero external build tools or framework dependencies.

---

## 📁 Directory Structure

```text
website/
├── index.html            # Official homepage (Hero, feature stories, release card, trust highlights)
├── features.html         # In-depth breakdown of transaction, budget, recurring, and security capabilities
├── download.html         # Official APK download, SHA-256 checksums, and step-by-step install guide
├── privacy.html          # Clear, honest privacy policy and on-device data architecture
├── about.html            # Product story, technical architecture, and open-source credits
├── 404.html              # Custom branded 404 error page
├── robots.txt            # Search engine crawler instructions
├── sitemap.xml           # Search engine sitemap
├── assets/
│   ├── images/           # Official logo, icons, and branding assets
│   │   ├── logo.png      # High-res KharchaFlow app logo
│   │   ├── favicon.png   # Favicon and apple-touch-icon
│   │   └── icon-round.png
│   ├── icons/            # Scalable SVG icons
│   └── mockups/          # Interactive CSS UI components
├── css/
│   └── styles.css        # Responsive design system, theme tokens, and typography
├── js/
│   └── main.js           # Mobile nav, dynamic GitHub Release integration, clipboard helpers
└── README.md             # This documentation file
```

---

## 🚀 Running Locally

Because the website is 100% static, you can run it in either of two ways:

### Option 1: Direct File Opening
Simply double-click `website/index.html` in your file explorer, or open it in any modern web browser.

### Option 2: Local HTTP Server (Recommended)
Using Python:
```bash
# In PowerShell / Command Prompt
cd "X:\Expense Tracker\website"
python -m http.server 8000
```
Then visit: `http://localhost:8000`

---

## 🌐 Deploying to GitHub Pages

To host this website on GitHub Pages for `https://vinaynalavade.github.io/KharchaFlow/`:

### Method A: From a `gh-pages` Branch
1. Push the contents of the `website/` folder to a `gh-pages` branch in your repository.
2. In GitHub repository settings: **Settings → Pages → Source → Deploy from branch `gh-pages` / `/ (root)`**.

### Method B: From the `main` Branch (`/docs` or GitHub Actions)
You can configure a GitHub Actions workflow that automatically publishes the `website/` directory to GitHub Pages on every push to `main`.

---

## ⚙️ Updating Releases & Download URLs

The website features an automated GitHub Release integration in `website/js/main.js` that attempts to fetch the latest release metadata directly from:
`https://api.github.com/repos/vinaynalavade/KharchaFlow/releases/latest`

If GitHub API rate limits or offline conditions occur, the website falls back to the constants defined at the top of `website/js/main.js`:

```javascript
const RELEASE_CONFIG = {
  owner: 'vinaynalavade',
  repo: 'KharchaFlow',
  defaultVersionName: '1.0.5',
  defaultVersionCode: 6,
  defaultReleaseDate: 'September 1, 2026',
  defaultApkSize: '3.9 MB',
  defaultApkFileName: 'KharchaFlow_v1.0.5.apk',
  defaultDownloadUrl: 'https://github.com/vinaynalavade/KharchaFlow/releases/download/v1.0.5/KharchaFlow_v1.0.5.apk',
  defaultSha256: '9f4fb30045922166f1f1eb60f8fc8b3cda6bdbe3f9cf0c235ec2a5fb1b9b4c5b',
  repoUrl: 'https://github.com/vinaynalavade/KharchaFlow',
  releasesUrl: 'https://github.com/vinaynalavade/KharchaFlow/releases'
};
```

When you publish future releases (e.g. `v1.0.6`), simply update these fallback constants in `website/js/main.js`.

---

## 🎨 Design System & Color Tokens

All design tokens, colors, typography, and spacing are defined in `website/css/styles.css` matching the KharchaFlow Logo Green brand system:

* **Brand Primary:** `--primary: #028166` (KharchaFlow Logo Green)
* **Semantic Income:** `--income: #059669` (Positive Income Emerald)
* **Semantic Expense:** `--expense: #e11d48` (Expense Rose Dark)
* **Canvas Background:** `--bg-page: #f8fafc` (Slate 50)
* **Card Surface:** `--bg-surface: #ffffff` (Pure White)
* **Text / Headings:** `--text-primary: #0f172a` (Slate 900)


