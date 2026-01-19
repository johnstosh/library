// (c) Copyright 2025 by Muczynski

export async function generateLabelsPdf(bookIds: number[]): Promise<Blob> {
  const params = bookIds.map((id) => `bookIds=${id}`).join('&')
  const response = await fetch(`/api/labels/generate?${params}`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to generate labels PDF')
  }

  return response.blob()
}
