from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch()
    context = browser.new_context()
    page = context.new_page()

    # Capture console logs
    console_logs = []
    page.on("console", lambda msg: console_logs.append(msg.text))

    page.goto("http://localhost:8080/apply-for-card.html")

    # Check that the librarian section is not visible
    librarian_section = page.locator("#librarian-section")
    expect(librarian_section).not_to_be_visible()

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

    # Check for unhandled promise rejections
    for log in console_logs:
        if "Uncaught (in promise)" in log:
            raise Exception("Unhandled promise rejection found in console logs")

with sync_playwright() as playwright:
    run(playwright)