// (c) Copyright 2025 by Muczynski
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { AbeSellerDto, BookListingDto } from '@/types/dtos'

export function useSellers() {
  return useQuery({
    queryKey: queryKeys.sellers.list(),
    queryFn: () => api.get<AbeSellerDto[]>('/sellers'),
  })
}

export function useSeller(id: number) {
  return useQuery({
    queryKey: queryKeys.sellers.detail(id),
    queryFn: () => api.get<AbeSellerDto>(`/sellers/${id}`),
    enabled: Number.isFinite(id) && id > 0,
  })
}

export function useUpdateAdditionalShipping() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, additionalShippingDollars }: { id: number; additionalShippingDollars: number | null }) =>
      api.put<AbeSellerDto>(`/sellers/${id}/additional-shipping`, { additionalShippingDollars }),
    onSuccess: (seller) => {
      queryClient.setQueryData(queryKeys.sellers.detail(seller.id), seller)
      queryClient.invalidateQueries({ queryKey: queryKeys.sellers.list() })
    },
  })
}

export function fetchListingsForBooks(bookIds: number[]) {
  return api.post<BookListingDto[]>('/listings/by-book-ids', bookIds)
}
