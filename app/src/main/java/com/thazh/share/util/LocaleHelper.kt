package com.thazh.share.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.thazh.share.data.AppLanguage
import com.thazh.share.ui.auth.findActivity
import java.util.Locale

object LocaleHelper {
    fun createLocalizedContext(base: Context, language: AppLanguage): Context {
        val unwrappedBase = runCatching { base.findActivity() as Context }.getOrElse { base }
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(unwrappedBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val localizedResourcesContext = unwrappedBase.createConfigurationContext(config)
        return object : ContextWrapper(unwrappedBase) {
            override fun getResources() = localizedResourcesContext.resources
        }
    }
}

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val (localizedContext, localizedConfig) = remember(baseContext, baseConfiguration, language) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        LocaleHelper.createLocalizedContext(baseContext, language) to config
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
    ) {
        content()
    }
}
