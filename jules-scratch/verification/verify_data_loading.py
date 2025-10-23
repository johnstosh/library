from playwright.sync_api import sync_playwright, expect

def run_verification():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 1. Login as librarian
        page.goto("http://localhost:8080")

        # Click the login button to reveal the form
        page.click('button:has-text("Login")')

        # Wait for the login form to be visible
        expect(page.locator('input[name="username"]')).to_be_visible(timeout=60000)

        page.fill('input[name="username"]', 'librarian')
        page.fill('input[name="password"]', 'divinemercy')
        page.click('button[type="submit"]')

        # 2. Create test data
        page.click('button:has-text("Test Data")')

        # Wait for the "Create Test Data" button to be visible
        create_button = page.locator('button:has-text("Create Test Data")')
        expect(create_button).to_be_visible(timeout=60000)
        create_button.click()

        # Wait for the confirmation message
        expect(page.locator("text=Test data created successfully")).to_be_visible()

        # 3. Navigate to Loans page and verify
        page.click('button:has-text("Loans")')
        expect(page.locator('[data-test="loan-item"]')).not_to_be_empty(timeout=60000)
        page.screenshot(path="jules-scratch/verification/loans-page.png")

        # 4. Navigate to Users page and verify
        page.click('button:has-text("Users")')
        expect(page.locator('[data-test="user-item"]')).not_to_be_empty(timeout=60000)
        page.screenshot(path="jules-scratch/verification/users-page.png")

        browser.close()

if __name__ == "__main__":
    run_verification()