package com.gtranslator.storage.batch;

import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.service.DictionaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfiguration {
    final static Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

    @Autowired
    private BatchService batchService;

    @Autowired
    private DictionaryService dictionaryService;

    @Bean
    public Job mainDictionaryJob(JobBuilderFactory jobs,
                                 Step loaderDictionaryStep,
                                 Step builderMasterDictionaryStep,
                                 Step writerDictionaryStep) {
        return jobs.get("mainDictionaryJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        logger.info("starting run mainDictionaryJob job");
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        logger.info("finish mainDictionaryJob job");
                    }
                })
                .start(loaderDictionaryStep)
                .next(new JobExecutionDecider() {
                    @Override
                    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
                        List models = BatchHelper.getObjectFromStepContext(
                                stepExecution, "loaderDictionaryStep", "dictionary-models");
                        return models == null || models.size() == 0
                                ? FlowExecutionStatus.STOPPED : FlowExecutionStatus.COMPLETED;
                    }
                })
                .next(builderMasterDictionaryStep)
                .next(writerDictionaryStep)
                .end()
                .build();
    }

    @Bean
    @StepScope
    public Tasklet loaderDictionaryTasklet(@Value("#{jobParameters['prev-load-date']}") Date prevLoadDate,
                                           @Value("#{jobParameters['current-load-date']}") Date currentLoadDate) {
        return (contribution, chunkContext) -> {
            List<DictionaryModel> models = dictionaryService.getDictionaryModelsByDate(prevLoadDate, currentLoadDate);
            chunkContext.getStepContext().getStepExecution().getExecutionContext().put("dictionary-models", models);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    public Tasklet builderDictionaryTasklet(@Value("#{stepExecutionContext['dictionary-model']}") DictionaryModel model) {
        return (contribution, chunkContext) -> {
            if (model != null) {
                BatchEntityContext result = batchService.createEntityModel(model);
                chunkContext.getStepContext().getStepExecution().getExecutionContext().put("entity-model", result);
            }
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    public Tasklet writerDictionaryTasklet() {
        return (contribution, chunkContext) -> {
            List<BatchEntityContext> list = BatchHelper.getObjectsFromStepContext(
                    chunkContext.getStepContext().getStepExecution(), "builderDictionaryStep", "entity-model");
            batchService.saveEntityModel(list);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step builderDictionaryStep(StepBuilderFactory stepBuilderFactory, Tasklet builderDictionaryTasklet) {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(true);
        attribute.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return stepBuilderFactory.get("builderDictionaryStep")
                .tasklet(builderDictionaryTasklet)
                .transactionAttribute(attribute)
                .build();
    }

    @Bean
    public Step loaderDictionaryStep(StepBuilderFactory stepBuilderFactory, Tasklet loaderDictionaryTasklet) {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(true);
        attribute.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return stepBuilderFactory.get("loaderDictionaryStep")
                .tasklet(loaderDictionaryTasklet)
                .transactionAttribute(attribute)
                .build();
    }

    @Bean
    public Step writerDictionaryStep(StepBuilderFactory stepBuilderFactory, Tasklet writerDictionaryTasklet) {
        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setReadOnly(false);
        attribute.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return stepBuilderFactory.get("writerDictionaryStep")
                .tasklet(writerDictionaryTasklet)
                .transactionAttribute(attribute)
                .build();
    }

    @Bean
    @DependsOn("builderDictionaryStep")
    protected Step builderMasterDictionaryStep(JobRepository jobRepository, StepBuilderFactory stepBuilderFactory,
                                               Step builderDictionaryStep) throws Exception {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setDaemon(true);
        taskExecutor.setCorePoolSize(8);
        taskExecutor.initialize();
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setStep(builderDictionaryStep);
        partitionHandler.setGridSize(8);
        String stepName = "builderDictionaryStep";
        SimplePartitioner partitioner = new SimplePartitioner();
        StepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, false, stepName, partitioner) {
            @Override
            public Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException {
                List<DictionaryModel> models = BatchHelper.getObjectFromStepContext(
                        stepExecution, "loaderDictionaryStep", "dictionary-models");
                if (models != null) {
                    int size = models.size();
                    Set<StepExecution> result = super.split(stepExecution, size);
                    Iterator<DictionaryModel> modelIterator = models.iterator();
                    for (StepExecution exec : result) {
                        exec.getExecutionContext().put("dictionary-model", modelIterator.next());
                    }
                    return result;
                } else {
                    return super.split(stepExecution, 0);
                }
            }
        };
        return stepBuilderFactory
                .get("builderDictionaryStep")
                .partitioner(stepName, partitioner)
                .partitionHandler(partitionHandler)
                .splitter(splitter)
                .build();
    }
}
