package com.example.chocolateShop.service;




import com.example.chocolateShop.dtos.category.CategoryRequest;
import com.example.chocolateShop.dtos.category.CategoryResponse;
import com.example.chocolateShop.exceptions.DuplicateResourceException;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.Category;
import com.example.chocolateShop.repository.CategoryRepository;
import com.example.chocolateShop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists");
        }

        String slug = uniqueSlug(request.name());

        Category category = Category.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists");
        }

        if (!category.getName().equalsIgnoreCase(request.name())) {
            category.setSlug(uniqueSlug(request.name()));
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.toSlug(name);
        String candidate = base;
        int suffix = 1;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
