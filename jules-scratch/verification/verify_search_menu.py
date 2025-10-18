from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Go to the index page
    page.goto("http://localhost:8080")

    # The search menu item should be visible
    search_menu_item = page.locator('[data-test=menu-search]')
    assert search_menu_item.is_visible()

    # The other menu items should not be visible
    libraries_menu_item = page.locator('[data-test=menu-libraries]')
    assert not libraries_menu_item.is_visible()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)