// (c) Copyright 2025 by Muczynski - Sections Module

import { shouldResetForSection } from './utils.js';
import { loadLocBulkLookupSection } from './loc-bulk-lookup.js';

// Combined loader for library-card section
function loadLibraryCardSectionCombined() {
    if (window.loadLibraryCardSection) {
        window.loadLibraryCardSection();
    }
    if (window.loadApplied) {
        window.loadApplied();
    }
}

export const sectionConfig = {
    'libraries': { load: loadLibraries, reset: null },
    'authors': { load: loadAuthors, reset: resetAuthorForm },
    'books': { load: loadBooks, reset: resetBookForm },
    'books-from-feed': { load: loadBooksFromFeedSection, reset: null },
    'photos': { load: loadPhotosSection, reset: null },
    'search': { load: null, reset: null },
    'library-card': { load: loadLibraryCardSectionCombined, reset: null },
    'users': { load: loadUsers, reset: null },
    'loans': { load: loadLoansSection, reset: null },
    'test-data': { load: loadTestDataStats, reset: null },
    'global-settings': { load: loadGlobalSettings, reset: null },
    'loc-bulk-lookup': { load: loadLocBulkLookupSection, reset: null },
    'settings': { load: loadSettings, reset: null }
};

export function showSection(sectionId, event) {
    console.log(`[Sections] Showing section: ${sectionId}`);
    const currentActiveButton = document.querySelector('#section-menu button.active');
    const newActiveButton = event ? event.target.closest('button') : document.querySelector(`#section-menu button[onclick*="'${sectionId}'"]`);

    // Reset forms when navigating away
    if (currentActiveButton) {
        const currentSectionId = currentActiveButton.getAttribute('onclick').match(/'([^']+)'/)[1];
        if (currentSectionId !== sectionId) {
            const config = sectionConfig[currentSectionId];
            if (config && config.reset && shouldResetForSection(currentSectionId)) {
                config.reset();
            }
        }
    }

    // If not logged in and showing a section, hide login and show main content
    if (!window.isLibrarian && (document.getElementById('login-form')?.style.display === 'block')) {
        const loginForm = document.getElementById('login-form');
        if (loginForm) loginForm.style.display = 'none';
        const mainContent = document.getElementById('main-content');
        if (mainContent) mainContent.style.display = 'block';
    }

    // Hide all sections and show the target one
    document.querySelectorAll('.section').forEach(section => {
        section.style.setProperty('display', 'none', 'important');
    });
    const targetSection = document.getElementById(sectionId + '-section');
    if (targetSection) {
        targetSection.style.setProperty('display', 'block', 'important');
    }

    // Load data and reset form for the section
    const config = sectionConfig[sectionId];
    if (config) {
        if (currentActiveButton === newActiveButton && config.reset && shouldResetForSection(sectionId)) {
            console.log(`[Sections] Resetting form for section: ${sectionId}`);
            config.reset();
        }
        if (config.load) {
            console.log(`[Sections] Loading data for section: ${sectionId}`);
            config.load();
        }
    } else {
        console.warn(`[Sections] No config found for section: ${sectionId}`);
    }


    // Update active button
    document.querySelectorAll('#section-menu button').forEach(btn => {
        btn.classList.remove('active');
    });
    if (newActiveButton) {
        newActiveButton.classList.add('active');
    }

    // Hide the navbar on mobile after a menu item is clicked
    const navbarCollapse = document.getElementById('navbarNav');
    if (navbarCollapse && navbarCollapse.classList.contains('show')) {
        const bsCollapse = new bootstrap.Collapse(navbarCollapse, {
            toggle: false
        });
        bsCollapse.hide();
    }
}
