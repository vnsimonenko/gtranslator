package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.*;
import com.gtranslator.storage.service.DictionaryService;
import com.gtranslator.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BatchServiceImpl implements BatchService {
    final static Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private Job mainDictionaryJob;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    private final String JOB_NAME = "mainDictionaryJob";

    @Scheduled(fixedDelayString = "${custom.prop.batch.scheduled.fixedDelay}")
    @Override
    public void execute() {
        logger.info("Start for job ...");
        try {
            try {
                Set<Long> ids = jobOperator.getRunningExecutions(JOB_NAME);
                if (ids.size() > 0) {
                    for (Long id : ids) {
                        JobExecution execution = jobExplorer.getJobExecution(id);
                        if (execution.getExitStatus() == ExitStatus.EXECUTING) {
                            jobOperator.stop(execution.getJobInstance().getInstanceId());
                        }
                    }
                    //return;
                }
            } catch (NoSuchJobException ex) {
            }
            List<JobInstance> jobs = jobExplorer.getJobInstances(JOB_NAME, 1, 1);
            JobInstance jobInstance = jobs.size() > 0 ? jobs.get(0) : null;
            JobParameters jobParameters = null;
            JobExecution jobExecution = null;
            if (jobInstance != null) {
                List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                jobExecution = jobExecutions.size() > 0 ? jobExecutions.get(0) : null;
                jobParameters = jobExecution != null ? jobExecution.getJobParameters() : null;
            }
            if (jobParameters == null) {
                Date prevDate = new Date(0);
                Date currentDate = new Date();
                jobParameters = new JobParametersBuilder()
                        .addDate("prev-load-date", prevDate)
                        .addDate("current-load-date", currentDate).toJobParameters();
            } else if (jobExecution.getExitStatus() == ExitStatus.FAILED) {
                Date prevDate = (Date) jobParameters.getParameters().get("prev-load-date").getValue();
                jobParameters = new JobParametersBuilder(jobParameters)
                        .addDate("prev-load-date", prevDate)
                        .addDate("current-load-date", new Date()).toJobParameters();
            } else {
                Date prevDate = new Date(0);
                if (jobParameters.getParameters().get("current-load-date") != null) {
                    prevDate = (Date) jobParameters.getParameters().get("current-load-date").getValue();
                }
                jobParameters = new JobParametersBuilder(jobParameters)
                        .addDate("prev-load-date", prevDate)
                        .addDate("current-load-date", new Date()).toJobParameters();
            }
            jobParameters = mainDictionaryJob.getJobParametersIncrementer().getNext(jobParameters);

            jobExecution = jobLauncher.run(mainDictionaryJob, jobParameters);
            if (!(jobExecution.getExitStatus().getExitCode().equals(ExitStatus.COMPLETED.getExitCode())
                    || jobExecution.getExitStatus().getExitCode().equals(ExitStatus.STOPPED.getExitCode()))) {
                logger.error("The job finished with exitcode: ", jobExecution.getExitStatus().getExitCode());
                for (Throwable tw : jobExecution.getAllFailureExceptions()) {
                    logger.error("fail for job: ", tw);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            logger.info("Finish for job");
        }
    }

    @org.springframework.context.event.EventListener({ContextClosedEvent.class})
    public void close()  {
        for (String jobName : jobOperator.getJobNames()) {
            try {
                for (Long jexecId : jobOperator.getRunningExecutions(jobName)) {
                    try {
                        jobOperator.stop(jexecId);
                        JobExecution jexec = jobExplorer.getJobExecution(jexecId);
                        int i = 600;
                        while (i-- > 0 && jexec.isRunning()) {
                            logger.info("waiting ...");
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                    } catch (NoSuchJobExecutionException | JobExecutionNotRunningException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            } catch (NoSuchJobException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public BatchEntityContext createEntityModel(DictionaryModel model) throws Exception {
        BatchEntityContext entities = BatchEntityContext.create();
        Map<Object, Object> bufferMap = new HashMap<>();

        Word word = wordRepository.findWithTranslations(model.getSource(), model.getSourceLang());
        if (word == null) {
            word = new Word();
            word.setText(model.getSource());
            word.setLang(model.getSourceLang());
            word.setTranslations(new HashSet<>());
            word.setTranscriptions(new HashSet<>());
        }

        if (word.isNew()) {
            entities.put(Word.class, word);
        }

        word.getTranslations().forEach(t -> bufferMap.put(Utils.createKey(Translation.class, t.getTword().getText(), t.getTword().getLang()), t));
        word.getTranscriptions().forEach(t -> bufferMap.put(Utils.createKey(Transcription.class, t.getText(), t.getPhonetic(), t.getWord().getText(), t.getWord().getLang()), t));

        for (DictionaryModel.TranscriptionRecord record : model.getTranscriptionRecords()) {
            Transcription transcription = (Transcription) bufferMap.get(Utils.createKey(Transcription.class, record.getTranscription(), record.getPhonetic(), word.getText(), word.getLang()));
            if (transcription == null) {
                transcription = new Transcription();
                transcription.setText(record.getTranscription());
                transcription.setPhonetic(record.getPhonetic());
                transcription.setWord(word);
                entities.put(Transcription.class, transcription);
            }
        }

        for (DictionaryModel.TranslationRecord record : model.getTranslationRecords()) {
            Word word2 = (Word) bufferMap.get(Utils.createKey(Word.class, record.getTranslation(), record.getLang()));
            if (word2 == null) {
                word2 = wordRepository.findByTextAndLang(record.getTranslation(), record.getLang());
            }
            if (word2 == null) {
                word2 = new Word();
                word2.setText(record.getTranslation());
                word2.setLang(record.getLang());
                entities.put(Word.class, word2);
            }
            Translation translation = (Translation) bufferMap.get(Utils.createKey(Translation.class, word2.getText(), word2.getLang()));
            if (translation == null) {
                translation = new Translation();
                translation.setSword(word);
                translation.setTword(word2);
                translation.setWeight(record.getWeight());
                entities.put(Translation.class, translation);
            }
            if (translation.getCategory() == null) {
                Category category = categoryRepository.findByText(record.getCategory());
                if (category == null) {
                    category = new Category();
                    category.setText(record.getCategory());
                    entities.put(Category.class, category);
                }
                translation.setCategory(category);
                entities.put(Translation.class, translation);
            }
        }
        return entities;
    }

    @Override
    public void saveEntityModel(List<BatchEntityContext> list) {
        Map<Object, Word> wordMap = new HashMap<>();
        Map<Object, Category> categoryMap = new HashMap<>();
        Map<Object, Transcription> transcriptionMap = new HashMap<>();
        Map<Object, Translation> translationMap = new HashMap<>();
        for (BatchEntityContext context : list) {
            Collection<Category> categories = ObjectUtils.defaultIfNull(context.get(Category.class), Collections.emptyList());
            for (Category c : categories) {
                categoryMap.put(Utils.createKey(c.getText()), c);
            }
            Collection<Word> words = ObjectUtils.defaultIfNull(context.get(Word.class), Collections.emptyList());
            for (Word w : words) {
                wordMap.put(Utils.createKey(w.getText(), w.getLang()), w);
            }
            Collection<Transcription> transcriptions = ObjectUtils.defaultIfNull(context.get(Transcription.class), Collections.emptyList());
            for (Transcription t : transcriptions) {
                //only english
                transcriptionMap.put(Utils.createKey(t.getText(), t.getPhonetic(), t.getWord().getText(), t.getWord().getLang()), t);
            }
            Collection<Translation> translations = ObjectUtils.defaultIfNull(context.get(Translation.class), Collections.emptyList());
            for (Translation t : translations) {
                translationMap.put(Utils.createKey(
                        t.getSword().getText(), t.getSword().getLang(),
                        t.getTword().getText(), t.getTword().getLang()), t);
            }
        }
        categoryRepository.save(categoryMap.values());
        wordRepository.save(wordMap.values());
        for (Transcription t : transcriptionMap.values()) {
            if (t.getWord().isNew()) {
                Word w = wordRepository.findByTextAndLang(t.getWord().getText(), t.getWord().getLang());
                t.setWord(ObjectUtils.defaultIfNull(w, t.getWord()));
            }
        }
        transcriptionRepository.save(transcriptionMap.values());
        for (Translation t : translationMap.values()) {
            if (t.getSword().isNew()) {
                Word w = wordRepository.findByTextAndLang(t.getSword().getText(), t.getSword().getLang());
                t.setSword(ObjectUtils.defaultIfNull(w, t.getSword()));
            }
            if (t.getTword().isNew()) {
                Word w = wordRepository.findByTextAndLang(t.getTword().getText(), t.getTword().getLang());
                t.setTword(ObjectUtils.defaultIfNull(w, t.getTword()));
            }
            if (t.getCategory().isNew()) {
                Category c = categoryRepository.findByText(t.getCategory().getText());
                t.setCategory(ObjectUtils.defaultIfNull(c, t.getCategory()));
            }
        }
        translationRepository.save(translationMap.values());
    }
}