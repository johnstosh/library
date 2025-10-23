from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")

    # Login
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()

    # Go to applications page
    page.wait_for_selector('[data-test="main-content"]')
    page.get_by_test_id("menu-applied").click()

    # Verify the changes
    expect(page.get_by_test_id("applied-header")).to_be_visible()

    # Check that the form is gone
    expect(page.get_by_test_id("applied-form")).not_to_be_visible()

    # Check for the new buttons
    expect(page.locator('button:has-text("✔️")')).to_be_visible()
    expect(page.locator('button:has-text("🗑️")')).to_be_visible()

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)