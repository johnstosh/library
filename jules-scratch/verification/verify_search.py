from playwright.sync_api import sync_playwright

from playwright.sync_api import expect

def run(playwright):
    browser = playwright.chromium.launch()
    page = browser.new_page()
    page.goto("http://localhost:8080")
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("user")
    page.locator('[data-test="login-password"]').fill("password")
    page.locator('[data-test="login-submit"]').click()
    expect(page.locator('[data-test="main-content"]')).to_be_visible()
    page.locator('[data-test="menu-search"]').click()
    page.wait_for_selector('input[placeholder="Search books and authors..."]')
    page.get_by_placeholder("Search books and authors...").fill("a")
    page.get_by_role("button", name="Search").click()
    page.screenshot(path="jules-scratch/verification/search.png")
    browser.close()

with sync_playwright() as playwright:
    run(playwright)