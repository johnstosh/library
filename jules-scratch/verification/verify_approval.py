import re
from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        # Apply for a card first
        page.goto("http://localhost:8080/apply-for-card.html")
        page.get_by_label("Name").fill("testuser")
        page.get_by_label("Password").fill("password")
        page.get_by_role("button", name="Apply").click()
        expect(page.get_by_text("Library card application successful!")).to_be_visible()

        # Log in as librarian
        page.goto("http://localhost:8080")
        page.get_by_role("button", name="Login").click()
        page.locator('[data-test="login-username"]').fill("librarian")
        page.locator('[data-test="login-password"]').fill("password")

        # Click the login button and wait for the navigation to complete
        with page.expect_navigation():
            page.locator('[data-test="login-form"] button[type="submit"]').click()

        # Wait for main content to be visible
        expect(page.locator('[data-test="main-content"]')).to_be_visible()

        # Go to applications page
        page.get_by_role("button", name="Applied").click()

        # Find the row for the new user and approve
        row = page.get_by_role("row", name=re.compile("testuser"))

        page.on("dialog", lambda dialog: dialog.accept())
        row.get_by_role("button", name="✔️").click()


        # Verify the status is approved
        expect(row.get_by_role("option", name="Approved")).to_be_selected()

        page.screenshot(path="jules-scratch/verification/verification.png")
    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)