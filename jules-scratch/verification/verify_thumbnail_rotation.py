import time
from playwright.sync_api import sync_playwright, expect

def run(playwright):
    time.sleep(10)
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")

    # Login
    page.locator("[data-test='menu-login']").click()
    page.locator("[data-test='login-username']").fill("librarian")
    page.locator("[data-test='login-password']").fill("divinemercy")
    page.locator("[data-test='login-submit']").click()

    # Wait for the main content to load
    expect(page.locator("[data-test='main-content']")).to_be_visible()

    # Go to books page
    expect(page.locator("[data-test='loans-section']")).to_be_visible()
    page.locator("[data-test='menu-books-link']").click()

    # Take screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)