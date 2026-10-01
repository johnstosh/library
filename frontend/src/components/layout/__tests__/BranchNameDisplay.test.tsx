// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BranchNameDisplay } from '../BranchNameDisplay'

describe('BranchNameDisplay', () => {
  it('shows the app name and sub-name', () => {
    render(
      <BranchNameDisplay
        name="St. Martin de Porres"
        subName="Sacred Heart Library System"
        dataTest="branch-name"
      />,
    )

    const heading = screen.getByTestId('branch-name')
    expect(heading).toContainElement(screen.getByTestId('app-name'))
    expect(screen.getByTestId('app-name')).toHaveTextContent('St. Martin de Porres')
    expect(screen.getByTestId('app-sub-name')).toHaveTextContent('Sacred Heart Library System')
  })

  it('omits the sub-name line when it is blank', () => {
    render(<BranchNameDisplay name="Library" subName="" dataTest="branch-name" />)

    expect(screen.getByTestId('app-name')).toHaveTextContent('Library')
    expect(screen.queryByTestId('app-sub-name')).not.toBeInTheDocument()
  })
})
