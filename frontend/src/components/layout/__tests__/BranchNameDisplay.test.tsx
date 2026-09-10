// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BranchNameDisplay } from '../BranchNameDisplay'

const { isDevSite } = vi.hoisted(() => ({
  isDevSite: vi.fn(() => false),
}))

vi.mock('@/utils/environment', () => ({
  isDevSite,
}))

describe('BranchNameDisplay', () => {
  afterEach(() => {
    isDevSite.mockReturnValue(false)
  })

  it('shows the branch and system name', () => {
    render(
      <BranchNameDisplay
        branchName="St. Martin de Porres"
        librarySystemName="Sacred Heart Library System"
        dataTest="branch-name"
      />,
    )

    const heading = screen.getByTestId('branch-name')
    expect(heading).toHaveTextContent('The St. Martin de Porres Branch')
    expect(heading).toHaveTextContent('of the Sacred Heart Library System')
    expect(heading).not.toHaveTextContent('DEV')
  })

  it('says DEV instead of the branch and system name on the dev site', () => {
    isDevSite.mockReturnValue(true)

    render(
      <BranchNameDisplay
        branchName="St. Martin de Porres"
        librarySystemName="Sacred Heart Library System"
        dataTest="branch-name"
      />,
    )

    const heading = screen.getByTestId('branch-name')
    expect(heading).toHaveTextContent('DEV')
    expect(heading).not.toHaveTextContent('St. Martin de Porres')
    expect(heading).not.toHaveTextContent('Sacred Heart Library System')
  })
})
