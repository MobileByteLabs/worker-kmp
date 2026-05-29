package io.github.mobilebytelabs.worker.app.gradle

/**
 * Tiny moustache-lite templating: replaces `{{name}}` placeholders + supports
 * the `{{#permissions}}…{{/permissions}}` section iteration we use for
 * AndroidManifest.xml permission entries.
 *
 * Templates live as resources under `templates/` in the plugin's JAR.
 */
internal object TemplateEngine {

    fun load(resourceName: String): String =
        TemplateEngine::class.java.classLoader.getResource("templates/$resourceName")
            ?.readText(Charsets.UTF_8)
            ?: error("worker-kmp-app: missing template resource templates/$resourceName")

    fun render(template: String, model: CodegenModel, extras: Map<String, String> = emptyMap()): String {
        // 1. Section iteration: {{#permissions}}<line>{{/permissions}} → joined permission entries
        val sectionRegex = Regex("\\{\\{#permissions\\}\\}([\\s\\S]*?)\\{\\{/permissions\\}\\}")
        var rendered = sectionRegex.replace(template) { match ->
            val inner = match.groupValues[1]
            model.androidPermissions.joinToString(separator = "\n") { permission ->
                inner.replace("{{.}}", permission)
            }
        }

        // 2. Field substitutions.
        val substitutions = buildMap {
            put("title", model.title)
            put("iosBundleId", model.iosBundleId)
            put("webCanvasId", model.webCanvasId)
            put("androidApplicationId", model.androidApplicationId)
            put("packageName", model.packageName)
            put("koinFnFqn", model.koinModulesFnFqn)
            put("koinFnSimpleName", model.koinModulesFnSimpleName)
            put("contentFnFqn", model.contentFnFqn)
            put("contentFnSimpleName", model.contentFnSimpleName)
            putAll(extras)
        }
        for ((key, value) in substitutions) {
            rendered = rendered.replace("{{$key}}", value)
        }
        return rendered
    }
}
