package com.gtranslator.storage.service;


import com.gtranslator.storage.domain.Dictionary;
import com.gtranslator.storage.domain.Lang;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.Set;

public interface DictionaryRepository extends CrudRepository<Dictionary, Long> {

    Dictionary findBySourceAndLang(String source, Lang lang);

    @Query(value = "from Dictionary d where d.lastModifiedDate > ?1 and d.lastModifiedDate <= ?2")
    Set<Dictionary> findByLastModifiedDate(Date prevDate, Date currentDate);

    Dictionary save(Dictionary dictionary);
}