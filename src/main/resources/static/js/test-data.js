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
        showError('test-data', 'Please enter a valid number of books to generate.');
        return;
    }

    try {
        await postData('/api/test-data/generate', { numBooks: numBooks });

        document.getElementById('num-books').value = '';
        await loadBooks();
        await loadAuthors();
        await loadTestDataStats();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to generate test data: ' + error.message);
    }
}

async function generateTestLoans() {
    const numLoansInput = document.getElementById('num-books').value;
    const numLoans = parseInt(numLoansInput);
    if (!numLoansInput || numLoans <= 0) {
        showError('test-data', 'Please enter a valid number of loans to generate.');
        return;
    }

    try {
        await postData('/api/test-data/generate-loans', { numLoans: numLoans });

        document.getElementById('num-books').value = '';
        await loadTestDataStats();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to generate test loans: ' + error.message);
    }
}

async function deleteAllTestData() {
    if (!confirm('Are you sure you want to delete all test data? This action cannot be undone.')) {
        return;
    }

    try {
        // Read an existing protected endpoint first to validate session/CSRF
        await fetchData('/api/libraries');

        await deleteData('/api/test-data/delete-all');

        await loadBooks();
        await loadAuthors();
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to delete test data: ' + error.message);
    }
}
