package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.Translation;
import org.springframework.data.repository.CrudRepository;

interface TranslationRepository extends CrudRepository<Translation, Long> {
}
