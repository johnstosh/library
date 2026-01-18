// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import type { LoanDto } from '@/types/dtos'

// Hook to get loans (with optional filter for showing all or just active)
export function useLoans(showAll = false) {
  return useQuery({
    queryKey: queryKeys.loans.list(showAll),
    queryFn: async () => {
      const endpoint = showAll ? '/loans?showAll=true' : '/loans'
      console.log('[useLoans] Fetching loans from:', endpoint)
      const loans = await api.get<LoanDto[]>(endpoint)
      console.log('[useLoans] Received loans:', loans.length, 'items')
      if (loans.length > 0) {
        console.log('[useLoans] First loan:', loans[0])
      }
      return loans
    },
    staleTime: 1000 * 60 * 2, // 2 minutes - loans change frequently
  })
}

// Hook to get a single loan
export function useLoan(id: number) {
  return useQuery({
    queryKey: queryKeys.loans.detail(id),
    queryFn: () => api.get<LoanDto>(`/loans/${id}`),
    enabled: !!id,
  })
}

// Hook to checkout a book (create a loan)
export function useCheckoutBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: { bookId: number; userId: number; loanDate?: string; dueDate?: string }) =>
      api.post<LoanDto>('/loans/checkout', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.loans.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Hook to return a book (update loan with return date)
export function useReturnBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (loanId: number) => api.put<LoanDto>(`/loans/return/${loanId}`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.loans.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}

// Hook to delete a loan
export function useDeleteLoan() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/loans/${id}`),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.loans.detail(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.loans.all })
    },
  })
}
