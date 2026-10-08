// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { authorNeedsCanonicalName, toCanonicalAuthorName } from '@/utils/canonicalAuthorName'
import { titleNeedsChicagoCase, toChicagoTitleCase } from '@/utils/chicagoTitleCase'
import { applyChipFilters, defaultBookChipFilters } from '@/utils/bookChipFilters'
import type { BookDto } from '@/types/dtos'

const DOMINIC = 'The History of St. Dominic: Founder of the Friars Preachers'

describe('Chicago title case', () => {
  it('rewrites lower, upper, RDA, and every-word caps, including the subtitle', () => {
    expect(toChicagoTitleCase('the history of st. dominic: founder of the friars preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase('THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS')).toBe(DOMINIC)
    expect(toChicagoTitleCase('The history of St. Dominic : founder of the Friars Preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase('The History Of St. Dominic: Founder Of The Friars Preachers')).toBe(DOMINIC)
    expect(toChicagoTitleCase(DOMINIC)).toBe(DOMINIC)
    expect(titleNeedsChicagoCase(DOMINIC)).toBe(false)
    expect(titleNeedsChicagoCase('THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS')).toBe(true)
  })

  it('capitalizes the first subtitle word and lowers short prepositions', () => {
    expect(toChicagoTitleCase('of mice and men')).toBe('Of Mice and Men')
    expect(toChicagoTitleCase('gone with the wind')).toBe('Gone with the Wind')
    expect(toChicagoTitleCase('foo: the bar')).toBe('Foo: The Bar')
    expect(toChicagoTitleCase('world war ii')).toBe('World War II')
    expect(toChicagoTitleCase('the mix of things')).toBe('The Mix of Things')
    expect(toChicagoTitleCase('the lord of the rings, c. 2')).toBe('The Lord of the Rings, c. 2')
    expect(toChicagoTitleCase('THE LORD OF THE RINGS, C. 2')).toBe('The Lord of the Rings, c. 2')
  })

  it('capitalizes the word after a sentence break', () => {
    const council = 'The Seventh Ecumenical Council. The Second Council of Nice'
    expect(toChicagoTitleCase(council)).toBe(council)
    expect(toChicagoTitleCase('The Seventh Ecumenical Council. the Second Council of Nice')).toBe(council)
    expect(toChicagoTitleCase('the seventh ecumenical council. the second council of nice')).toBe(council)
    expect(titleNeedsChicagoCase(council)).toBe(false)
    expect(titleNeedsChicagoCase('The Seventh Ecumenical Council. the Second Council of Nice')).toBe(true)
    expect(toChicagoTitleCase('the end. of mice and men')).toBe('The End. Of Mice and Men')
    expect(toChicagoTitleCase('the world we live in. a history')).toBe('The World We Live In. A History')
    expect(toChicagoTitleCase('what is it? the answer')).toBe('What Is It? The Answer')
    expect(toChicagoTitleCase('stop! the book')).toBe('Stop! The Book')
    expect(toChicagoTitleCase('wait... the end')).toBe('Wait... The End')
    expect(toChicagoTitleCase('done. next-to last')).toBe('Done. Next-to Last')
  })

  it('leaves abbreviations and initials alone', () => {
    expect(toChicagoTitleCase('mr. and mrs. smith')).toBe('Mr. and Mrs. Smith')
    expect(toChicagoTitleCase('body, etc. from original mss.')).toBe('Body, Etc. From Original Mss.')
    expect(toChicagoTitleCase("pass from me,' etc., and against")).toBe("Pass from Me,' Etc., and Against")
    expect(toChicagoTitleCase('j. r. r. tolkien')).toBe('J. R. R. Tolkien')
    expect(toChicagoTitleCase('u.s. history')).toBe('U.S. History')
    expect(toChicagoTitleCase('volume ii. the council')).toBe('Volume II. The Council')
    expect(toChicagoTitleCase('world war ii.')).toBe('World War II.')
    expect(toChicagoTitleCase('i. the mystical explanation')).toBe('I. The Mystical Explanation')
    expect(toChicagoTitleCase('life of mrs. eliza a. seton')).toBe('Life of Mrs. Eliza A. Seton')
  })

  it('keeps catalog shapes that headline style would otherwise flatten', () => {
    expect(toChicagoTitleCase("The Soul's Journey into God; The Tree of Life; The Life of St. Francis")).toBe(
      "The Soul's Journey into God; The Tree of Life; The Life of St. Francis",
    )
    expect(toChicagoTitleCase('fabiola; or, the church of the catacombs')).toBe(
      'Fabiola; Or, The Church of the Catacombs',
    )
    expect(toChicagoTitleCase('all for jesus: or, the easy ways of divine love')).toBe(
      'All for Jesus: Or, The Easy Ways of Divine Love',
    )
    expect(toChicagoTitleCase('Sophocles II: Ajax, The Women of Trachis, Electra, Philoctetes')).toBe(
      'Sophocles II: Ajax, the Women of Trachis, Electra, Philoctetes',
    )
    expect(toChicagoTitleCase('st. martin de porres')).toBe('St. Martin de Porres')
    expect(toChicagoTitleCase('saint john baptist de la salle')).toBe('Saint John Baptist de la Salle')
    expect(toChicagoTitleCase('claude la colombière')).toBe('Claude La Colombière')
    expect(toChicagoTitleCase('ricordo di roma')).toBe('Ricordo di Roma')
    expect(toChicagoTitleCase("john paul ii's theology")).toBe("John Paul II's Theology")
    expect(toChicagoTitleCase('the rosary: the great weapon of the 21st century')).toBe(
      'The Rosary: The Great Weapon of the 21st Century',
    )
    expect(toChicagoTitleCase('blessed miguel pro: 20th-century mexican martyr')).toBe(
      'Blessed Miguel Pro: 20th-Century Mexican Martyr',
    )
    expect(toChicagoTitleCase('Head First HTML with CSS & XHTML')).toBe('Head First HTML with CSS & XHTML')
    expect(toChicagoTitleCase('SQL Pocket Guide')).toBe('SQL Pocket Guide')
    expect(toChicagoTitleCase('The Kanji ABC')).toBe('The Kanji ABC')
    expect(toChicagoTitleCase('The Miracle of Our Lady of Fatima (DVD)')).toBe(
      'The Miracle of Our Lady of Fatima (DVD)',
    )
    expect(toChicagoTitleCase('YOUCAT: Youth Catechism of the Catholic Church')).toBe(
      'YOUCAT: Youth Catechism of the Catholic Church',
    )
    expect(toChicagoTitleCase('mr. mcfadden\'s hallowe\'en')).toBe("Mr. McFadden's Hallowe'en")
    expect(toChicagoTitleCase('the story of saint jeanne d\'arc')).toBe("The Story of Saint Jeanne d'Arc")
    expect(toChicagoTitleCase('the passion of ss. perpetua and felicity, mm')).toBe(
      'The Passion of SS. Perpetua and Felicity, MM',
    )
    expect(toChicagoTitleCase('john n. neumann, d.d., fourth bishop')).toBe(
      'John N. Neumann, D.D., Fourth Bishop',
    )
    expect(toChicagoTitleCase('father chaignon, s.j., volume 1')).toBe('Father Chaignon, S.J., Volume 1')
    expect(
      toChicagoTitleCase(
        'the story of thomas more / weddings in the family / the road to damascus / from an altar screen',
      ),
    ).toBe('The Story of Thomas More / Weddings in the Family / The Road to Damascus / From an Altar Screen')
    expect(
      toChicagoTitleCase('Men & Women Are From Eden: A Study Guide to John Paul II\'s Theology of the Body'),
    ).toBe("Men & Women Are from Eden: A Study Guide to John Paul II's Theology of the Body")
    expect(toChicagoTitleCase('The Lion, the Witch and the Wardrobe')).toBe(
      'The Lion, the Witch and the Wardrobe',
    )
    expect(toChicagoTitleCase('in the 16th, 17th and 18th centuries')).toBe(
      'In the 16th, 17th and 18th Centuries',
    )
    expect(toChicagoTitleCase('De Trinitate (On the Trinity)')).toBe('De Trinitate (On the Trinity)')
    expect(toChicagoTitleCase('Spy × Family, v. 3')).toBe('Spy × Family, v. 3')
    expect(toChicagoTitleCase('The GIFTionary')).toBe('The GIFTionary')
  })
})

describe('canonical author names', () => {
  it('inverts commas, strips years, and expands parenthetical initials', () => {
    expect(toCanonicalAuthorName('Simpson, Richard')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('Simpson, Richard, 1920-1995')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('Richard Simpson (1920-1995)')).toBe('Richard Simpson')
    expect(toCanonicalAuthorName('King, Martin Luther, Jr.')).toBe('Martin Luther King Jr.')
    expect(toCanonicalAuthorName('Johnson, B. J.-P. (Barney John-Paul)')).toBe('Barney John Paul Johnson')
    expect(toCanonicalAuthorName('Tolkien, J. R. R. (John Ronald Reuel), 1892-1973')).toBe(
      'John Ronald Reuel Tolkien',
    )
    expect(toCanonicalAuthorName('Johnson, B. J.')).toBe('B. J. Johnson')
    expect(toCanonicalAuthorName('B. J.-P. Johnson')).toBe('B. J.-P. Johnson')
    expect(authorNeedsCanonicalName('Simpson, Richard')).toBe(true)
    expect(authorNeedsCanonicalName('Richard Simpson')).toBe(false)
    expect(authorNeedsCanonicalName('B. J. Johnson')).toBe(false)
    const community = 'Sisters of Charity of Our Lady, Mother of the Church'
    expect(toCanonicalAuthorName(community)).toBe(community)
    expect(authorNeedsCanonicalName(community)).toBe(false)
    expect(toCanonicalAuthorName('Ignatius, of Loyola')).toBe('Ignatius, of Loyola')
    expect(toCanonicalAuthorName('Bernard, of Clairvaux, Saint')).toBe('Bernard, of Clairvaux, Saint')
    expect(toCanonicalAuthorName('Puente, Luis de la')).toBe('Luis de la Puente')
    expect(toCanonicalAuthorName('Venerable Louis of Granada, OP')).toBe('Venerable Louis of Granada OP')
  })
})

describe('naming chips', () => {
  function book(overrides: Partial<BookDto>): BookDto {
    return {
      id: 1,
      title: 'The History of St. Dominic: Founder of the Friars Preachers',
      author: 'Richard Simpson',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      ...overrides,
    }
  }

  it('keeps only books that fail the selected naming rule', () => {
    const chicago = book({ id: 1 })
    const lower = book({ id: 2, title: 'the history of st. dominic: founder of the friars preachers' })
    const inverted = book({ id: 3, author: 'Simpson, Richard' })

    expect(applyChipFilters([chicago, lower, inverted], {
      ...defaultBookChipFilters,
      titleNotChicago: true,
    }).map((row) => row.id)).toEqual([2])

    expect(applyChipFilters([chicago, lower, inverted], {
      ...defaultBookChipFilters,
      authorNotCanonical: true,
    }).map((row) => row.id)).toEqual([3])
  })
})
