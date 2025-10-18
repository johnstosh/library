from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    p.selectors.set_test_id_attribute("data-test")
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://localhost:8080")

    # Wait for the login button to be visible
    page.wait_for_selector('[data-test="menu-login"]')

    # Login
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("librarian")
    page.get_by_test_id("login-submit").click()

    # Wait for the user-is-librarian class to be added to the body
    page.wait_for_selector('body.user-is-librarian')

    # Navigate to Test Data section
    page.get_by_test_id("menu-test-data").click()

    # Take screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()