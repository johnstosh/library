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
        clearError('test-data');
    } catch (error) {
        showError('test-data', 'Failed to generate test data: ' + error.message);
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
