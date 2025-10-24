from playwright.sync_api import sync_playwright, expect

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto("http://localhost:8080")
    page.get_by_test_id("menu-login").click()
    expect(page.get_by_test_id("login-form")).to_be_visible(timeout=10000)
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("divinemercy")
    page.get_by_test_id("login-submit").click()
    expect(page.get_by_test_id("main-content")).to_be_visible()
    page.get_by_test_id("menu-test-data").click()
    page.screenshot(path="jules-scratch/verification/verification.png")
    browser.close()