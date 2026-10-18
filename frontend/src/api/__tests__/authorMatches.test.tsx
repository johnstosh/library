// (c) Copyright 2025 by Muczynski
import { renderHook, waitFor } from '@testing-library/react'
import { act } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../client'
import { authorSearchTerm, useAuthorNameSearch } from '../authorMatches'

vi.mock('../client', () => ({
  api: {
    get: vi.fn(),
  },
}))

afterEach(() => {
  vi.clearAllMocks()
})

function deferred<T>() {
  let resolve: (value: T) => void = () => {}
  let reject: (reason?: unknown) => void = () => {}
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('authorSearchTerm', () => {
  it('waits until the name has 3 characters', () => {
    expect(authorSearchTerm('  au ')).toBeNull()
    expect(authorSearchTerm(' Aug ')).toBe('Aug')
  })
})

describe('useAuthorNameSearch', () => {
  it('does not call the server before 3 characters', () => {
    renderHook(() => useAuthorNameSearch('au'))
    expect(api.get).not.toHaveBeenCalled()
  })

  it('keeps one request in flight and follows up once with the latest text', async () => {
    const first = deferred<unknown[]>()
    const second = deferred<unknown[]>()
    vi.mocked(api.get)
      .mockImplementationOnce(() => first.promise)
      .mockImplementationOnce(() => second.promise)

    const { result, rerender } = renderHook(
      (name: string) => useAuthorNameSearch(name),
      { initialProps: 'Aug' },
    )

    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))
    expect(api.get).toHaveBeenCalledWith('/authors/name-matches?name=Aug')
    expect(result.current.isFetching).toBe(true)

    rerender('Augu')
    rerender('August')
    expect(api.get).toHaveBeenCalledTimes(1)

    await act(async () => {
      first.resolve([{ id: 1, name: 'Augur' }])
    })

    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2))
    expect(api.get).toHaveBeenLastCalledWith('/authors/name-matches?name=August')
    expect(result.current.data).toEqual([])

    await act(async () => {
      second.resolve([{ id: 2, name: 'Augustine' }])
    })

    await waitFor(() => expect(result.current.isFetching).toBe(false))
    expect(result.current.data).toEqual([{ id: 2, name: 'Augustine' }])
    expect(api.get).toHaveBeenCalledTimes(2)
  })
})
