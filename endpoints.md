# Endpoint Annotations Guide

This document provides guidelines for properly annotating REST API endpoints in the library application.

## Table of Contents
- [Authorization Annotations](#authorization-annotations)
- [Common Mistakes](#common-mistakes)
- [Security Configuration](#security-configuration)
- [Examples](#examples)
- [Testing Authorization](#testing-authorization)

---

## Authorization Annotations

### hasAuthority() vs hasRole()

**IMPORTANT: Always use `hasAuthority()` in this application, NOT `hasRole()`**

#### Why?

The application stores roles in the database as:
- `"LIBRARIAN"`
- `"USER"`

Spring Security's behavior:
- `hasRole('LIBRARIAN')` → looks for authority `"ROLE_LIBRARIAN"` (adds ROLE_ prefix automatically)
- `hasAuthority('LIBRARIAN')` → looks for authority `"LIBRARIAN"` (exact match, no prefix)

Since our database and `CustomUserDetailsService` use role names without the `ROLE_` prefix, we must use `hasAuthority()`.

### Correct Pattern

```java
@RestController
@RequestMapping("/api/something")
public class SomeController {

    // ✅ CORRECT - Public endpoint (no annotation)
    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("Public data");
    }

    // ✅ CORRECT - Any authenticated user
    @GetMapping("/authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> authenticatedEndpoint() {
        return ResponseEntity.ok("Authenticated data");
    }

    // ✅ CORRECT - Librarian only (use hasAuthority)
    @PostMapping("/admin-action")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin action completed");
    }

    // ✅ CORRECT - Multiple roles (OR condition)
    @GetMapping("/multi-role")
    @PreAuthorize("hasAuthority('LIBRARIAN') or hasAuthority('USER')")
    public ResponseEntity<String> multiRoleEndpoint() {
        return ResponseEntity.ok("Multi-role data");
    }
}
```

### Wrong Pattern

```java
@RestController
@RequestMapping("/api/something")
public class SomeController {

    // ❌ WRONG - Will fail! Looks for "ROLE_LIBRARIAN"
    @PostMapping("/admin-action")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("This will return 403!");
    }

    // ❌ WRONG - hasRole with ROLE_ prefix is redundant
    @PostMapping("/admin-action")
    @PreAuthorize("hasRole('ROLE_LIBRARIAN')")
    public ResponseEntity<String> redundantEndpoint() {
        // This looks for "ROLE_ROLE_LIBRARIAN"!
        return ResponseEntity.ok("This will return 403!");
    }
}
```

---

## Common Mistakes

### 1. Using hasRole() Instead of hasAuthority()

**Problem:**
```java
@PreAuthorize("hasRole('LIBRARIAN')")  // ❌ Wrong!
```

**Solution:**
```java
@PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct!
```

### 2. Inconsistent Security Between SecurityConfig and @PreAuthorize

The `SecurityConfig.java` uses `hasAuthority()` for path-based security:

```java
.requestMatchers("/apply/api/**").hasAuthority("LIBRARIAN")
```

Your `@PreAuthorize` annotations should match this pattern.

### 3. Not Checking Authentication

For endpoints that don't require a specific role but need authentication:

```java
@PreAuthorize("isAuthenticated()")  // ✅ Correct!
```

### 4. Forgetting @EnableMethodSecurity

The application needs this in `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ✅ Required for @PreAuthorize to work!
public class SecurityConfig {
    // ...
}
```

---

## Security Configuration

### Current Setup

**Roles in Database:**
- `LIBRARIAN` - Full administrative access
- `USER` - Regular user access

**Authority Granting (CustomUserDetailsService):**
```java
.authorities(user.getRoles().stream()
    .map(role -> new SimpleGrantedAuthority(role.getName()))  // "LIBRARIAN" or "USER"
    .collect(Collectors.toList()))
```

**Path-Based Security (SecurityConfig):**
```java
.requestMatchers("/apply/api/**").hasAuthority("LIBRARIAN")  // Path requires LIBRARIAN
.requestMatchers("/api/user-settings").authenticated()        // Path requires any auth
.requestMatchers("/api/public/**").permitAll()               // Public paths
```

**Method-Based Security (@PreAuthorize):**
```java
@PreAuthorize("hasAuthority('LIBRARIAN')")  // Method requires LIBRARIAN
@PreAuthorize("isAuthenticated()")           // Method requires any auth
```

---

## Examples

### Photo Backup Endpoints

```java
@RestController
@RequestMapping("/api/photo-backup")
public class PhotoBackupController {

    // Public - anyone can view stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBackupStats() {
        // No @PreAuthorize needed - public
    }

    // Public - anyone can view photo list
    @GetMapping("/photos")
    public ResponseEntity<List<Map<String, Object>>> getAllPhotosWithBackupStatus() {
        // No @PreAuthorize needed - public
    }

    // Librarian only - administrative action
    @PostMapping("/backup-all")
    @PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct!
    public ResponseEntity<Map<String, Object>> backupAllPhotos() {
        // Only librarians can trigger backups
    }

    // Librarian only - administrative action
    @PostMapping("/backup/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct!
    public ResponseEntity<Map<String, Object>> backupPhoto(@PathVariable Long photoId) {
        // Only librarians can trigger individual backups
    }
}
```

### Import/Export Endpoints

```java
@RestController
@RequestMapping("/api/import")
public class ImportController {

    // Librarian only - data modification
    @PostMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct!
    public ResponseEntity<String> importJson(@RequestBody ImportRequestDto dto) {
        // Only librarians can import data
    }

    // Librarian only - sensitive data export
    @GetMapping("/json")
    @PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct!
    public ResponseEntity<ImportRequestDto> exportJson() {
        // Only librarians can export all data
    }
}
```

### User Management Endpoints

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Librarian only - view all users
    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<User>> getAllUsers() {
        // Only librarians can see all users
    }

    // Any authenticated user - view own profile
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getCurrentUser() {
        // Any logged-in user can view their own profile
    }

    // Librarian only - create users
    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Only librarians can create users
    }
}
```

---

## Testing Authorization

### Manual Testing Steps

1. **Test as Librarian:**
   - Log in as user with LIBRARIAN role
   - Access endpoint - should succeed
   - Check logs for successful authorization

2. **Test as Regular User:**
   - Log in as user with USER role
   - Access librarian-only endpoint - should get 403 Forbidden
   - Check logs for authorization denial

3. **Test as Unauthenticated:**
   - Log out
   - Access authenticated endpoint - should get 401 Unauthorized or redirect to login

### Debug Authorization Issues

If you get 403 Forbidden errors:

1. **Check the annotation:**
   ```java
   // Is it hasAuthority or hasRole?
   @PreAuthorize("hasAuthority('LIBRARIAN')")  // ✅ Correct
   @PreAuthorize("hasRole('LIBRARIAN')")       // ❌ Wrong
   ```

2. **Check user's authorities:**
   - Look at database: `SELECT * FROM role;`
   - Check what authorities are granted in `CustomUserDetailsService`
   - Log the user's authorities in the controller

3. **Check SecurityConfig:**
   - Ensure `@EnableMethodSecurity` is present
   - Check if path-based security conflicts with method-based

4. **Check logs:**
   ```
   Authorization denied on path uri=/api/endpoint: Access Denied
   ```
   This means the authorization annotation is being evaluated but failing.

---

## Quick Reference

| Access Level | Annotation |
|--------------|------------|
| Public (no auth required) | None |
| Any authenticated user | `@PreAuthorize("isAuthenticated()")` |
| Librarian only | `@PreAuthorize("hasAuthority('LIBRARIAN')")` |
| User only | `@PreAuthorize("hasAuthority('USER')")` |
| Librarian OR User | `@PreAuthorize("hasAuthority('LIBRARIAN') or hasAuthority('USER')")` |

---

## Related Files

- `SecurityConfig.java` - Path-based security configuration
- `CustomUserDetailsService.java` - Authority granting logic
- `Role.java` - Role entity (database model)
- Controllers with `@PreAuthorize` - Method-based security

---

## Best Practices

1. ✅ **Always use `hasAuthority()` in this application**
2. ✅ **Be explicit about security** - don't rely on implicit rules
3. ✅ **Test both positive and negative cases** - verify auth works AND fails appropriately
4. ✅ **Document why an endpoint requires certain permissions**
5. ✅ **Use meaningful HTTP status codes** - 401 for unauthenticated, 403 for unauthorized
6. ✅ **Log authorization decisions** - helps with debugging
7. ✅ **Keep SecurityConfig and @PreAuthorize in sync** - use same authority names

---

## Troubleshooting Checklist

- [ ] Using `hasAuthority()` instead of `hasRole()`?
- [ ] `@EnableMethodSecurity` present in SecurityConfig?
- [ ] Role names in database match authority strings exactly?
- [ ] CustomUserDetailsService granting authorities correctly?
- [ ] SecurityConfig path rules not conflicting with method security?
- [ ] User actually has the required role in database?
- [ ] Logged in with correct user account for testing?

---

**Last Updated:** 2025-11-15
**Related Issue:** Authorization fixes for photo backup endpoints
