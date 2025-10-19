from playwright.sync_api import sync_playwright, expect

def run_verification():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        try:
            # 1. Arrange: Go to the application homepage.
            page.goto("http://localhost:8080")

            # Login
            page.click("[data-test='menu-login']")
            page.wait_for_selector("[data-test='login-form']", state='visible', timeout=60000)
            page.fill("[data-test='login-username']", "librarian")
            page.fill("[data-test='login-password']", "password")
            page.click("[data-test='login-submit']")
            page.wait_for_selector("[data-test='main-content']", state='visible', timeout=60000)
            page.wait_for_timeout(1000) # Wait for 1 second

            # 2. Act: Navigate to the search page and perform a search.
            page.click("[data-test='menu-search']")
            page.wait_for_selector("[data-test='search-section']", state='visible', timeout=60000)
            page.fill("[data-test='search-input']", "a")
            page.click("[data-test='search-btn']")
            page.wait_for_selector("[data-test='search-results']", state='visible', timeout=60000)

            # 3. Assert: Check if the book numbers are displayed correctly.
            # Wait for the search results to load
            expect(page.locator("[data-test='search-books-list']")).to_be_visible(timeout=60000)

            # Check the header
            expect(page.locator("h4:has-text('Books 1 - 20')")).to_be_visible(timeout=60000)

            # Check the first book
            expect(page.locator("[data-test='search-book-item']").first).to_contain_text("1.", timeout=60000)

            # 4. Screenshot: Capture the final result for visual verification.
            page.screenshot(path="jules-scratch/verification/verification.png")

        finally:
            browser.close()

if __name__ == "__main__":
    run_verification()