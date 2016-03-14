package com.gtranslator.storage.batch;

import org.springframework.batch.core.StepExecution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchHelper {
    public static <T> T getObjectFromStepContext(StepExecution stepExecution, String stepName, String key) {
        Iterator<StepExecution> iterator = stepExecution.getJobExecution().getStepExecutions().iterator();
        while (iterator.hasNext()) {
            StepExecution execution = iterator.next();
            if (stepName.equals(execution.getStepName())) {
                return (T) execution.getExecutionContext().get(key);
            }
        }
        return null;
    }

    public static <T> List<T> getObjectsFromStepContext(StepExecution stepExecution, String prefixStepName, String key) {
        Iterator<StepExecution> iterator = stepExecution.getJobExecution().getStepExecutions().iterator();
        List<T> result = new ArrayList<>();
        while (iterator.hasNext()) {
            StepExecution execution = iterator.next();
            if (execution.getStepName().startsWith(prefixStepName + ":")) {
                result.add((T) execution.getExecutionContext().get(key));
            }
        }
        return result;
    }
}
