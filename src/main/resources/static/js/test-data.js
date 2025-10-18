async function generateTestData() {
    const numBooks = document.getElementById('num-books').value;
    if (!numBooks || numBooks <= 0) {
        alert('Please enter a valid number of books to generate.');
        return;
    }

    try {
        const response = await fetch('/api/test-data/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify({ numBooks: numBooks })
        });

        if (response.ok) {
            alert('Test data generated successfully!');
        } else {
            const error = await response.text();
            alert(`Error generating test data: ${error}`);
        }
    } catch (error) {
        console.error('Error generating test data:', error);
        alert('An error occurred while generating test data.');
    }
}

async function deleteAllTestData() {
    if (!confirm('Are you sure you want to delete all test data? This action cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch('/api/test-data/delete-all', {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (response.ok) {
            alert('Test data deleted successfully!');
        } else {
            const error = await response.text();
            alert(`Error deleting test data: ${error}`);
        }
    } catch (error) {
        console.error('Error deleting test data:', error);
        alert('An error occurred while deleting test data.');
    }
}