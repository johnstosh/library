from playwright.sync_api import sync_playwright, expect
import time

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    time.sleep(5) # a small delay
    page.goto("http://localhost:8080")
    page.get_by_test_id("menu-login").click()
    expect(page.get_by_test_id("login-form")).to_be_visible()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("divinemercy")
    page.get_by_test_id("login-submit").click()
    expect(page.get_by_test_id("main-content")).to_be_visible()
    page.get_by_test_id("menu-test-data").click()
    page.on("dialog", lambda dialog: dialog.dismiss())
    page.get_by_test_id("total-purge-btn").click()
    error_div = page.locator('#test-data-section [data-test="form-error"]')
    expect(error_div).to_be_visible()
    expect(error_div).to_have_text("Failed to purge database: This is a test exception.")
    page.screenshot(path="jules-scratch/verification/error_verification.png")
    browser.close()