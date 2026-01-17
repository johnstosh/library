# Database Upgrade Compatibility Analysis

**Date**: 2026-01-17
**Analysis**: Comparing dev branch database schema with library-main (production) schema
**Upgrade Path**: library-main → dev

## Executive Summary

⚠️ **CRITICAL ISSUE FOUND**: The upgrade from library-main to dev **will NOT go smoothly** without code fixes. The Library entity has field renames without corresponding `@Column` annotations, which will cause JPA to create new columns and leave existing data orphaned.

**Recommendation**: Add `@Column` annotations to preserve backward compatibility before deploying to production.

---

## Detailed Analysis

### 1. Tables Unchanged

These tables have no structural changes (same columns, same names):

| Table | Status | Notes |
|-------|--------|-------|
| `global_settings` | ✅ No changes | Identical in both versions |
| `applied` | ⚠️ Minor change | Password column length expanded from 60→64 (safe) |

---

### 2. Tables with Changes Handled by JPA

JPA will automatically handle these changes during deployment:

#### Password Column Expansions (Safe)
| Table | Column | Old Length | New Length | Impact |
|-------|--------|------------|------------|--------|
| `users` | `password` | 60 | 64 | ✅ Safe - JPA expands column, data preserved |
| `applied` | `password` | 60 | 64 | ✅ Safe - JPA expands column, data preserved |

#### New Columns Added (Safe)
These columns will be automatically added by JPA with NULL values for existing rows:

| Table | New Columns in Dev | Impact |
|-------|-------------------|--------|
| `author` | `grokipediaUrl`, `lastModified` | ✅ Safe - columns added as nullable |
| `book` | `grokipediaUrl`, `freeTextUrl` | ✅ Safe - columns added as nullable |
| `loan` | `lastModified` | ✅ Safe - columns added as nullable |
| `user` | `lastModified` | ✅ Safe - columns added as nullable |
| `photo` | `dateTaken` | ✅ Safe - columns added as nullable |

#### New Indexes Added (Safe)
These indexes will be automatically created by JPA:

| Table | New Indexes | Impact |
|-------|------------|--------|
| `users` | `idx_user_username`, `idx_user_sso` | ✅ Safe - improves query performance |
| `loan` | `idx_loan_book_return`, `idx_loan_user_return`, `idx_loan_due_date` | ✅ Safe - improves query performance |
| `author` | `idx_author_name` | ✅ Safe - improves query performance |
| `book` | `idx_book_title` | ✅ Safe - improves query performance |

---

### 3. Tables with Unused Old Rows (Orphaned Data)

#### ❌ CRITICAL: Library Table Column Rename Issue

**Problem**: Field names changed without `@Column` annotations to preserve database column names.

| Version | Field Names | Database Columns |
|---------|-------------|------------------|
| **library-main (prod)** | `name`, `hostname` | `name`, `hostname` |
| **dev** | `branchName`, `librarySystemName` | ⚠️ `branchname`, `librarySystemName` (NEW COLUMNS!) |

**What Will Happen**:
1. JPA will create NEW columns: `branchname` and `librarysystemname` (or with underscores depending on naming strategy)
2. Existing data in `name` and `hostname` columns will remain but become orphaned
3. All library records will have NULL values in the new columns
4. Application will display blank branch names

**Impact**: 🔴 **BREAKING** - All library/branch data will be lost

**Solution Required**: Add `@Column` annotations in `Library.java`:
```java
@Column(name = "name")
private String branchName;

// Note: hostname field was removed in dev, no mapping needed
// Old data in hostname column will remain but be unused
```

---

### 4. Entity Renames with Table Annotations (Safe)

#### ✅ Role → Authority Entity Rename (Handled Correctly)

| Version | Entity Name | Table Name |
|---------|-------------|------------|
| **library-main (prod)** | `Role` | `role` (no annotation, defaults to entity name) |
| **dev** | `Authority` | `role` (explicit `@Table(name = "role")` annotation) |

**Status**: ✅ **SAFE** - The dev version correctly uses `@Table(name = "role")` annotation to preserve the table name.

**Table Structure**: Both versions have identical columns (`id`, `name`).

---

### 5. Field Renames with Join Table Preservation (Safe)

#### User Entity: roles → authorities Field Rename

| Version | Field Name | Type | Join Table |
|---------|------------|------|------------|
| **library-main** | `roles` | `Set<Role>` | `users_roles` |
| **dev** | `authorities` | `Set<Authority>` | `users_roles` |

**Status**: ✅ **SAFE** - Both versions explicitly specify the join table name in `@JoinTable` annotation:
```java
@JoinTable(
    name = "users_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
```

The field name change doesn't affect the database because the join table name and column names are explicitly defined.

---

### 6. Removed Columns (Unused Data)

These columns exist in production but are removed in dev:

| Table | Column | Status |
|-------|--------|--------|
| `library` | `hostname` | ⚠️ Column will remain in database but unused (orphaned data) |

**Impact**: Non-breaking, but database will have unused column with old data.

---

### 7. Missing Annotations Analysis

#### ❌ Library Entity - Missing @Column Annotations

**File**: `src/main/java/com/muczynski/library/domain/Library.java`

**Current Code** (dev):
```java
private String branchName;
private String librarySystemName;
```

**Required Code** for backward compatibility:
```java
@Column(name = "name")
private String branchName;

private String librarySystemName;  // New field, will create new column
```

**Explanation**:
- The `branchName` field maps to the old `name` column
- The `librarySystemName` field is new and will create a new column
- The old `hostname` column will remain but be unused

---

## Migration Impact Summary

### What Will Work Automatically

1. ✅ Password column length expansion (users, applied)
2. ✅ New columns added with NULL defaults
3. ✅ New indexes created
4. ✅ Role → Authority entity rename (table name preserved)
5. ✅ User.roles → User.authorities field rename (join table preserved)
6. ✅ New lifecycle callbacks (@PrePersist, @PreUpdate)

### What Will Fail

1. ❌ **Library/Branch data will be lost** due to field rename without @Column annotation
   - All branch names will become NULL
   - Application will fail to display library/branch information
   - Books will still reference libraries by ID, but display will show blank names

---

## Recommended Action Plan

### Before Deployment

1. **Fix Library Entity**:
   ```java
   @Entity
   public class Library {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(name = "name")  // ← ADD THIS
       private String branchName;

       private String librarySystemName;  // New column will be created
   }
   ```

2. **Update Documentation**:
   - Update `feature-design-libraries.md` to remove `hostname` field (line 27)
   - Clarify that `librarySystemName` is a new field

3. **Test Migration**:
   - Export production database backup
   - Test upgrade on staging environment
   - Verify branch names display correctly
   - Verify books still reference correct libraries

### After Deployment

1. **Data Cleanup** (optional):
   - Remove unused `hostname` column (requires manual SQL migration)
   - Populate `librarySystemName` with appropriate values

---

## Code Changes Required

### File: src/main/java/com/muczynski/library/domain/Library.java

**Before**:
```java
private String branchName;
private String librarySystemName;
```

**After**:
```java
@Column(name = "name")
private String branchName;

private String librarySystemName;
```

---

## Testing Checklist

- [ ] Unit tests pass after adding @Column annotation
- [ ] Integration tests verify library data persists
- [ ] Migration tested on copy of production database
- [ ] Branch names display correctly in UI after upgrade
- [ ] Books still reference correct libraries
- [ ] Statistics (book counts, loan counts) still work

---

## Conclusion

The database upgrade from library-main to dev **requires code fixes** before deployment. The primary issue is the Library entity field rename without proper `@Column` annotations.

**Risk Level**: 🔴 **HIGH** - Without fixes, library/branch data will be lost.

**Fix Complexity**: ⚠️ **LOW** - Simple one-line annotation addition.

**Recommendation**: Apply the `@Column(name = "name")` annotation to `Library.branchName` field before deploying to production.
