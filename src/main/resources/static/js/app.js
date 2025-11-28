// (c) Copyright 2025 by Muczynski - Main App Entry Point

// Initialize global isLibrarian flag (will be set by auth.js when user logs in)
window.isLibrarian = false;

// Expose fallback functions for any that might not be defined yet
// These will be replaced by actual implementations when their respective files load
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
