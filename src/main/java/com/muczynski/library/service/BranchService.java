/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BranchDto;
import com.muczynski.library.dto.BranchStatisticsDto;
import com.muczynski.library.mapper.BranchMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchMapper branchMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    public BranchDto createBranch(BranchDto branchDto) {
        Library branch = branchMapper.toEntity(branchDto);
        Library savedBranch = branchRepository.save(branch);
        return branchMapper.toDto(savedBranch);
    }

    public List<BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(branchMapper::toDto)
                .collect(Collectors.toList());
    }

    public BranchDto getBranchById(Long id) {
        return branchRepository.findById(id)
                .map(branchMapper::toDto)
                .orElse(null);
    }

    public BranchDto updateBranch(Long id, BranchDto branchDto) {
        Library branch = branchRepository.findById(id).orElseThrow(() -> new LibraryException("Branch not found: " + id));
        Library updatedBranch = branchMapper.toEntity(branchDto);
        updatedBranch.setId(id);
        Library savedBranch = branchRepository.save(updatedBranch);
        return branchMapper.toDto(savedBranch);
    }

    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new LibraryException("Branch not found: " + id);
        }
        branchRepository.deleteById(id);
    }

    /**
     * Get the default branch, creating it if it doesn't exist
     * This ensures there's always at least one branch available
     */
    public Library getOrCreateDefaultBranch() {
        List<Library> branches = branchRepository.findAll();
        if (!branches.isEmpty()) {
            return branches.get(0);
        }

        // Create default branch
        Library branch = new Library();
        branch.setBranchName("St. Martin de Porres");
        branch.setLibrarySystemName("Sacred Heart Library System");
        return branchRepository.save(branch);
    }

    /**
     * Get statistics for all branches
     */
    public List<BranchStatisticsDto> getBranchStatistics() {
        List<Library> branches = branchRepository.findAll();
        List<BranchStatisticsDto> statistics = new ArrayList<>();

        for (Library branch : branches) {
            Long bookCount = bookRepository.countByLibraryId(branch.getId());
            Long activeLoansCount = loanRepository.countByBookLibraryIdAndReturnDateIsNull(branch.getId());

            BranchStatisticsDto stats = new BranchStatisticsDto(
                branch.getId(),
                branch.getBranchName(),
                bookCount,
                activeLoansCount
            );
            statistics.add(stats);
        }

        return statistics;
    }
}
