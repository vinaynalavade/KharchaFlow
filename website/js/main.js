/**
 * KharchaFlow Official Website — Main JavaScript Logic
 * Theme toggling (Default: Light), mobile navigation with scroll lock,
 * dynamic release metadata, luxury scroll animations, clipboard helpers.
 */

(function () {
  'use strict';

  // --- Configuration & Production Release Fallbacks ---
  const RELEASE_CONFIG = {
    owner: 'vinaynalavade',
    repo: 'KharchaFlow',
    defaultVersionName: '1.0.5',
    defaultVersionCode: 6,
    defaultReleaseDate: 'September 1, 2026',
    defaultApkSize: '3.9 MB',
    defaultApkFileName: 'KharchaFlow_v1.0.5.apk',
    defaultDownloadUrl: 'https://github.com/vinaynalavade/KharchaFlow/releases/download/v1.0.5/KharchaFlow_v1.0.5.apk',
    defaultSha256: '9ef32daef27bde5afb2a29963e21219507ffa3984f1f64f52d7dae2424e0d909',
    repoUrl: 'https://github.com/vinaynalavade/KharchaFlow',
    releasesUrl: 'https://github.com/vinaynalavade/KharchaFlow/releases'
  };

  // --- Theme Management (Light mode is strictly the default) ---
  const THEME_STORAGE_KEY = 'kharchaflow-theme';

  function getPreferredTheme() {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    if (saved === 'dark' || saved === 'light') {
      return saved;
    }
    // Default is explicitly light for luxury finance aesthetic
    return 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    const toggleBtns = document.querySelectorAll('.theme-toggle');
    toggleBtns.forEach(btn => {
      btn.setAttribute('aria-label', theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode');
      btn.innerHTML = theme === 'dark'
        ? `<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 00-1.41 0 .996.996 0 000 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 000-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 00-1.41-1.41l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 00-1.41-1.41l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>`
        : `<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 01-4.4 2.26 5.403 5.403 0 01-5.4-5.4c0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>`;
    });
  }

  function initTheme() {
    const theme = getPreferredTheme();
    applyTheme(theme);

    document.querySelectorAll('.theme-toggle').forEach(btn => {
      btn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        localStorage.setItem(THEME_STORAGE_KEY, next);
        applyTheme(next);
      });
    });
  }

  // --- Mobile Navigation Drawer ---
  function initMobileNav() {
    const menuBtn = document.querySelector('.mobile-menu-btn');
    const nav = document.querySelector('.nav');

    if (!menuBtn || !nav) return;

    function closeNav() {
      nav.classList.remove('is-open');
      menuBtn.setAttribute('aria-expanded', 'false');
      menuBtn.innerHTML = `<svg viewBox="0 0 24 24" width="24" height="24"><path fill="currentColor" d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>`;
      document.body.style.overflow = '';
    }

    function openNav() {
      nav.classList.add('is-open');
      menuBtn.setAttribute('aria-expanded', 'true');
      menuBtn.innerHTML = `<svg viewBox="0 0 24 24" width="24" height="24"><path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>`;
      document.body.style.overflow = 'hidden';
    }

    menuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (nav.classList.contains('is-open')) {
        closeNav();
      } else {
        openNav();
      }
    });

    // Close when clicking any nav link
    nav.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        closeNav();
      });
    });

    // Close when clicking outside
    document.addEventListener('click', (e) => {
      if (nav.classList.contains('is-open') && !nav.contains(e.target) && !menuBtn.contains(e.target)) {
        closeNav();
      }
    });
  }

  // --- Active Nav Link Highlighting ---
  function initActiveNav() {
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    const navLinks = document.querySelectorAll('.nav-link');

    navLinks.forEach(link => {
      const href = link.getAttribute('href');
      if (href === currentPath || (currentPath === '' && href === 'index.html')) {
        link.classList.add('active');
        link.setAttribute('aria-current', 'page');
      } else {
        link.classList.remove('active');
        link.removeAttribute('aria-current');
      }
    });
  }

  // --- Luxury Scroll Animations ---
  function initScrollAnimations() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }

    const animTargets = document.querySelectorAll('.animate-on-scroll, .card, .feature-story, .privacy-banner, .download-card, .github-banner');
    
    animTargets.forEach(el => {
      el.classList.add('animate-on-scroll');
    });

    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver((entries, obs) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            obs.unobserve(entry.target);
          }
        });
      }, {
        threshold: 0.08,
        rootMargin: '0px 0px -40px 0px'
      });

      animTargets.forEach(el => observer.observe(el));
    } else {
      // Fallback
      animTargets.forEach(el => el.classList.add('is-visible'));
    }
  }

  // --- Dynamic Release Metadata Fetching ---
  async function initReleaseMetadata() {
    updateReleaseUiElements({
      versionName: RELEASE_CONFIG.defaultVersionName,
      downloadUrl: RELEASE_CONFIG.defaultDownloadUrl,
      sha256: RELEASE_CONFIG.defaultSha256,
      sizeBytes: 3953040,
      formattedSize: RELEASE_CONFIG.defaultApkSize,
      publishedAt: RELEASE_CONFIG.defaultReleaseDate,
      fileName: RELEASE_CONFIG.defaultApkFileName
    });

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 6000);

      const response = await fetch(
        `https://api.github.com/repos/${RELEASE_CONFIG.owner}/${RELEASE_CONFIG.repo}/releases/latest`,
        {
          signal: controller.signal,
          headers: { 'Accept': 'application/vnd.github.v3+json' }
        }
      );
      clearTimeout(timeoutId);

      if (!response.ok) return;

      const releaseData = await response.json();
      if (!releaseData || !Array.isArray(releaseData.assets)) return;

      const apkAsset = releaseData.assets.find(
        asset => asset.name && asset.name.endsWith('.apk') && !asset.name.includes('debug')
      ) || releaseData.assets.find(asset => asset.name && asset.name.endsWith('.apk'));

      if (!apkAsset) return;

      const rawTag = releaseData.tag_name || '';
      const versionName = rawTag.replace(/^v/i, '') || RELEASE_CONFIG.defaultVersionName;
      const formattedSize = apkAsset.size
        ? `${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB`
        : RELEASE_CONFIG.defaultApkSize;

      let sha256 = RELEASE_CONFIG.defaultSha256;
      if (apkAsset.digest && apkAsset.digest.startsWith('sha256:')) {
        sha256 = apkAsset.digest.substring(7).trim();
      }

      const publishedDate = releaseData.published_at
        ? new Date(releaseData.published_at).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
        : RELEASE_CONFIG.defaultReleaseDate;

      updateReleaseUiElements({
        versionName: versionName,
        downloadUrl: apkAsset.browser_download_url || RELEASE_CONFIG.defaultDownloadUrl,
        sha256: sha256,
        sizeBytes: apkAsset.size,
        formattedSize: formattedSize,
        publishedAt: publishedDate,
        fileName: apkAsset.name
      });
    } catch (_err) {
      // Graceful silent fallback
    }
  }

  function updateReleaseUiElements(data) {
    document.querySelectorAll('.dynamic-version').forEach(el => {
      el.textContent = `v${data.versionName}`;
    });

    document.querySelectorAll('.dynamic-version-plain').forEach(el => {
      el.textContent = data.versionName;
    });

    document.querySelectorAll('.dynamic-apk-size').forEach(el => {
      el.textContent = data.formattedSize;
    });

    document.querySelectorAll('.dynamic-release-date').forEach(el => {
      el.textContent = data.publishedAt;
    });

    document.querySelectorAll('.dynamic-sha256').forEach(el => {
      el.textContent = data.sha256;
    });

    document.querySelectorAll('.dynamic-download-link').forEach(el => {
      el.setAttribute('href', data.downloadUrl);
    });

    document.querySelectorAll('.dynamic-filename').forEach(el => {
      el.textContent = data.fileName;
    });
  }

  // --- Copy to Clipboard Helper ---
  function initCopyButtons() {
    document.querySelectorAll('.copy-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const targetSelector = btn.getAttribute('data-copy-target');
        const targetEl = targetSelector ? document.querySelector(targetSelector) : null;
        const textToCopy = targetEl ? targetEl.textContent.trim() : btn.getAttribute('data-copy-text');

        if (!textToCopy) return;

        try {
          await navigator.clipboard.writeText(textToCopy);
          const originalText = btn.textContent;
          btn.textContent = 'Copied!';
          btn.classList.add('copied');
          setTimeout(() => {
            btn.textContent = originalText;
            btn.classList.remove('copied');
          }, 2000);
        } catch (_err) {
          const textarea = document.createElement('textarea');
          textarea.value = textToCopy;
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand('copy');
          document.body.removeChild(textarea);
          btn.textContent = 'Copied!';
          setTimeout(() => { btn.textContent = 'Copy'; }, 2000);
        }
      });
    });
  }

  // --- Initialize on DOM Ready ---
  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initMobileNav();
    initActiveNav();
    initScrollAnimations();
    initReleaseMetadata();
    initCopyButtons();
  });
})();
