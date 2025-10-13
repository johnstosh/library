from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch()
    page = browser.new_page()
    page.goto("http://localhost:8080")

    # Login
    page.fill("#username", "librarian")
    page.fill("#password", "password")
    page.click("text=Login")

    # Wait for main content to load
    page.wait_for_selector("#main-content")

    # Add a library
    page.fill("#new-library-name", "Main Library")
    page.fill("#new-library-hostname", "main.library.com")
    page.click("text=Add Library")

    # Add an author
    page.fill("#new-author-name", "J.D. Salinger")
    page.click("text=Add Author")

    page.screenshot(path="jules-scratch/verification/verification.png")
    browser.close()

with sync_playwright() as playwright:
    run(playwright)