# Vatican.va Provider Debug Notes

## Test Encyclicals

Testing with well-known papal encyclicals to verify the provider correctly finds free text versions.

### 1. Rerum Novarum
- **Author**: Pope Leo XIII
- **Year**: 1891
- **Subject**: On Capital and Labor
- **Manual Check**: Available at vatican.va
- **Expected URL**: https://www.vatican.va/content/leo-xiii/en/encyclicals/documents/hf_l-xiii_enc_15051891_rerum-novarum.html
- **Provider Result**: FOUND ✓

### 2. Humanae Vitae
- **Author**: Pope Paul VI
- **Year**: 1968
- **Subject**: On Human Life
- **Manual Check**: Available at vatican.va
- **Expected URL**: https://www.vatican.va/content/paul-vi/en/encyclicals/documents/hf_p-vi_enc_25071968_humanae-vitae.html
- **Provider Result**: FOUND ✓

### 3. Laudato Si'
- **Author**: Pope Francis
- **Year**: 2015
- **Subject**: On Care for Our Common Home
- **Manual Check**: Available at vatican.va
- **Expected URL**: https://www.vatican.va/content/francesco/en/encyclicals/documents/papa-francesco_20150524_enciclica-laudato-si.html
- **Provider Result**: FOUND ✓

### 4. Fides et Ratio
- **Author**: Pope John Paul II
- **Year**: 1998
- **Subject**: On Faith and Reason
- **Manual Check**: Available at vatican.va
- **Expected URL**: https://www.vatican.va/content/john-paul-ii/en/encyclicals/documents/hf_jp-ii_enc_14091998_fides-et-ratio.html
- **Provider Result**: FOUND ✓

### 5. Deus Caritas Est
- **Author**: Pope Benedict XVI
- **Year**: 2005
- **Subject**: God is Love
- **Manual Check**: Available at vatican.va
- **Expected URL**: https://www.vatican.va/content/benedict-xvi/en/encyclicals/documents/hf_ben-xvi_enc_20051225_deus-caritas-est.html
- **Provider Result**: FOUND ✓

---

## Provider Issues Found

### Issue 1: Vatican.va search page requires JavaScript
The original approach used Vatican's search page (`/content/vatican/en/search.html?q=...`) which returns a JavaScript-rendered page. RestTemplate cannot execute JavaScript.

**Solution**: Instead of using the search page, scrape the encyclicals index pages for each pope:
- URL pattern: `https://www.vatican.va/content/{pope-slug}/en/encyclicals.index.html`
- Each pope has a unique slug (e.g., "francesco", "benedict-xvi", "john-paul-ii")

### Issue 2: Encyclical titles wrapped in italic tags
Some titles on the index pages are wrapped in `<i>` tags (e.g., `<i>Deus caritas est</i>`), which broke the regex pattern that expected plain text.

**Solution**: Updated regex to handle optional `<i>` tags:
```
<a[^>]+href="...">\\s*(?:<i>)?([^<]+)(?:</i>)?
```

### Issue 3: Titles include dates in parentheses
Index page titles include publication dates (e.g., "Humanae Vitae (July 25, 1968)"), which could interfere with title matching.

**Solution**: Strip trailing parenthetical dates before matching:
```java
linkText = linkText.replaceAll("\\s*\\([^)]+\\)\\s*$", "").trim();
```

---

## Fixes Applied

1. **Complete rewrite of VaticanProvider**:
   - Added pope name → URL slug mapping for 9 popes
   - Scrapes encyclicals index pages instead of using search
   - If pope is known from author name, searches that pope first
   - Falls back to searching all recent popes

2. **Regex improvements**:
   - Handles italic `<i>` tags around titles
   - Strips date suffixes from link text

3. **Pope identification**:
   - Supports various name formats (e.g., "Pope Francis", "Francis", "Pope John Paul II", "JP2")

---

## Final Test Results

| Encyclical | Author | Found | URL |
|------------|--------|-------|-----|
| Rerum Novarum | Leo XIII | ✓ | .../hf_l-xiii_enc_15051891_rerum-novarum.html |
| Humanae Vitae | Paul VI | ✓ | .../hf_p-vi_enc_25071968_humanae-vitae.html |
| Laudato Si' | Francis | ✓ | .../papa-francesco_20150524_enciclica-laudato-si.html |
| Fides et Ratio | John Paul II | ✓ | .../hf_jp-ii_enc_14091998_fides-et-ratio.html |
| Deus Caritas Est | Benedict XVI | ✓ | .../hf_ben-xvi_enc_20051225_deus-caritas-est.html |

**Success Rate: 5/5 (100%)**
