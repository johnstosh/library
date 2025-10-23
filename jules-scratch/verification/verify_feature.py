from playwright.sync_api import sync_playwright, expect

def run_verification():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Login
        page.goto("http://localhost:8080")
        page.locator('[data-test="menu-login"]').click()
        page.locator('[data-test="login-username"]').fill("librarian")
        page.locator('[data-test="login-password"]').fill("divinemercy")
        page.locator('[data-test="login-submit"]').click()

        # Wait for main content to load
        expect(page.locator('[data-test="main-content"]')).to_be_visible()

        # Go to authors page
        page.locator('[data-test="menu-authors"]').click()

        # Add an author
        page.locator('[data-test="new-author-name"]').fill("Test Author")
        page.locator('[data-test="add-author-btn"]').click()

        # Wait for the table to be visible
        expect(page.locator('[data-test="author-table"]')).to_be_visible()

        # Click edit on the first author
        page.locator('[data-test="edit-author-btn"]').first.click()

        # Click cancel
        page.locator('[data-test="cancel-author-btn"]').click()

        # Verify the list is visible
        expect(page.locator('[data-test="author-table"]')).to_be_visible()

        # Take a screenshot
        page.screenshot(path="jules-scratch/verification/verification.png")

        browser.close()

if __name__ == "__main__":
    run_verification()