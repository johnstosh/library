// (c) Copyright 2026 by Muczynski
import { useMutation } from '@tanstack/react-query'
import { api } from './client'

export type MailingListId =
  | 'LIVES_OF_SAINTS'
  | 'CHILDRENS_FICTION'
  | 'READ_ALOUD_BOOKS'
  | 'NEW_CATALOG_ENTRIES'
  | 'ADULT_FICTION'

export type MailingFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY'

export interface MailingListSignupRequest {
  email: string
  lists: MailingListId[]
  frequency: MailingFrequency
}

export interface MailingListSignupLine {
  list: MailingListId
  displayName: string
  frequency: MailingFrequency
  status: 'created' | 'resubscribed' | 'already_subscribed'
  message?: string
  sent: boolean
}

export interface MailingListSignupResponse {
  results: MailingListSignupLine[]
}

export function useJoinMailingLists() {
  return useMutation({
    mutationFn: (data: MailingListSignupRequest) =>
      api.post<MailingListSignupResponse>('/public/mailing-lists/subscriptions', data, {
        requireAuth: false,
      }),
  })
}
