// (c) Copyright 2025 by Muczynski
async function loadTestDataStats() {
    try {
        const stats = await fetchData('/api/test-data/stats');
        const statsDiv = document.getElementById('test-data-stats');
        statsDiv.innerHTML = `
            <p><strong>Books:</strong> ${stats.books}</p>
            <p><strong>Authors:</strong> ${stats.authors}</p>
            <p><strong>Loans:</strong> ${stats.loans}</p>
        `;
    } catch (error) {
        showError('test-data', 'Failed to load test data stats: ' + error.message);
    }
}

async function generateTestData() {
    const numBooksInput = document.getElementById('num-books').value;
    const numBooks = parseInt(numBooksInput);
    if (!numBooksInput || numBooks <= 0) {
        showError('test-data', 'Please enter a valid number of items to generate.');
        return;
    }

    const btn = document.getElementById('generate-test-data-btn');
    showButtonSpinner(btn, 'Generating...');

    try {
        await postData('/api/test-data/generate', { numBooks: numBooks });

        document.getElementById('num-books').value = '';
        await loadBooks();
        await loadAuthors();
        await loadTestDataStats();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to generate test data: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Add Random Books');
    }
}

async function generateTestLoans() {
    const numLoansInput = document.getElementById('num-books').value;
    const numLoans = parseInt(numLoansInput);
    if (!numLoansInput || numLoans <= 0) {
        showError('test-data', 'Please enter a valid number of loans to generate.');
        return;
    }

    const btn = document.getElementById('generate-test-loans-btn');
    showButtonSpinner(btn, 'Generating...');

    try {
        await postData('/api/test-data/generate-loans', { numLoans: numLoans });

        document.getElementById('num-books').value = '';
        await loadTestDataStats();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to generate test loans: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Add-Random-Loans');
    }
}

async function deleteAllTestData() {
    if (!confirm('Are you sure you want to delete all test data? This action cannot be undone.')) {
        return;
    }

    const btn = document.getElementById('delete-all-test-data-btn');
    showButtonSpinner(btn, 'Deleting...');

    try {
        // Read an existing protected endpoint first to validate session/CSRF
        await fetchData('/api/libraries');

        await deleteData('/api/test-data/delete-all');

        await loadBooks();
        await loadAuthors();
        await loadTestDataStats();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to delete test data: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Delete All Test Data');
    }
}

async function totalPurge() {
    if (!confirm('Are you sure you want to totally purge the database? This action cannot be undone and will require a server restart.')) {
        return;
    }

    const btn = document.getElementById('total-purge-btn');
    showButtonSpinner(btn, 'Purging...');

    try {
        // Read an existing protected endpoint first to validate session/CSRF
        await fetchData('/api/libraries');

        await deleteData('/api/test-data/total-purge');

        clearError('test-data');
        alert('Total purge successful. Please restart the server.');
    } catch (error) {
        showError('test-data', 'Failed to purge database: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Total Purge');
    }
}
