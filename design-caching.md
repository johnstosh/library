# Caching System Design

This document describes how the frontend caching system works and the requirements for entity types to participate in it.

## Overview

The caching system uses **lastModified-based cache invalidation** to minimize data transfer while ensuring fresh data. Instead of fetching full entity objects on every request, the system:

1. Fetches lightweight summaries (ID + lastModified timestamp)
2. Compares with cached entities
3. Only fetches entities that have actually changed

This approach significantly reduces bandwidth and improves performance, especially for large datasets.

## Architecture

### Backend Requirements

For an entity type to participate in caching, the backend must provide:

#### 1. Summary DTO
A lightweight DTO containing only:
- `id` - the entity's unique identifier
- `lastModified` - timestamp of last modification (ISO-8601 string)

Example:
```java
public class BookSummaryDto {
    private Long id;
    private String lastModified;
}
```

#### 2. Summaries Endpoint
Returns all entity summaries for caching:
```
GET /api/{entity}/summaries -> List<EntitySummaryDto>
```

#### 3. Filter Summary Endpoints
For each filter view, return summaries (not full DTOs):
```
GET /api/{entity}/without-loc -> List<EntitySummaryDto>
GET /api/{entity}/most-recent-day -> List<EntitySummaryDto>
```

#### 4. Batch Fetch Endpoint
Fetches multiple entities by their IDs:
```
POST /api/{entity}/by-ids -> List<EntityDto>
Body: [1, 2, 3, ...]
```

#### 5. lastModified Field
The entity must have a `lastModified` field that:
- Updates automatically on any change
- Uses consistent ISO-8601 format (e.g., "2025-12-06T14:30:00")
- Is included in the DTO

### Frontend Requirements

#### 1. Query Keys
Define query keys for caching:
```typescript
export const queryKeys = {
  books: {
    all: ['books'] as const,
    summaries: () => [...queryKeys.books.all, 'summaries'] as const,
    filterSummaries: (filter: string) => [...queryKeys.books.all, 'filterSummaries', filter] as const,
    byIds: (ids: number[], filter?: string) => [...queryKeys.books.all, 'byIds', ids.join(','), filter] as const,
    detail: (id: number) => [...queryKeys.books.all, 'detail', id] as const,
  },
}
```

#### 2. Hook Pattern
Implement the caching hook with these steps:

```typescript
export function useEntities(filter?: string) {
  const queryClient = useQueryClient()

  // Step 1: Fetch summaries from appropriate endpoint
  const { data: summaries } = useQuery({
    queryKey: filter ? queryKeys.entities.filterSummaries(filter) : queryKeys.entities.summaries(),
    queryFn: () => api.get<EntitySummaryDto[]>(getFilterEndpoint(filter)),
    staleTime: 0,
    refetchOnMount: true,
  })

  // Step 2: Determine which entities need fetching
  const entitiesToFetch = useMemo(() => {
    if (!summaries) return []
    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<EntityDto>(queryKeys.entities.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient])

  // Step 3: Batch fetch changed entities
  const { data: fetchedEntities } = useQuery({
    queryKey: queryKeys.entities.byIds(entitiesToFetch, filter),
    queryFn: () => api.post<EntityDto[]>('/entities/by-ids', entitiesToFetch),
    enabled: summaries !== undefined && entitiesToFetch.length > 0,
  })

  // Step 4: Populate individual caches
  React.useEffect(() => {
    fetchedEntities?.forEach((entity) => {
      queryClient.setQueryData(queryKeys.entities.detail(entity.id), entity)
    })
  }, [fetchedEntities, queryClient])

  // Step 5: Build final list from fetched + cached
  const allEntities = useMemo(() => {
    // ... combine fetchedEntities with cached entities
  }, [summaries, fetchedEntities, ...])

  return { data: allEntities, isLoading: ... }
}
```

## Currently Implemented

### Books
- **Summaries Endpoint**: `GET /api/books/summaries`
- **Filter Endpoints**: All return `BookSummaryDto[]`:
  - `/api/books/most-recent-day`
  - `/api/books/without-loc`
  - `/api/books/by-3letter-loc`
  - `/api/books/without-grokipedia`
- **Batch Fetch**: `POST /api/books/by-ids`

### Authors
- **Summaries Endpoint**: `GET /api/authors/summaries`
- **Filter Endpoints**: All return `AuthorSummaryDto[]`:
  - `/api/authors/without-description`
  - `/api/authors/zero-books`
  - `/api/authors/without-grokipedia`
  - `/api/authors/most-recent-day`
- **Batch Fetch**: `POST /api/authors/by-ids`

## Cache Invalidation

Invalidate summaries when entities change:

```typescript
// On create
queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })

// On update
queryClient.setQueryData(queryKeys.books.detail(id), updatedBook)
queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })

// On delete
queryClient.removeQueries({ queryKey: queryKeys.books.detail(id) })
queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
```

## Benefits

1. **Reduced bandwidth**: Only changed entities are fetched
2. **Faster page loads**: Cached entities display immediately
3. **Consistent state**: lastModified ensures no stale data
4. **Works with filters**: Each filter uses the same caching pattern

## Considerations

- Summary endpoints must be fast (index-only queries if possible)
- lastModified must update reliably on all changes
- Batch endpoint should handle large ID lists efficiently
- Consider pagination for very large datasets
