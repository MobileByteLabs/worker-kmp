package io.github.mobilebytelabs.worker.migrate

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WorkerMigratePluginTest {

    @Test
    fun plugin_registersBothTasks() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.mobilebytelabs.worker.migrate")
        assertNotNull(project.tasks.findByName("cmpWorkerMigrateCheck"))
        assertNotNull(project.tasks.findByName("cmpWorkerMigrateApply"))
    }

    @Test
    fun checkTask_group_isWorkerKmp() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.mobilebytelabs.worker.migrate")
        val task = project.tasks.findByName("cmpWorkerMigrateCheck")
        assertEquals("worker-kmp", task?.group)
    }
}
