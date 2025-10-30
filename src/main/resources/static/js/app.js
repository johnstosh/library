// (c) Copyright 2025 by Muczynski - Main App Entry Point

import { getCookie, fetchData, postData, putData, deleteData, showError, clearError, showBulkSuccess, formatDate, shouldResetForSection } from './utils.js';
import { checkAuthentication, showPublicSearchPage, showLoginForm, showLoginError, showMainContent, logout } from './auth.js';
import { sectionConfig, showSection } from './sections.js';
import { initApp } from './init.js';

// Attach global functions and variables for HTML onclick and other scripts
window.isLibrarian = false;
window.getCookie = getCookie;
window.fetchData = fetchData;
window.postData = postData;
window.putData = putData;
window.deleteData = deleteData;
window.showError = showError;
window.clearError = clearError;
window.showBulkSuccess = showBulkSuccess;
window.formatDate = formatDate;
window.shouldResetForSection = shouldResetForSection;
window.checkAuthentication = checkAuthentication;
window.showPublicSearchPage = showPublicSearchPage;
window.showLoginForm = showLoginForm;
window.showLoginError = showLoginError;
window.showMainContent = showMainContent;
window.logout = logout;
window.sectionConfig = sectionConfig;
window.showSection = showSection;

// Expose other globals that might be called (assuming they exist in other modules or scripts)
window.loadLibraries = window.loadLibraries || (() => console.log('loadLibraries not defined'));
window.populateBookDropdowns = window.populateBookDropdowns || (() => console.log('populateBookDropdowns not defined'));
window.loadAuthors = window.loadAuthors || (() => console.log('loadAuthors not defined'));
window.resetAuthorForm = window.resetAuthorForm || (() => console.log('resetAuthorForm not defined'));
window.loadBooks = window.loadBooks || (() => console.log('loadBooks not defined'));
window.resetBookForm = window.resetBookForm || (() => console.log('resetBookForm not defined'));
window.loadApplied = window.loadApplied || (() => console.log('loadApplied not defined'));
window.loadUsers = window.loadUsers || (() => console.log('loadUsers not defined'));
window.loadLoans = window.loadLoans || (() => console.log('loadLoans not defined'));
window.loadTestDataStats = window.loadTestDataStats || (() => console.log('loadTestDataStats not defined'));
window.loadSettings = window.loadSettings || (() => console.log('loadSettings not defined'));
window.performSearch = window.performSearch || (() => console.log('performSearch not defined'));

// Initialize the app
document.addEventListener('DOMContentLoaded', initApp);

console.log('app.js loaded');
