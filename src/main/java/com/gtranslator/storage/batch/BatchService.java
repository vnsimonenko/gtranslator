package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.DictionaryModel;
import org.springframework.batch.core.launch.NoSuchJobException;

import java.util.List;

public interface BatchService {
    void execute();

    BatchEntityContext createEntityModel(DictionaryModel model) throws Exception;

    void saveEntityModel(List<BatchEntityContext> list);
}
