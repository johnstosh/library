document.addEventListener('DOMContentLoaded', function () {
    const darkModeSwitch = document.getElementById('dark-mode-switch');

    fetch('/api/settings')
        .then(response => response.json())
        .then(settings => {
            darkModeSwitch.checked = settings.isDarkMode;
            updateDarkMode(settings.isDarkMode);
        });

    darkModeSwitch.addEventListener('change', function () {
        const isDarkMode = this.checked;
        updateDarkMode(isDarkMode);
        fetch('/api/settings', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ isDarkMode: isDarkMode })
        });
    });

    function updateDarkMode(isDarkMode) {
        if (isDarkMode) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    }
});