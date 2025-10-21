from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080/apply-for-card.html")

    print(page.content())

    expect(page).to_have_title("Apply for a Library Card")

    page.screenshot(path="jules-scratch/verification/apply-for-card.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)