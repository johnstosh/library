// (c) Copyright 2025 by Muczynski
import { renderHook, waitFor } from '@testing-library/react'
import { act } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../client'
import { checkoutSearchTerms, useCheckoutBookSearch } from '../checkoutBooks'

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

describe('checkoutSearchTerms', () => {
  it('waits until one field has 3 characters and drops shorter fields', () => {
    expect(checkoutSearchTerms({ title: 'ab', author: 'cd', locNumber: 'ef' })).toBeNull()
    expect(checkoutSearchTerms({ title: ' mob ', author: 'a', locNumber: '  ' })).toEqual({
      title: 'mob',
      author: '',
      locNumber: '',
    })
  })
})

describe('useCheckoutBookSearch', () => {
  it('does not call the server before 3 characters', () => {
    renderHook(() => useCheckoutBookSearch({ title: 'ab', author: '', locNumber: '' }))
    expect(api.get).not.toHaveBeenCalled()
  })

  it('keeps one request in flight and follows up once with the latest text', async () => {
    const first = deferred<unknown[]>()
    const second = deferred<unknown[]>()
    vi.mocked(api.get)
      .mockImplementationOnce(() => first.promise)
      .mockImplementationOnce(() => second.promise)

    const { result, rerender } = renderHook(
      (filters: { title: string; author: string; locNumber: string }) => useCheckoutBookSearch(filters),
      { initialProps: { title: 'mob', author: '', locNumber: '' } },
    )

    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))
    expect(api.get).toHaveBeenCalledWith('/books/checkout-matches?title=mob')
    expect(result.current.isFetching).toBe(true)

    rerender({ title: 'moby', author: '', locNumber: '' })
    rerender({ title: 'moby d', author: '', locNumber: '' })
    expect(api.get).toHaveBeenCalledTimes(1)

    await act(async () => {
      first.resolve([{ id: 1, title: 'Moby', author: 'Melville' }])
    })

    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2))
    expect(api.get).toHaveBeenLastCalledWith('/books/checkout-matches?title=moby+d')
    expect(result.current.data).toEqual([])

    await act(async () => {
      second.resolve([{ id: 2, title: 'Moby Dick', author: 'Melville' }])
    })

    await waitFor(() => expect(result.current.isFetching).toBe(false))
    expect(result.current.data).toEqual([{ id: 2, title: 'Moby Dick', author: 'Melville' }])
    expect(api.get).toHaveBeenCalledTimes(2)
  })
})
