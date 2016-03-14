package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Word;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface WordRepository extends CrudRepository<Word, Long> {
    @EntityGraph(value = "Word.dependencies", type = EntityGraph.EntityGraphType.LOAD)
    @Query(value = "from Word w left join w.translations t where w.text = ?1 and w.lang = ?2 and t.tword.lang = ?3")
    Word findWithTranslations(String text, Lang fromLang, Lang tolang);

    @EntityGraph(value = "Word.dependencies", type = EntityGraph.EntityGraphType.LOAD)
    @Query(value = "from Word w left join w.translations t where w.text = ?1 and w.lang = ?2")
    Word findWithTranslations(String text, Lang fromLang);

    Word findByTextAndLang(String text, Lang fromLang);
}
