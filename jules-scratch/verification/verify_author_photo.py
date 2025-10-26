from playwright.sync_api import sync_playwright, Page, expect
import os

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        page.goto("http://localhost:8080")

        # Wait for the page to be ready and login
        page.wait_for_selector("[data-test='menu-login']", timeout=120000)
        page.locator("[data-test='menu-login']").click()
        page.locator("[data-test='login-username']").fill("librarian")
        page.locator("[data-test='login-password']").fill("divinemercy")
        page.locator("[data-test='login-submit']").click()
        expect(page.locator("[data-test='main-content']")).to_be_visible()

        # Navigate to authors
        page.locator("[data-test='menu-authors']").click()
        expect(page.locator("[data-test='authors-section']")).to_be_visible()

        # Create a new author to ensure one exists
        page.locator("[data-test='new-author-name']").fill("Test Author for Photo Verification")
        page.locator("[data-test='add-author-btn']").click()
        expect(page.locator("[data-test='author-item']").first).to_be_visible()


        # Edit an author
        page.locator("[data-test='edit-author-btn']").first.click()

        # Verify "Add Photo" button is visible
        expect(page.locator("[data-test='add-author-photo-btn']")).to_be_visible()

        # Take a screenshot
        screenshot_path = "jules-scratch/verification/verification.png"
        page.screenshot(path=screenshot_path)
        print(f"Screenshot saved to {screenshot_path}")

    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)