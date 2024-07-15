package com.example.servlettestapp

import org.slf4j.MDC
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import java.util.concurrent.Executor

@Configuration
class ExecutorConfig {

    @Bean
    fun asyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutorBuilder()
        return executor
            //.taskDecorator(MdcTaskDecorator())
            .build()
    }

    //NOTE: No task decorator is needed to hook the MDC back up, because it's handled automatically
    // by the span
    class MdcTaskDecorator : TaskDecorator {
        override fun decorate(runnable: Runnable): Runnable {
            val contextMap = MDC.getCopyOfContextMap();
            return Runnable {
                try {
                    MDC.setContextMap(contextMap)
                    runnable.run()
                } finally {
                    MDC.clear()
                }
            }
        }

    }
}