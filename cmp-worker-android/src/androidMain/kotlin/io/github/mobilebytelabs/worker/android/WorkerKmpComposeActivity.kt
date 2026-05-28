package io.github.mobilebytelabs.worker.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

/**
 * Base [ComponentActivity] that hosts an arbitrary Compose Multiplatform [content] composable.
 *
 * Designed to collapse the boilerplate every Android consumer of worker-kmp would otherwise
 * repeat: subclass [ComponentActivity], override [onCreate], call [setContent]. Instead, the
 * consumer writes a one-liner subclass that hands the launcher its `@Composable` content lambda:
 *
 * ```
 * class MainActivity : WorkerKmpComposeActivity({ SampleApp() })
 * ```
 *
 * Register the subclass in `AndroidManifest.xml` exactly as you would any [ComponentActivity]:
 *
 * ```xml
 * <activity
 *     android:name=".MainActivity"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MAIN" />
 *         <category android:name="android.intent.category.LAUNCHER" />
 *     </intent-filter>
 * </activity>
 * ```
 *
 * The library does NOT wrap [content] with `MaterialTheme` or any other theming — keep your
 * theme decisions in commonMain so the same composable can host the iOS, Desktop, and Web
 * launchers without divergence.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC3.
 *
 * @param content Compose Multiplatform UI to render once the activity is created.
 */
public open class WorkerKmpComposeActivity(private val content: @Composable () -> Unit) : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { content() }
    }
}
