package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.Transcription;
import org.springframework.data.repository.CrudRepository;

interface TranscriptionRepository extends CrudRepository<Transcription, Long> {
}
