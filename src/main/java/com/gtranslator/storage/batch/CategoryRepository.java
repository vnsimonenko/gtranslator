package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.Category;
import org.springframework.data.repository.CrudRepository;

interface CategoryRepository extends CrudRepository<Category, Long> {
    Category findByText(String text);
}
