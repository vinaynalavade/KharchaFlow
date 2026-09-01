/**
 * KharchaFlow Official Website — Main JavaScript Logic
 * Clean light-theme consumer product experience.
 * Mobile navigation with scroll lock, dynamic release metadata,
 * smooth scroll animations, and clipboard helpers.
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
    defaultSha256: '9f4fb30045922166f1f1eb60f8fc8b3cda6bdbe3f9cf0c235ec2a5fb1b9b4c5b',
    repoUrl: 'https://github.com/vinaynalavade/KharchaFlow',
    releasesUrl: 'https://github.com/vinaynalavade/KharchaFlow/releases'
  };

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

  // --- Smooth Scroll Animations ---
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
      sizeBytes: 3953036,
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
    initMobileNav();
    initActiveNav();
    initScrollAnimations();
    initReleaseMetadata();
    initCopyButtons();
  });
})();
