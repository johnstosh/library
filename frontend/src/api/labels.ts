// (c) Copyright 2025 by Muczynski

export interface LabelsPdfResult {
  blob: Blob
  filename: string
}

export async function generateLabelsPdf(bookIds: number[]): Promise<LabelsPdfResult> {
  const params = bookIds.map((id) => `bookIds=${id}`).join('&')
  const response = await fetch(`/api/labels/generate?${params}`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to generate labels PDF')
  }

  // Extract filename from Content-Disposition header
  const contentDisposition = response.headers.get('Content-Disposition')
  let filename = 'book-labels.pdf'
  if (contentDisposition) {
    const filenameMatch = contentDisposition.match(/filename="?([^";\n]+)"?/)
    if (filenameMatch) {
      filename = filenameMatch[1]
    }
  }

  const blob = await response.blob()
  return { blob, filename }
}
