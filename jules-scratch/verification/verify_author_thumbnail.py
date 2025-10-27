from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://localhost:8080")

    # Login
    page.wait_for_selector('[data-test="menu-login"]')
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()

    # Generate test data
    page.wait_for_selector('[data-test="menu-test-data"]')
    page.locator('[data-test="menu-test-data"]').click()
    page.wait_for_selector('[data-test="generate-test-data-btn"]')
    page.locator('[data-test="generate-test-data-btn"]').click()
    page.wait_for_timeout(5000)

    # Go to authors page
    page.locator('[data-test="menu-authors"]').click()
    page.wait_for_selector('[data-test="author-item"]', timeout=60000)


    # Edit author and add photo
    page.locator('[data-test="edit-author-btn"]').first.click()
    page.wait_for_selector('[data-test="add-author-photo-btn"]')
    page.locator('[data-test="add-author-photo-btn"]').click()
    page.set_input_files('input[type="file"]', 'src/test/resources/test-images/test-image.jpg')
    page.wait_for_selector('[data-test="author-photo"]')


    # Go back to authors page
    page.locator('[data-test="menu-authors"]').click()
    page.wait_for_selector('[data-test="author-photo-cell"]')

    page.screenshot(path="jules-scratch/verification/verification.png")
    browser.close()